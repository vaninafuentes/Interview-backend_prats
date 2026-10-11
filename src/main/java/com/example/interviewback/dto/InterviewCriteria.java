package com.example.interviewback.dto;

/**
 * DTO que representa los criterios de evaluación individuales de la IA.
 *
 * Capa: DTO
 * ---------
 * Forma parte de InterviewFeedbackResponse.
 * Cada campo es una puntuación de 0 a 10 para un aspecto específico
 * de la respuesta del candidato.
 *
 * {
 *   "technicalKnowledge": 9,
 *   "relevance": 8,
 *   "clarity": 8,
 *   "communication": 7,
 *   "structure": 8
 * }
 */
public class InterviewCriteria {

    private int technicalKnowledge;
    private int relevance;
    private int clarity;
    private int communication;
    private int structure;

    public InterviewCriteria() {}

    public InterviewCriteria(int technicalKnowledge, int relevance, int clarity,
                              int communication, int structure) {
        this.technicalKnowledge = technicalKnowledge;
        this.relevance = relevance;
        this.clarity = clarity;
        this.communication = communication;
        this.structure = structure;
    }

    public int getTechnicalKnowledge() { return technicalKnowledge; }
    public void setTechnicalKnowledge(int technicalKnowledge) { this.technicalKnowledge = technicalKnowledge; }

    public int getRelevance() { return relevance; }
    public void setRelevance(int relevance) { this.relevance = relevance; }

    public int getClarity() { return clarity; }
    public void setClarity(int clarity) { this.clarity = clarity; }

    public int getCommunication() { return communication; }
    public void setCommunication(int communication) { this.communication = communication; }

    public int getStructure() { return structure; }
    public void setStructure(int structure) { this.structure = structure; }
}
