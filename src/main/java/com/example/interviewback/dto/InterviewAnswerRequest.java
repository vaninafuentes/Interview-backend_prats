package com.example.interviewback.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Respuesta del usuario a una pregunta de entrevista.
 *
 * <p>Admite dos modos, que resuelve {@code InterviewService}:
 * <ul>
 *   <li>Pregunta existente: se envía {@code questionId} y el texto se lee de la base.</li>
 *   <li>Pregunta generada al vuelo: se envían {@code questionText}, {@code questionCategory}
 *       y {@code questionDifficulty}.</li>
 * </ul>
 */
public class InterviewAnswerRequest {

    /** ID de pregunta de DB. Null para preguntas generadas por IA. */
    private Long questionId;

    /** Texto de la pregunta. Requerido si questionId es null. */
    private String questionText;

    /** Categoría/tema. Requerido si questionId es null. */
    private String questionCategory;

    /** Dificultad. Requerido si questionId es null. */
    private String questionDifficulty;

    @NotBlank(message = "La respuesta no puede estar vacía")
    private String userAnswer;

    public InterviewAnswerRequest() {}

    // Getters y setters

    public Long getQuestionId()            { return questionId; }
    public void setQuestionId(Long id)     { this.questionId = id; }

    public String getQuestionText()            { return questionText; }
    public void setQuestionText(String t)      { this.questionText = t; }

    public String getQuestionCategory()        { return questionCategory; }
    public void setQuestionCategory(String c)  { this.questionCategory = c; }

    public String getQuestionDifficulty()      { return questionDifficulty; }
    public void setQuestionDifficulty(String d){ this.questionDifficulty = d; }

    public String getUserAnswer()              { return userAnswer; }
    public void setUserAnswer(String a)        { this.userAnswer = a; }
}
