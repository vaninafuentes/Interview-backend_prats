package com.example.interviewback.security;

/**
 * Usuario autenticado que se registra en el contexto de seguridad y se inyecta
 * en los controladores con {@code @AuthenticationPrincipal}.
 *
 * <p>Es inmutable y no contiene la contraseña: los controladores nunca reciben
 * la entidad {@link com.example.interviewback.model.User}, de modo que el hash
 * de la credencial no circula fuera de la capa de persistencia.
 */
public record AuthenticatedUser(Long id, String name, String email) {
}
