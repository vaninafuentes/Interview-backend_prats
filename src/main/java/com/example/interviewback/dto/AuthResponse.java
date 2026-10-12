package com.example.interviewback.dto;

/**
 * DTO de salida para las respuestas de autenticación.
 *
 * Devuelto por:
 *   POST /api/auth/register → 201 Created
 *   POST /api/auth/login    → 200 OK
 *
 * Contiene el JWT + datos básicos del usuario para que
 * el front no necesite hacer otra petición para obtenerlos.
 */
public class AuthResponse {

    /** JWT firmado con HMAC-SHA. El cliente lo guarda para autenticar las demas peticiones. */
    private String token;

    /** ID del usuario en la base de datos */
    private Long id;

    /** Nombre del usuario */
    private String name;

    /** Email del usuario */
    private String email;

    public AuthResponse() {}

    public AuthResponse(String token, Long id, String name, String email) {
        this.token = token;
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
