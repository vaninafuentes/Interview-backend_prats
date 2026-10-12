package com.example.interviewback.service;

import com.example.interviewback.dto.InterviewAnswerRequest;
import com.example.interviewback.dto.InterviewFeedbackResponse;
import com.example.interviewback.dto.InterviewHintRequest;
import com.example.interviewback.dto.InterviewHintResponse;
import com.example.interviewback.dto.InterviewHistoryDto;
import com.example.interviewback.dto.InterviewStatsDto;
import com.example.interviewback.model.InterviewSession;
import com.example.interviewback.model.Question;
import com.example.interviewback.repository.InterviewSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio orquestador de la sesión de entrevista.
 *
 * Soporta dos modos de evaluación:
 *   1. Pregunta de BD:       request.getQuestionId() != null
 *   2. Pregunta generada IA: request.getQuestionText() + category + difficulty
 */
@Service
public class InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewService.class);

    private final QuestionService questionService;
    private final GroqService groqService;
    private final InterviewSessionRepository sessionRepository;

    public InterviewService(QuestionService questionService,
                            GroqService groqService,
                            InterviewSessionRepository sessionRepository) {
        this.questionService = questionService;
        this.groqService = groqService;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Procesa la respuesta del usuario a una pregunta de entrevista.
     *
     * Modo 1 — Pregunta de BD (questionId provisto):
     *   Lee el texto, categoría y dificultad de la BD.
     *
     * Modo 2 — Pregunta generada (questionId null):
     *   Usa los campos questionText, questionCategory, questionDifficulty del request.
     *
     * @param request DTO con la respuesta y los datos de la pregunta
     * @param userId  ID del usuario autenticado
     */
    // Sin @Transactional a propósito: este método llama a la API de IA, que es
    // lenta y externa. Mantener abierta una transacción durante esa llamada
    // retendría una conexión del pool sin usarla, y con pocas conexiones
    // disponibles alcanzaría para agotarlo. Cada operación de base (leer la
    // pregunta, guardar la sesión) se ejecuta en su propia transacción.
    public InterviewFeedbackResponse evaluate(InterviewAnswerRequest request, Long userId) {
        log.info("[InterviewService] evaluate() | userId={} | questionId={} | hasText={}",
                userId, request.getQuestionId(), request.getQuestionText() != null);

        String questionText;
        String category;
        String difficulty;
        Long questionId = request.getQuestionId();

        if (questionId != null) {
            // Modo 1: pregunta de la base de datos
            Question question = questionService.getQuestionById(questionId);
            questionText = question.getText();
            category     = question.getCategory();
            difficulty   = question.getDifficulty();
        } else {
            // Modo 2: pregunta generada por IA
            questionText = request.getQuestionText();
            category     = request.getQuestionCategory();
            difficulty   = request.getQuestionDifficulty();

            if (questionText == null || questionText.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Debe proveer questionId o questionText para evaluar la respuesta.");
            }
            if (category == null || category.isBlank()) category = "General";
            if (difficulty == null || difficulty.isBlank()) difficulty = "MEDIUM";
        }

        // Llamar a Groq para evaluar
        InterviewFeedbackResponse feedback = groqService.evaluateAnswer(
                questionText, category, difficulty, request.getUserAnswer());

        // Persistir la sesión con metadata completa
        try {
            String recommendationsStr = feedback.getRecommendations() != null
                    ? String.join("|", feedback.getRecommendations())
                    : "";

            InterviewSession session = new InterviewSession(
                    userId,
                    questionId,
                    questionText,
                    category,
                    difficulty,
                    request.getUserAnswer(),
                    feedback.getFeedback(),
                    feedback.getScore(),
                    recommendationsStr,
                    feedback.getModelAnswer()
            );
            sessionRepository.save(session);
            log.info("[InterviewService] Sesión guardada | userId={} | questionId={}", userId, questionId);
        } catch (Exception e) {
            log.error("[InterviewService] Error al guardar la sesión: {} - {}",
                    e.getClass().getSimpleName(), e.getMessage());
        }

        return feedback;
    }

    /**
     * Obtiene el historial completo de entrevistas del usuario autenticado.
     */
    @Transactional(readOnly = true)
    public List<InterviewHistoryDto> getUserHistory(Long userId) {
        return sessionRepository.findByUserIdOrderByAnsweredAtDesc(userId)
                .stream()
                .map(this::toHistoryDto)
                .collect(Collectors.toList());
    }

    /**
     * Calcula estadísticas consolidadas del usuario para el dashboard.
     */
    @Transactional(readOnly = true)
    public InterviewStatsDto getUserStats(Long userId) {
        List<InterviewSession> sessions = sessionRepository.findByUserIdOrderByAnsweredAtDesc(userId);

        if (sessions.isEmpty()) {
            return new InterviewStatsDto(
                    0, 0.0, 0, 0, java.util.Collections.emptyMap(), java.util.Collections.emptyList()
            );
        }

        int totalSessions = sessions.size();
        double avgScore = sessions.stream()
                .filter(s -> s.getScore() != null)
                .mapToInt(InterviewSession::getScore)
                .average()
                .orElse(0.0);
        avgScore = Math.round(avgScore * 10.0) / 10.0;

        int bestScore = sessions.stream()
                .filter(s -> s.getScore() != null)
                .mapToInt(InterviewSession::getScore)
                .max()
                .orElse(0);

        // Agrupación por categoría
        java.util.Map<String, Double> categoryAverages = sessions.stream()
                .filter(s -> s.getCategory() != null && s.getScore() != null)
                .collect(Collectors.groupingBy(
                        InterviewSession::getCategory,
                        Collectors.averagingInt(InterviewSession::getScore)
                ))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        java.util.Map.Entry::getKey,
                        e -> Math.round(e.getValue() * 10.0) / 10.0
                ));

        // Cálculo de racha (streak) en días consecutivos
        int currentStreak = calculateStreak(sessions);

        // Últimas 5 sesiones para actividad reciente
        List<InterviewHistoryDto> recent = sessions.stream()
                .limit(5)
                .map(this::toHistoryDto)
                .collect(Collectors.toList());

        return new InterviewStatsDto(
                totalSessions, avgScore, currentStreak, bestScore, categoryAverages, recent
        );
    }

    /**
     * Genera una pista inteligente para una pregunta.
     */
    // Sin @Transactional por el mismo motivo que evaluate: llama a la API de IA.
    public InterviewHintResponse getHint(InterviewHintRequest request) {
        String hint = groqService.generateHint(request.getQuestionText(), request.getCategory());
        return new InterviewHintResponse(hint);
    }

    private InterviewHistoryDto toHistoryDto(InterviewSession session) {
        List<String> tipsList = java.util.Collections.emptyList();
        if (session.getTips() != null && !session.getTips().isBlank()) {
            tipsList = java.util.Arrays.asList(session.getTips().split("\\|"));
        }

        return new InterviewHistoryDto(
                session.getId(),
                session.getQuestionId(),
                session.getQuestionText() != null ? session.getQuestionText() : "Pregunta de práctica",
                session.getCategory() != null ? session.getCategory() : "General",
                session.getDifficulty() != null ? session.getDifficulty() : "MEDIUM",
                session.getUserAnswer(),
                session.getAiFeedback(),
                session.getScore() != null ? session.getScore() : 0,
                tipsList,
                session.getModelAnswer(),
                session.getAnsweredAt()
        );
    }

    private int calculateStreak(List<InterviewSession> sessions) {
        java.util.Set<java.time.LocalDate> dates = sessions.stream()
                .map(s -> s.getAnsweredAt().toLocalDate())
                .collect(Collectors.toCollection(java.util.TreeSet::new));

        if (dates.isEmpty()) return 0;

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate yesterday = today.minusDays(1);

        // Si no practicó ni hoy ni ayer, la racha es 0
        if (!dates.contains(today) && !dates.contains(yesterday)) {
            return 0;
        }

        int streak = 0;
        java.time.LocalDate cursor = dates.contains(today) ? today : yesterday;

        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }

        return streak;
    }
}

