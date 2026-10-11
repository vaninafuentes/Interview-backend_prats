package com.example.interviewback.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO que representa un registro individual del historial de prácticas del usuario.
 */
public class InterviewHistoryDto {

    private Long id;
    private Long questionId;
    private String questionText;
    private String category;
    private String difficulty;
    private String userAnswer;
    private String feedback;
    private Integer score;
    private List<String> tips;
    private String modelAnswer;
    private LocalDateTime answeredAt;

    public InterviewHistoryDto() {}

    public InterviewHistoryDto(Long id, Long questionId, String questionText,
                               String category, String difficulty, String userAnswer,
                               String feedback, Integer score, List<String> tips,
                               String modelAnswer, LocalDateTime answeredAt) {
        this.id = id;
        this.questionId = questionId;
        this.questionText = questionText;
        this.category = category;
        this.difficulty = difficulty;
        this.userAnswer = userAnswer;
        this.feedback = feedback;
        this.score = score;
        this.tips = tips;
        this.modelAnswer = modelAnswer;
        this.answeredAt = answeredAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public List<String> getTips() { return tips; }
    public void setTips(List<String> tips) { this.tips = tips; }

    public String getModelAnswer() { return modelAnswer; }
    public void setModelAnswer(String modelAnswer) { this.modelAnswer = modelAnswer; }

    public LocalDateTime getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(LocalDateTime answeredAt) { this.answeredAt = answeredAt; }
}
