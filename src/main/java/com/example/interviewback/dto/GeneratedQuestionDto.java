package com.example.interviewback.dto;

/**
 * DTO que representa una pregunta generada dinámicamente por Groq AI.
 *
 * A diferencia de la entidad Question (que tiene id y vive en la DB),
 * esta es una pregunta efímera: se genera en tiempo real y se descarta
 * después de la sesión. El frontend la almacena en memoria.
 *
 * {
 *   "text":       "¿Qué diferencia hay entre state y props en React?",
 *   "category":   "React Native",
 *   "difficulty": "MEDIUM"
 * }
 */
public class GeneratedQuestionDto {

    private String text;
    private String category;
    private String difficulty;

    public GeneratedQuestionDto() {}

    public GeneratedQuestionDto(String text, String category, String difficulty) {
        this.text = text;
        this.category = category;
        this.difficulty = difficulty;
    }

    public String getText()           { return text; }
    public void setText(String t)     { this.text = t; }

    public String getCategory()       { return category; }
    public void setCategory(String c) { this.category = c; }

    public String getDifficulty()       { return difficulty; }
    public void setDifficulty(String d) { this.difficulty = d; }
}
