package com.example.interviewback.config;

import com.example.interviewback.model.Question;
import com.example.interviewback.repository.QuestionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seed de datos iniciales para la tabla de preguntas.
 *
 * Capa: CONFIG
 * ------------
 * Implementa CommandLineRunner, una interfaz de Spring Boot que se ejecuta
 * automáticamente al arrancar la aplicación (después de que el contexto
 * se haya inicializado completamente).
 *
 * Lógica:
 *   1. Verifica si la tabla "questions" está vacía (count == 0)
 *   2. Si está vacía, inserta preguntas de prueba
 *   3. Si ya tiene datos, no hace nada (idempotente)
 *
 * Esto es útil para desarrollo: cada vez que arrancas la app tenés
 * datos de prueba listos sin necesidad de scripts SQL manuales.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final QuestionRepository questionRepository;

    public DataSeeder(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Override
    public void run(String... args) {
        // Solo insertar si la tabla está vacía para no duplicar datos
        if (questionRepository.count() > 0) {
            System.out.println("📋 La tabla de preguntas ya tiene datos. Seed omitido.");
            return;
        }

        System.out.println("🌱 Insertando preguntas de prueba...");

        List<Question> questions = List.of(
                // --- Java ---
                new Question(
                        "¿Cuál es la diferencia entre una clase abstracta y una interfaz en Java?",
                        "Java", "EASY"
                ),
                new Question(
                        "Explicá el concepto de Garbage Collection en Java. ¿Cómo funciona el GC?",
                        "Java", "MEDIUM"
                ),
                new Question(
                        "¿Qué es el patrón Singleton y cómo se implementa de forma thread-safe en Java?",
                        "Java", "MEDIUM"
                ),

                // --- React ---
                new Question(
                        "¿Qué es el Virtual DOM en React y por qué mejora el rendimiento?",
                        "React", "EASY"
                ),
                new Question(
                        "Explicá la diferencia entre useEffect y useLayoutEffect en React.",
                        "React", "MEDIUM"
                ),
                new Question(
                        "¿Cómo funciona la reconciliación (reconciliation) en React? Describí el algoritmo de diffing.",
                        "React", "HARD"
                ),

                // --- SQL ---
                new Question(
                        "¿Cuál es la diferencia entre INNER JOIN, LEFT JOIN y RIGHT JOIN?",
                        "SQL", "EASY"
                ),
                new Question(
                        "Explicá qué es un índice en base de datos y cuándo conviene usarlo.",
                        "SQL", "MEDIUM"
                ),

                // --- Algoritmos ---
                new Question(
                        "¿Qué es la notación Big O y por qué es importante para evaluar algoritmos?",
                        "Algoritmos", "EASY"
                ),
                new Question(
                        "Implementá un algoritmo para detectar si una linked list tiene un ciclo. ¿Cuál es su complejidad?",
                        "Algoritmos", "HARD"
                )
        );

        questionRepository.saveAll(questions);
        System.out.println("✅ Se insertaron " + questions.size() + " preguntas de prueba.");
    }
}
