package com.example.interviewback.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones.
 *
 * Captura cualquier excepción no manejada en los controllers y:
 *   1. La loguea con el stack trace completo (para debug)
 *   2. Devuelve un JSON de error legible al frontend
 *
 * Sin esto, Spring Boot devuelve un HTML de error genérico
 * que Axios no puede parsear, causando "Network Error" en el frontend.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Maneja errores de validación (@Valid / @NotNull / @NotBlank).
     * Ejemplo: questionId null, userAnswer vacío.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationError(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("[GlobalExceptionHandler] Validation error: {}", errors);

        return ResponseEntity.badRequest().body(Map.of(
                "error", "Validación fallida",
                "message", errors
        ));
    }

    /**
     * Maneja errores de negocio (404 pregunta no encontrada, 401, etc.).
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        log.warn("[GlobalExceptionHandler] ResponseStatusException: {} - {}", ex.getStatusCode(), ex.getReason());

        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
                "error", ex.getReason() != null ? ex.getReason() : "Error",
                "message", ex.getReason() != null ? ex.getReason() : "Error en la petición"
        ));
    }

    /**
     * Catch-all: cualquier excepción no manejada.
     * Loguea el stack trace completo para facilitar el debug.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        // Log completo con stack trace para que aparezca en la consola de IntelliJ
        log.error("[GlobalExceptionHandler] Error interno no manejado: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error interno del servidor",
                "message", ex.getMessage() != null ? ex.getMessage() : "Error desconocido"
        ));
    }
}
