package com.example.interviewback.dto;

import jakarta.validation.constraints.NotBlank;

public class InterviewHintRequest {

    @NotBlank(message = "questionText no puede estar vacío")
    private String questionText;

    private String category;

    public InterviewHintRequest() {}

    public InterviewHintRequest(String questionText, String category) {
        this.questionText = questionText;
        this.category = category;
    }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
