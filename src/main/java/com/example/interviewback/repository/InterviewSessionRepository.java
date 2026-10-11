package com.example.interviewback.repository;

import com.example.interviewback.model.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad InterviewSession.
 *
 * Capa: REPOSITORY
 * ----------------
 * Extiende JpaRepository para obtener las operaciones CRUD estándar sin
 * necesidad de implementación manual (Spring Data las genera por reflexión).
 *
 * Métodos derivados adicionales:
 *   - findByUserId: historial de sesiones de un usuario
 *   - findByUserIdAndQuestionId: verifica si un usuario ya respondió cierta pregunta
 */
@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    /** Obtiene todas las sesiones de práctica de un usuario, ordenadas por fecha desc */
    List<InterviewSession> findByUserIdOrderByAnsweredAtDesc(Long userId);

    /** Cuenta cuántas veces un usuario respondió una pregunta específica */
    int countByUserIdAndQuestionId(Long userId, Long questionId);
}
