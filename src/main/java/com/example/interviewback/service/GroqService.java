package com.example.interviewback.service;

import com.example.interviewback.dto.InterviewCriteria;
import com.example.interviewback.dto.InterviewFeedbackResponse;
import com.example.interviewback.dto.GeneratedQuestionDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Servicio que integra la API de Groq para evaluar respuestas de entrevista.
 *
 * Capa: SERVICE
 * -------------
 * Groq es una plataforma de inferencia rápida compatible con la API de OpenAI.
 *
 * Endpoint: https://api.groq.com/openai/v1/chat/completions
 * Modelo:   openai/gpt-oss-20b
 *
 * La API key se obtiene de la variable de entorno GROQ_API_KEY,
 * declarada en application.properties como:
 *   groq.api-key=${GROQ_API_KEY:NOT_CONFIGURED}
 *
 * Nunca se loguea ni expone la API key.
 *
 * Manejo de errores HTTP:
 *   401 → clave inválida (error de configuración)
 *   403 → sin acceso al modelo
 *   429 → rate limit alcanzado (1 reintento automático de 3 segundos)
 *   5xx → error interno de Groq
 */
@Service
public class GroqService {

    private static final Logger log = LoggerFactory.getLogger(GroqService.class);

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL   = "qwen/qwen3.8-27b";

    // Máximo de tokens para evaluación de respuesta
    private static final int MAX_OUTPUT_TOKENS = 700;

    // Máximo de tokens para generación de preguntas (más generoso: N preguntas * ~80 tokens)
    private static final int MAX_GENERATION_TOKENS = 900;

    // Segundos de espera antes del único reintento por 429
    private static final int RETRY_WAIT_SECONDS = 3;

    @Value("${groq.api-key:NOT_CONFIGURED}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GroqService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Genera preguntas de entrevista sobre un tema usando Groq AI.
     *
     * @param topic      tema de la entrevista (ej: "React Native", "Java")
     * @param difficulty dificultad (ej: "EASY", "MEDIUM", "HARD")
     * @param count      cantidad de preguntas a generar (1-15)
     * @return lista de preguntas generadas
     */
    public List<GeneratedQuestionDto> generateQuestions(String topic, String difficulty, int count) {
        if ("NOT_CONFIGURED".equals(apiKey) || apiKey == null || apiKey.isBlank()) {
            log.warn("[GroqService] GROQ_API_KEY no configurada. Devolviendo preguntas de fallback.");
            return buildFallbackQuestions(topic, difficulty, count);
        }

        log.info("[GroqService] Generando {} preguntas | tema={} | dificultad={}", count, topic, difficulty);

        String prompt = buildGenerationPrompt(topic, difficulty, count);

        Map<String, Object> requestBody = Map.of(
                "model",      GROQ_MODEL,
                "messages",   List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature",     0.8,
                "max_tokens",      MAX_GENERATION_TOKENS,
                "response_format", Map.of("type", "json_object")
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(GROQ_API_URL, request, String.class);
            log.info("[GroqService] Preguntas generadas | status={}", response.getStatusCode());
            return parseGeneratedQuestions(response.getBody(), topic, difficulty);

        } catch (HttpStatusCodeException e) {
            int statusCode = e.getStatusCode().value();
            log.error("[GroqService] Error HTTP {} al generar preguntas | body={}",
                    statusCode, e.getResponseBodyAsString());
            if (statusCode == 429) {
                // Esperar y reintentar una vez
                log.warn("[GroqService] 429 al generar. Reintentando en {}s...", RETRY_WAIT_SECONDS);
                waitSeconds(RETRY_WAIT_SECONDS);
                try {
                    ResponseEntity<String> retry = restTemplate.postForEntity(GROQ_API_URL, request, String.class);
                    return parseGeneratedQuestions(retry.getBody(), topic, difficulty);
                } catch (Exception ex) {
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                            "La IA está temporalmente limitada. Esperá unos segundos e intentá nuevamente.");
                }
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Error al generar preguntas con la IA (" + statusCode + ").");
        } catch (Exception e) {
            log.error("[GroqService] Error inesperado al generar preguntas: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error generando las preguntas de entrevista.");
        }
    }

    // -------------------------------------------------------------------------
    // Prompt de generación
    // -------------------------------------------------------------------------

    private String buildGenerationPrompt(String topic, String difficulty, int count) {
        return String.format(
            "Generá exactamente %d preguntas de entrevista técnica sobre \"%s\" con dificultad \"%s\".%n" +
            "Respondé ÚNICAMENTE con un objeto JSON válido sin Markdown, sin texto extra:%n" +
            "{\"questions\": [{\"text\": \"...\", \"category\": \"%s\", \"difficulty\": \"%s\"},...]}%n" +
            "Reglas:%n" +
            "- Las preguntas deben ser claras, específicas y adecuadas para una entrevista de trabajo%n" +
            "- Variá los temas dentro de \"%s\" para cubrir diferentes aspectos%n" +
            "- Dificultad %s: %s%n" +
            "- NO repitas preguntas similares",
            count, topic, difficulty, topic, difficulty, topic, difficulty,
            difficulty.equals("EASY") ? "preguntas conceptuales básicas" :
            difficulty.equals("MEDIUM") ? "preguntas que requieren experiencia práctica" :
            "preguntas de diseño, arquitectura o casos complejos"
        );
    }

    private List<GeneratedQuestionDto> parseGeneratedQuestions(
            String responseBody, String topic, String difficulty) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").get(0)
                    .path("message").path("content").asText("");
            content = stripMarkdownCodeBlock(content).trim();

            JsonNode parsed = objectMapper.readTree(content);

            // El modelo devuelve { "questions": [...] }
            JsonNode questionsNode = parsed.path("questions");
            if (questionsNode.isMissingNode() || !questionsNode.isArray()) {
                log.error("[GroqService] Respuesta de generación sin campo 'questions'. Body: {}",
                        content.substring(0, Math.min(200, content.length())));
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Respuesta inesperada del servicio de IA al generar preguntas.");
            }

            List<GeneratedQuestionDto> questions = new ArrayList<>();
            questionsNode.forEach(node -> {
                String text     = node.path("text").asText("");
                String category = node.path("category").asText(topic);
                String diff     = node.path("difficulty").asText(difficulty);
                if (!text.isBlank()) {
                    questions.add(new GeneratedQuestionDto(text, category, diff));
                }
            });

            log.info("[GroqService] {} preguntas generadas correctamente", questions.size());
            return questions;

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("[GroqService] Error al parsear preguntas generadas: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudieron interpretar las preguntas generadas por la IA.");
        }
    }

    private List<GeneratedQuestionDto> buildFallbackQuestions(
            String topic, String difficulty, int count) {
        List<GeneratedQuestionDto> fallback = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            fallback.add(new GeneratedQuestionDto(
                    "Pregunta " + i + " sobre " + topic + " (IA no disponible)",
                    topic,
                    difficulty
            ));
        }
        return fallback;
    }

    /**
     * Evalúa la respuesta del candidato a una pregunta de entrevista.
     *
     * @param questionText texto de la pregunta
     * @param category     categoría de la pregunta (ej: "Java", "SQL")
     * @param difficulty   dificultad (ej: "EASY", "MEDIUM", "HARD")
     * @param userAnswer   respuesta del candidato
     * @return feedback estructurado con score, criteria, strengths, mistakes, recommendations, feedback
     */
    public InterviewFeedbackResponse evaluateAnswer(String questionText, String category,
                                                    String difficulty, String userAnswer) {
        if ("NOT_CONFIGURED".equals(apiKey) || apiKey == null || apiKey.isBlank()) {
            log.warn("[GroqService] GROQ_API_KEY no configurada. Devolviendo feedback de fallback.");
            return buildFallbackFeedback();
        }

        log.info("[GroqService] Evaluando respuesta | modelo={} | categoria={} | dificultad={}",
                GROQ_MODEL, category, difficulty);

        // Primer intento
        try {
            return callGroqApi(questionText, category, difficulty, userAnswer);
        } catch (RateLimitException e) {
            log.warn("[GroqService] 429 recibido en primer intento. Reintentando en {}s...", RETRY_WAIT_SECONDS);
            waitSeconds(RETRY_WAIT_SECONDS);
        }

        // Único reintento
        try {
            log.info("[GroqService] Reintento de llamada a Groq | modelo={}", GROQ_MODEL);
            return callGroqApi(questionText, category, difficulty, userAnswer);
        } catch (RateLimitException e) {
            log.error("[GroqService] 429 también en el reintento. Abortando.");
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "La IA está temporalmente limitada por cantidad de solicitudes. " +
                    "Esperá unos segundos e intentá nuevamente."
            );
        }
    }

    // -------------------------------------------------------------------------
    // Llamada HTTP a la API de Groq
    // -------------------------------------------------------------------------

    private InterviewFeedbackResponse callGroqApi(String questionText, String category,
                                                   String difficulty, String userAnswer) {
        String systemPrompt = buildSystemPrompt();
        String userMessage  = buildUserMessage(questionText, category, difficulty, userAnswer);

        Map<String, Object> requestBody = Map.of(
                "model",      GROQ_MODEL,
                "messages",   List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user",   "content", userMessage)
                ),
                "temperature",     0.5,
                "max_tokens",      MAX_OUTPUT_TOKENS,
                "response_format", Map.of("type", "json_object")
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(GROQ_API_URL, request, String.class);
            log.info("[GroqService] Respuesta recibida de Groq | status={}", response.getStatusCode());
            return parseGroqResponse(response.getBody());

        } catch (HttpStatusCodeException e) {
            int statusCode = e.getStatusCode().value();
            // Logueamos el cuerpo del error de Groq (sin API key)
            log.error("[GroqService] Error HTTP {} de Groq | body={}", statusCode, e.getResponseBodyAsString());

            switch (statusCode) {
                case 401 -> throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "La clave de API de Groq no es válida. Contactá al administrador."
                );
                case 403 -> throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Sin acceso al modelo de IA. Contactá al administrador."
                );
                case 429 -> throw new RateLimitException("Rate limit alcanzado");
                default -> throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Error en el servicio de IA (" + statusCode + "). Intentá nuevamente más tarde."
                );
            }

        } catch (RateLimitException e) {
            throw e; // re-lanzar para el retry
        } catch (Exception e) {
            log.error("[GroqService] Error inesperado al llamar a Groq: {}", e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error procesando la respuesta de la IA."
            );
        }
    }

    // -------------------------------------------------------------------------
    // Construcción del prompt
    // -------------------------------------------------------------------------

    private String buildSystemPrompt() {
        return """
                Sos un entrevistador técnico profesional. Evaluá la respuesta del candidato \
                considerando la categoría y dificultad de la pregunta.

                Criterios de evaluación:
                - technicalKnowledge: exactitud y profundidad técnica
                - relevance: qué tan pertinente es la respuesta a la pregunta
                - clarity: claridad de la explicación
                - communication: capacidad de comunicar ideas
                - structure: organización y coherencia

                Reglas importantes:
                - No inventés errores ni asumas información no dicha
                - Distinguí brevedad de error: una respuesta correcta pero corta no es incorrecta
                - Para preguntas de comportamiento/RRHH, no exijas conocimiento técnico
                - Sé constructivo y específico
                - strengths, mistakes y recommendations deben basarse en la respuesta real

                Respondé ÚNICAMENTE con un objeto JSON válido, sin Markdown, sin texto extra:
                {
                  "score": <número 0-10>,
                  "criteria": {
                    "technicalKnowledge": <número 0-10>,
                    "relevance": <número 0-10>,
                    "clarity": <número 0-10>,
                    "communication": <número 0-10>,
                    "structure": <número 0-10>
                  },
                  "strengths": ["<fortaleza concreta>"],
                  "mistakes": ["<error o aspecto faltante>"],
                  "recommendations": ["<consejo accionable>"],
                  "feedback": "<resumen breve en 2-3 oraciones>",
                  "modelAnswer": "<ejemplo claro, conciso y técnico de cómo un desarrollador Senior respondería idealmente esta pregunta>"
                }
                Todos los valores numéricos deben ser números, no strings.""";
    }

    private String buildUserMessage(String questionText, String category,
                                    String difficulty, String userAnswer) {
        return String.format(
                "Pregunta: %s%nCategoría: %s%nDificultad: %s%nRespuesta del candidato: %s",
                questionText, category, difficulty, userAnswer
        );
    }

    // -------------------------------------------------------------------------
    // Parseo de la respuesta de Groq
    // -------------------------------------------------------------------------

    private InterviewFeedbackResponse parseGroqResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            JsonNode choicesNode = root.path("choices");
            if (choicesNode.isMissingNode() || !choicesNode.isArray() || choicesNode.isEmpty()) {
                log.error("[GroqService] Respuesta de Groq no contiene 'choices'. Body recortado: {}",
                        responseBody.substring(0, Math.min(200, responseBody.length())));
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Respuesta inesperada del servicio de IA.");
            }

            String content = choicesNode.get(0)
                    .path("message")
                    .path("content")
                    .asText("");

            // Limpiar posibles delimitadores Markdown ```json ... ```
            content = stripMarkdownCodeBlock(content).trim();

            if (content.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "El servicio de IA devolvió una respuesta vacía.");
            }

            // Parsear el JSON del contenido
            JsonNode feedbackJson = objectMapper.readTree(content);

            int score = clamp(feedbackJson.path("score").asInt(5));

            JsonNode criteriaNode = feedbackJson.path("criteria");
            InterviewCriteria criteria = new InterviewCriteria(
                    clamp(criteriaNode.path("technicalKnowledge").asInt(5)),
                    clamp(criteriaNode.path("relevance").asInt(5)),
                    clamp(criteriaNode.path("clarity").asInt(5)),
                    clamp(criteriaNode.path("communication").asInt(5)),
                    clamp(criteriaNode.path("structure").asInt(5))
            );

            List<String> strengths        = parseStringArray(feedbackJson.path("strengths"));
            List<String> mistakes         = parseStringArray(feedbackJson.path("mistakes"));
            List<String> recommendations  = parseStringArray(feedbackJson.path("recommendations"));
            String feedback               = feedbackJson.path("feedback").asText("Sin feedback disponible.");
            String modelAnswer            = feedbackJson.path("modelAnswer").asText("Considerá estructurar tu respuesta explicando el concepto base, un caso de uso práctico y posibles consideraciones de rendimiento.");

            return new InterviewFeedbackResponse(score, criteria, strengths, mistakes, recommendations, feedback, modelAnswer);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("[GroqService] Error al parsear respuesta de Groq: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo interpretar la respuesta del servicio de IA.");
        }
    }

    // -------------------------------------------------------------------------
    // Utilidades
    // -------------------------------------------------------------------------

    /**
     * Elimina delimitadores Markdown ```json ... ``` o ``` ... ``` del contenido.
     */
    private String stripMarkdownCodeBlock(String content) {
        if (content == null) return "";
        String stripped = content.strip();
        if (stripped.startsWith("```")) {
            int firstNewline = stripped.indexOf('\n');
            if (firstNewline != -1) {
                stripped = stripped.substring(firstNewline + 1);
            }
            if (stripped.endsWith("```")) {
                stripped = stripped.substring(0, stripped.lastIndexOf("```"));
            }
        }
        return stripped;
    }

    private List<String> parseStringArray(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(item -> result.add(item.asText()));
        }
        return result;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(10, value));
    }

    private void waitSeconds(int seconds) {
        try {
            Thread.sleep((long) seconds * 1000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    // -------------------------------------------------------------------------
    // Generación de pistas (Hints)
    // -------------------------------------------------------------------------

    public String generateHint(String questionText, String category) {
        if ("NOT_CONFIGURED".equals(apiKey) || apiKey == null || apiKey.isBlank()) {
            return "Pensá en los fundamentos del concepto, sus ventajas/desventajas y un caso real donde lo aplicarías.";
        }

        String prompt = String.format(
                "Sos un mentor en una entrevista de trabajo para la categoría \"%s\". " +
                "El candidato está dudando frente a esta pregunta: \"%s\". " +
                "Escribí una pista (hint) sutil, de máximo 2 oraciones, que lo guíe sobre en qué enfocar su respuesta " +
                "sin darle la respuesta servida. Respondé ÚNICAMENTE un JSON: {\"hint\": \"...\"}",
                category != null ? category : "General",
                questionText
        );

        Map<String, Object> requestBody = Map.of(
                "model", GROQ_MODEL,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.7,
                "max_tokens", 150,
                "response_format", Map.of("type", "json_object")
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(GROQ_API_URL, request, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText("");
            content = stripMarkdownCodeBlock(content).trim();
            JsonNode parsed = objectMapper.readTree(content);
            return parsed.path("hint").asText("Pensá en los fundamentos de " + category + " y cómo explicarlo paso a paso.");
        } catch (Exception e) {
            log.warn("[GroqService] Fallback hint debido a error: {}", e.getMessage());
            return "Enfocate en explicar qué es, por qué se usa y qué problema resuelve.";
        }
    }

    // -------------------------------------------------------------------------
    // Feedback de fallback (sin API key)
    // -------------------------------------------------------------------------

    private InterviewFeedbackResponse buildFallbackFeedback() {
        InterviewCriteria criteria = new InterviewCriteria(5, 5, 5, 5, 5);
        return new InterviewFeedbackResponse(
                5,
                criteria,
                Collections.singletonList("Tu respuesta fue recibida correctamente."),
                Collections.singletonList("La evaluación con IA no está disponible en este momento."),
                List.of(
                        "Configurá la variable de entorno GROQ_API_KEY antes de correr el backend.",
                        "Conseguí una key gratuita en console.groq.com"
                ),
                "La evaluación automática no está disponible. Verificá que GROQ_API_KEY " +
                "esté definida como variable de entorno antes de iniciar el servidor."
        );
    }

    // -------------------------------------------------------------------------
    // Excepción interna para rate limit (uso del retry)
    // -------------------------------------------------------------------------

    private static class RateLimitException extends RuntimeException {
        public RateLimitException(String message) {
            super(message);
        }
    }
}
