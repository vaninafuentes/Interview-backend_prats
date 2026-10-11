package com.example.interviewback.service;

import com.example.interviewback.model.Question;
import com.example.interviewback.repository.QuestionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


/**
 * Servicio de lógica de negocio para preguntas de entrevista.
 *
 * Capa: SERVICE
 * -------------
 * La capa de servicio contiene la lógica de negocio de la aplicación.
 * Actúa como intermediario entre el Controller (que recibe las peticiones HTTP)
 * y el Repository (que accede a la base de datos).
 *
 * ¿Por qué separar en una capa de servicio?
 *   1. El controller no debería tener lógica de negocio, solo manejar HTTP
 *   2. Facilita testing unitario (se puede mockear el repository)
 *   3. Permite reutilizar la lógica desde múltiples controllers si es necesario
 *   4. Es donde se agregan validaciones, transformaciones y reglas de negocio
 *
 * Inyección de dependencias:
 *   Se usa inyección por constructor (recomendada por Spring) en vez de @Autowired
 *   en campo. Esto hace la clase más fácil de testear y las dependencias explícitas.
 */
@Service
public class QuestionService {

    private final QuestionRepository questionRepository;

    // Inyección por constructor: Spring detecta automáticamente el bean
    // QuestionRepository y lo inyecta aquí. No necesitamos @Autowired
    // cuando hay un solo constructor.
    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    /**
     * Obtiene todas las preguntas de la base de datos.
     *
     * @return lista de todas las preguntas (puede estar vacía)
     */
    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }

    /**
     * Busca una pregunta por su ID.
     *
     * @param id el identificador de la pregunta
     * @return la pregunta encontrada
     * @throws ResponseStatusException 404 NOT_FOUND si no existe la pregunta
     */
    public Question getQuestionById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pregunta no encontrada con id: " + id
                ));
    }

    /**
     * Crea y persiste una nueva pregunta en la base de datos.
     *
     * @param question la entidad con los datos de la nueva pregunta
     * @return la pregunta guardada (con el ID asignado por la DB)
     */
    public Question createQuestion(Question question) {
        return questionRepository.save(question);
    }
}
