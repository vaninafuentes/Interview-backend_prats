package com.example.interviewback.dto;

public class InterviewHintResponse {

    private String hint;

    public InterviewHintResponse() {}

    public InterviewHintResponse(String hint) {
        this.hint = hint;
    }

    public String getHint() { return hint; }
    public void setHint(String hint) { this.hint = hint; }
}
