package com.example.interviewback.dto;

import java.util.List;

/**
 * Resultado de la evaluación de una respuesta: puntuación global, puntuaciones
 * por criterio, fortalezas, errores, recomendaciones y un resumen.
 *
 * <p>{@code score} se expresa de 0 a 10.
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
