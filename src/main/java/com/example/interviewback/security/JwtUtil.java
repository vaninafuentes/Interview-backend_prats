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
 * Genera y valida los tokens JWT.
 *
 * <p>La firma es HMAC-SHA. El algoritmo concreto lo elige la librería (jjwt)
 * según el tamaño de la clave: con la clave actual de 48 bytes usa <b>HS384</b>;
 * con una de 32 bytes usaría HS256. Una clave de menos de 32 bytes es rechazada,
 * lo que evita firmas débiles.
 *
 * <pre>
 *   Header:    { alg: "HS384", typ: "JWT" }
 *   Payload:   { sub: "email", name: "...", id: ..., iat: ..., exp: ... }
 *   Signature: HMAC-SHA384( base64(header) + "." + base64(payload), clave )
 * </pre>
 *
 * <p>El payload <b>no está cifrado</b>, solo codificado en Base64: cualquiera
 * puede leerlo. Lo que impide modificarlo sin la clave es la firma.
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
                .signWith(getSigningKey())         // firma con HMAC-SHA; jjwt elige el algoritmo segun la clave
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
