package com.example.interviewback.service;

import com.example.interviewback.dto.AuthResponse;
import com.example.interviewback.dto.LoginRequest;
import com.example.interviewback.dto.RegisterRequest;
import com.example.interviewback.model.User;
import com.example.interviewback.repository.UserRepository;
import com.example.interviewback.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Servicio de autenticación.
 *
 * Capa: SERVICE
 * -------------
 * Contiene la lógica de negocio para register y login.
 * Delega al UserRepository para persistencia y a JwtUtil para tokens.
 *
 * Seguridad:
 *   - Las contraseñas se hashean con BCrypt ANTES de persistir
 *   - BCrypt verifica hashes en login sin necesidad de desencriptar
 *   - Nunca se guarda ni devuelve la contraseña en texto plano
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Registra un nuevo usuario.
     *
     * Flujo:
     *   1. Verifica que el email no esté registrado
     *   2. Hashea la contraseña con BCrypt
     *   3. Guarda el User en la base de datos
     *   4. Genera y devuelve un JWT + datos del usuario
     *
     * @param request datos del nuevo usuario
     * @return AuthResponse con JWT + info del usuario
     * @throws ResponseStatusException 409 CONFLICT si el email ya existe
     */
    public AuthResponse register(RegisterRequest request) {
        // Verificar email único
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe una cuenta con ese email."
            );
        }

        // Hashear contraseña (BCrypt agrega salt automáticamente)
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // Crear y persistir el usuario
        User user = new User(request.getName(), request.getEmail(), hashedPassword);
        user = userRepository.save(user);

        // Generar JWT y armar respuesta
        String token = jwtUtil.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail());
    }

    /**
     * Autentica un usuario existente.
     *
     * Flujo:
     *   1. Busca el usuario por email
     *   2. Verifica la contraseña con BCrypt
     *   3. Genera y devuelve un JWT + datos del usuario
     *
     * @param request credenciales del usuario
     * @return AuthResponse con JWT + info del usuario
     * @throws ResponseStatusException 401 UNAUTHORIZED si las credenciales son incorrectas
     */
    public AuthResponse login(LoginRequest request) {
        // Buscar usuario por email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Email o contraseña incorrectos."
                ));

        // Verificar contraseña con BCrypt (compara hash sin desencriptar)
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Email o contraseña incorrectos."
            );
        }

        // Generar JWT y armar respuesta
        String token = jwtUtil.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail());
    }
}
