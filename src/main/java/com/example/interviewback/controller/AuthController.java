package com.example.interviewback.controller;

import com.example.interviewback.dto.AuthResponse;
import com.example.interviewback.dto.LoginRequest;
import com.example.interviewback.dto.RegisterRequest;
import com.example.interviewback.security.AuthenticatedUser;
import com.example.interviewback.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de autenticación.
 *
 * <p>El registro y el inicio de sesión son públicos y devuelven un token JWT.
 * {@code /me} exige token válido y permite al cliente comprobar si su sesión
 * sigue activa.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Devuelve el usuario indicado por el token.
     *
     * <p>No hace falta comprobar que haya usuario: las rutas que no sean
     * públicas exigen autenticación en {@code SecurityConfig}, así que una
     * petición sin token válido se rechaza antes de llegar a este método.
     */
    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUser> me(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(user);
    }
}
