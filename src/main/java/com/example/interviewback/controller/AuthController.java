package com.example.interviewback.controller;

import com.example.interviewback.dto.AuthResponse;
import com.example.interviewback.dto.LoginRequest;
import com.example.interviewback.dto.RegisterRequest;
import com.example.interviewback.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST de autenticación.
 *
 * Endpoints públicos (no requieren JWT):
 *   POST /api/auth/register → registra un nuevo usuario → 201 Created + AuthResponse
 *   POST /api/auth/login    → autentica un usuario    → 200 OK     + AuthResponse
 *
 * @Valid activa la validación automática de los DTOs (Bean Validation).
 * Si un campo falla la validación, Spring devuelve 400 Bad Request automáticamente.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/register
     * Registra un nuevo usuario y devuelve un JWT.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login
     * Autentica un usuario y devuelve un JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/auth/me
     * Valida el token JWT y retorna los datos del usuario autenticado desde el SecurityContext.
     * Requiere header Authorization: Bearer <token>.
     */
    @org.springframework.web.bind.annotation.GetMapping("/me")
    public ResponseEntity<?> getAuthenticatedUser(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof com.example.interviewback.model.User user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(java.util.Map.of(
                    "error", "No autenticado",
                    "message", "Token JWT ausente o inválido"
            ));
        }

        return ResponseEntity.ok(java.util.Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail()
        ));
    }
}

