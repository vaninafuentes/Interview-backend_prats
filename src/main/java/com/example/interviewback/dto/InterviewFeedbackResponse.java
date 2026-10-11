package com.example.interviewback.dto;

import java.util.List;

/**
 * DTO de respuesta con el feedback generado por Groq AI.
 *
 * Capa: DTO
 * ---------
 * Representa el JSON que el backend devuelve al frontend:
 *   POST /api/interview/evaluate → 200 OK
 *
 * {
 *   "score": 8,
 *   "criteria": {
 *     "technicalKnowledge": 9,
 *     "relevance": 8,
 *     "clarity": 8,
 *     "communication": 7,
 *     "structure": 8
 *   },
 *   "strengths": ["Explicó correctamente el concepto principal"],
 *   "mistakes": ["No explicó una diferencia importante"],
 *   "recommendations": ["Agregar un ejemplo práctico"],
 *   "feedback": "La respuesta demuestra un buen dominio del tema."
 * }
 *
 * Campos:
 *   - score:           puntuación global de 0 a 10
 *   - criteria:        puntuaciones individuales por criterio
 *   - strengths:       fortalezas concretas identificadas en la respuesta
 *   - mistakes:        errores o aspectos incorrectos/faltantes
 *   - recommendations: consejos accionables para mejorar
 *   - feedback:        resumen breve de la evaluación general
 */
public class InterviewFeedbackResponse {

    private int score;
    private InterviewCriteria criteria;
    private List<String> strengths;
    private List<String> mistakes;
    private List<String> recommendations;
    private String feedback;
    private String modelAnswer;

    public InterviewFeedbackResponse() {}

    public InterviewFeedbackResponse(int score, InterviewCriteria criteria,
                                     List<String> strengths, List<String> mistakes,
                                     List<String> recommendations, String feedback) {
        this(score, criteria, strengths, mistakes, recommendations, feedback, null);
    }

    public InterviewFeedbackResponse(int score, InterviewCriteria criteria,
                                     List<String> strengths, List<String> mistakes,
                                     List<String> recommendations, String feedback,
                                     String modelAnswer) {
        this.score = score;
        this.criteria = criteria;
        this.strengths = strengths;
        this.mistakes = mistakes;
        this.recommendations = recommendations;
        this.feedback = feedback;
        this.modelAnswer = modelAnswer;
    }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public InterviewCriteria getCriteria() { return criteria; }
    public void setCriteria(InterviewCriteria criteria) { this.criteria = criteria; }

    public List<String> getStrengths() { return strengths; }
    public void setStrengths(List<String> strengths) { this.strengths = strengths; }

    public List<String> getMistakes() { return mistakes; }
    public void setMistakes(List<String> mistakes) { this.mistakes = mistakes; }

    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public String getModelAnswer() { return modelAnswer; }
    public void setModelAnswer(String modelAnswer) { this.modelAnswer = modelAnswer; }
}
