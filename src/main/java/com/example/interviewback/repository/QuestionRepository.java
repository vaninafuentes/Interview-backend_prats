package com.example.interviewback.repository;

import com.example.interviewback.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA para la entidad Question.
 *
 * Capa: REPOSITORY
 * ----------------
 * La capa de repositorio se encarga del acceso a datos (Data Access Layer).
 * Al extender JpaRepository, Spring Data genera automáticamente en runtime
 * la implementación de todos los métodos CRUD estándar:
 *
 *   - findAll()       → SELECT * FROM questions
 *   - findById(id)    → SELECT * FROM questions WHERE id = ?
 *   - save(entity)    → INSERT / UPDATE
 *   - deleteById(id)  → DELETE FROM questions WHERE id = ?
 *   - count()         → SELECT COUNT(*) FROM questions
 *
 * No necesitamos escribir ni una línea de SQL gracias a Spring Data JPA.
 * Si en el futuro necesitamos queries custom, podemos agregar métodos
 * como findByCategory(String category) y Spring los implementa por convención.
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    // Spring Data genera la implementación automáticamente.
    // Agregar métodos custom acá si se necesitan en el futuro, por ejemplo:
    // List<Question> findByCategory(String category);
    // List<Question> findByDifficulty(String difficulty);
}
