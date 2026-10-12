package com.example.interviewback.service;

import com.example.interviewback.model.Question;
import com.example.interviewback.repository.QuestionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Consulta y creación de preguntas de entrevista.
 */
@Service
public class QuestionService {

    private final QuestionRepository questionRepository;

    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Transactional(readOnly = true)
    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }

    /**
     * @throws ResponseStatusException 404 si la pregunta no existe
     */
    @Transactional(readOnly = true)
    public Question getQuestionById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pregunta no encontrada con id: " + id
                ));
    }

    @Transactional
    public Question createQuestion(Question question) {
        return questionRepository.save(question);
    }
}
