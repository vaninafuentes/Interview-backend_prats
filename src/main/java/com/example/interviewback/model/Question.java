package com.example.interviewback.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA que representa una pregunta de entrevista técnica.
 *
 * Capa: MODEL
 * -----------
 * La capa de modelo define las entidades del dominio que se mapean
 * directamente a tablas de la base de datos mediante JPA/Hibernate.
 * Cada instancia de esta clase corresponde a una fila en la tabla "questions".
 *
 * Campos:
 *   - id:         identificador único, auto-generado por MySQL (AUTO_INCREMENT)
 *   - text:       el texto completo de la pregunta
 *   - category:   categoría temática (ej: "Java", "React", "SQL")
 *   - difficulty: nivel de dificultad (ej: "EASY", "MEDIUM", "HARD")
 */
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // columnDefinition = TEXT permite textos largos (más de 255 chars por defecto)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String difficulty;

    /**
     * Constructor vacío requerido por la especificación JPA.
     * Hibernate necesita instanciar la entidad vía reflexión.
     */
    public Question() {
    }

    /**
     * Constructor de conveniencia para crear preguntas con todos los campos.
     * Útil para el seeder de datos de prueba y tests.
     */
    public Question(String text, String category, String difficulty) {
        this.text = text;
        this.category = category;
        this.difficulty = difficulty;
    }

    // --- Getters y Setters ---
    // JPA requiere accesores para leer/escribir los campos de la entidad.
    // Jackson (serialización JSON) también los usa para armar la respuesta HTTP.

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
}
