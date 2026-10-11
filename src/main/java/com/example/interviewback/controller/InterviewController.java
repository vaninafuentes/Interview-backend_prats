package com.example.interviewback.controller;

import com.example.interviewback.dto.GeneratedQuestionDto;
import com.example.interviewback.dto.InterviewAnswerRequest;
import com.example.interviewback.dto.InterviewFeedbackResponse;
import com.example.interviewback.dto.QuestionGenerationRequest;
import com.example.interviewback.model.User;
import com.example.interviewback.service.GroqService;
import com.example.interviewback.service.InterviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Controlador REST para la funcionalidad de práctica de entrevistas.
 *
 * Endpoints:
 *   POST /api/interview/generate  → genera preguntas con Groq AI (requiere JWT)
 *   POST /api/interview/evaluate  → evalúa una respuesta con Groq AI (requiere JWT)
 */
@RestController
@RequestMapping("/api/interview")
public class InterviewController {

    private final InterviewService interviewService;
    private final GroqService groqService;

    public InterviewController(InterviewService interviewService, GroqService groqService) {
        this.interviewService = interviewService;
        this.groqService = groqService;
    }

    /**
     * POST /api/interview/generate
     *
     * Genera preguntas de entrevista usando Groq AI según el tema,
     * dificultad y cantidad indicados por el usuario.
     *
     * @param request        body JSON con topic, difficulty, count
     * @param authentication usuario autenticado via JWT
     * @return lista de preguntas generadas
     */
    @PostMapping("/generate")
    public ResponseEntity<List<GeneratedQuestionDto>> generateQuestions(
            @Valid @RequestBody QuestionGenerationRequest request,
            Authentication authentication
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }

        List<GeneratedQuestionDto> questions = groqService.generateQuestions(
                request.getTopic(),
                request.getDifficulty(),
                request.getCount()
        );
        return ResponseEntity.ok(questions);
    }

    /**
     * POST /api/interview/evaluate
     *
     * Recibe la respuesta del usuario a una pregunta de entrevista,
     * la evalúa con Groq AI y devuelve el feedback.
     *
     * Soporta dos modos:
     *   - Pregunta de BD: { questionId, userAnswer }
     *   - Pregunta generada: { questionText, questionCategory, questionDifficulty, userAnswer }
     *
     * @param request        body JSON con los datos de la pregunta y la respuesta
     * @param authentication usuario autenticado via JWT
     * @return feedback con score, criterios, fortalezas, errores y recomendaciones
     */
    @PostMapping("/evaluate")
    public ResponseEntity<InterviewFeedbackResponse> evaluate(
            @Valid @RequestBody InterviewAnswerRequest request,
            Authentication authentication
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }

        InterviewFeedbackResponse feedback = interviewService.evaluate(request, user.getId());
        return ResponseEntity.ok(feedback);
    }

    /**
     * GET /api/interview/history
     * Devuelve el historial de preguntas respondidas por el usuario autenticado.
     */
    @org.springframework.web.bind.annotation.GetMapping("/history")
    public ResponseEntity<List<com.example.interviewback.dto.InterviewHistoryDto>> getHistory(
            Authentication authentication
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }

        return ResponseEntity.ok(interviewService.getUserHistory(user.getId()));
    }

    /**
     * GET /api/interview/stats
     * Devuelve métricas calculadas (racha, promedio, sesiones totales, etc.) para el dashboard.
     */
    @org.springframework.web.bind.annotation.GetMapping("/stats")
    public ResponseEntity<com.example.interviewback.dto.InterviewStatsDto> getStats(
            Authentication authentication
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }

        return ResponseEntity.ok(interviewService.getUserStats(user.getId()));
    }

    /**
     * POST /api/interview/hint
     * Genera una pista orientativa para la pregunta en curso sin spoilear la respuesta.
     */
    @PostMapping("/hint")
    public ResponseEntity<com.example.interviewback.dto.InterviewHintResponse> getHint(
            @Valid @RequestBody com.example.interviewback.dto.InterviewHintRequest request,
            Authentication authentication
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }

        return ResponseEntity.ok(interviewService.getHint(request));
    }
}

