package com.example.interviewback.repository;

import com.example.interviewback.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Acceso a datos de preguntas.
 *
 * <p>Hereda de {@code JpaRepository} las operaciones CRUD; Spring Data genera la
 * implementación en tiempo de ejecución. Las consultas por otros criterios se
 * agregan declarando métodos como {@code findByCategory(String)}.
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
}
