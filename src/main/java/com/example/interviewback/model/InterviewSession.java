package com.example.interviewback.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Entidad JPA que representa una sesión de práctica de entrevista.
 *
 * Capa: MODEL
 * -----------
 * Guarda el registro de cada vez que un usuario responde a una pregunta
 * durante una sesión de entrevista simulada. Incluye la respuesta del usuario
 * y el feedback generado por la IA (Grok).
 *
 * Campos:
 *   - id:          identificador único, auto-generado
 *   - userId:      ID del usuario que practicó
 *   - questionId:  ID de la pregunta respondida
 *   - userAnswer:  respuesta escrita por el usuario (puede ser transcripción de audio)
 *   - aiFeedback:  feedback generado por Grok en texto libre
 *   - score:       puntuación de 0 a 10 asignada por la IA
 *   - tips:        consejos separados por "|" para mejorar la respuesta
 *   - answeredAt:  timestamp de cuándo se respondió
 */
@Entity
@Table(name = "interview_sessions")
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    // Puede ser null para preguntas generadas dinámicamente por IA
    @Column(nullable = true)
    private Long questionId;

    // TEXT porque la respuesta puede ser larga
    @Column(nullable = false, columnDefinition = "TEXT")
    private String userAnswer;

    // Feedback generado por Grok
    @Column(columnDefinition = "TEXT")
    private String aiFeedback;

    // Score de 0 a 10
    @Column
    private Integer score;

    // Tips separados por "|" (ej: "Mencioná complejidad|Usá un ejemplo")
    @Column(columnDefinition = "TEXT")
    private String tips;

    @Column(columnDefinition = "TEXT")
    private String questionText;

    @Column
    private String category;

    @Column
    private String difficulty;

    @Column(columnDefinition = "TEXT")
    private String modelAnswer;

    @Column(nullable = false)
    private LocalDateTime answeredAt;

    public InterviewSession() {
    }

    /**
     * Constructor para preguntas de DB (questionId conocido).
     */
    public InterviewSession(Long userId, Long questionId, String userAnswer,
                            String aiFeedback, Integer score, String tips) {
        this.userId = userId;
        this.questionId = questionId;
        this.userAnswer = userAnswer;
        this.aiFeedback = aiFeedback;
        this.score = score;
        this.tips = tips;
        this.answeredAt = LocalDateTime.now();
    }

    public InterviewSession(Long userId, Long questionId, String questionText,
                            String category, String difficulty, String userAnswer,
                            String aiFeedback, Integer score, String tips, String modelAnswer) {
        this.userId = userId;
        this.questionId = questionId;
        this.questionText = questionText;
        this.category = category;
        this.difficulty = difficulty;
        this.userAnswer = userAnswer;
        this.aiFeedback = aiFeedback;
        this.score = score;
        this.tips = tips;
        this.modelAnswer = modelAnswer;
        this.answeredAt = LocalDateTime.now();
    }

    // --- Getters y Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }

    public String getAiFeedback() { return aiFeedback; }
    public void setAiFeedback(String aiFeedback) { this.aiFeedback = aiFeedback; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getTips() { return tips; }
    public void setTips(String tips) { this.tips = tips; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getModelAnswer() { return modelAnswer; }
    public void setModelAnswer(String modelAnswer) { this.modelAnswer = modelAnswer; }

    public LocalDateTime getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(LocalDateTime answeredAt) { this.answeredAt = answeredAt; }
}
