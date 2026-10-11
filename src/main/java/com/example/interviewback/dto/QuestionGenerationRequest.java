package com.example.interviewback.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para la solicitud de generación de preguntas por IA.
 *
 * Endpoint: POST /api/interview/generate
 *
 * {
 *   "topic":      "React Native",
 *   "difficulty": "MEDIUM",
 *   "count":      5
 * }
 */
public class QuestionGenerationRequest {

    @NotBlank(message = "El tema es obligatorio")
    private String topic;

    @NotBlank(message = "La dificultad es obligatoria")
    private String difficulty;

    @NotNull(message = "La cantidad de preguntas es obligatoria")
    @Min(value = 1, message = "Mínimo 1 pregunta")
    @Max(value = 15, message = "Máximo 15 preguntas")
    private Integer count;

    public QuestionGenerationRequest() {}

    public QuestionGenerationRequest(String topic, String difficulty, Integer count) {
        this.topic = topic;
        this.difficulty = difficulty;
        this.count = count;
    }

    public String getTopic()       { return topic; }
    public void setTopic(String t) { this.topic = t; }

    public String getDifficulty()       { return difficulty; }
    public void setDifficulty(String d) { this.difficulty = d; }

    public Integer getCount()       { return count; }
    public void setCount(Integer c) { this.count = c; }
}
