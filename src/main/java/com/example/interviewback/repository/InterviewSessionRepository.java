package com.example.interviewback.repository;

import com.example.interviewback.model.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Acceso a datos de las sesiones de práctica. */
@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    List<InterviewSession> findByUserIdOrderByAnsweredAtDesc(Long userId);

    int countByUserIdAndQuestionId(Long userId, Long questionId);
}
