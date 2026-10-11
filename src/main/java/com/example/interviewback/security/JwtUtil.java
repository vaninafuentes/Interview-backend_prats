package com.example.interviewback.security;

import com.example.interviewback.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utilidad para generar y validar JSON Web Tokens (JWT).
 *
 * Algoritmo: HS256 (HMAC + SHA-256)
 * La clave secreta se lee de application.properties para no hardcodearla.
 *
 * Estructura del JWT:
 *   Header: { alg: "HS256", typ: "JWT" }
 *   Payload: { sub: "email", name: "...", iat: ..., exp: ... }
 *   Signature: HMAC-SHA256(base64(header) + "." + base64(payload), secret)
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    /** Genera la clave criptográfica a partir del secreto configurado */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Genera un JWT firmado para el usuario dado.
     *
     * @param user el usuario autenticado/registrado
     * @return token JWT como String
     */
    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())          // "sub" → email del usuario
                .claim("name", user.getName())     // claim custom con el nombre
                .claim("id", user.getId())         // claim custom con el ID
                .issuedAt(new Date())              // "iat" → fecha de emisión
                .expiration(new Date(System.currentTimeMillis() + expirationMs)) // "exp"
                .signWith(getSigningKey())         // firma con HS256
                .compact();
    }

    /**
     * Extrae el email (subject) del JWT.
     *
     * @param token JWT a parsear
     * @return email del usuario
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Valida que el token sea correcto y no haya expirado.
     *
     * @param token JWT a validar
     * @return true si es válido
     */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token); // lanza excepción si es inválido o expirado
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Parsea y retorna los claims del JWT */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
