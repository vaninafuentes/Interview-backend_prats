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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Registro e inicio de sesión.
 *
 * <p>Las contraseñas se almacenan hasheadas con BCrypt y nunca se guardan ni se
 * devuelven en texto plano.
 */
@Service
public class AuthService {

    // Un mismo mensaje para los dos casos de fallo evita revelar si un email
    // está registrado: un atacante no puede distinguir "no existe" de
    // "contraseña incorrecta", lo que le impide enumerar usuarios.
    private static final String CREDENCIALES_INVALIDAS = "Email o contraseña incorrectos.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Registra un usuario nuevo y devuelve un token junto con sus datos.
     *
     * <p>Va dentro de una transacción porque verifica la existencia del email y
     * después inserta: sin ella, dos registros simultáneos con el mismo email
     * podrían superar la verificación antes de que alguno inserte.
     *
     * @throws ResponseStatusException 409 si el email ya está registrado
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese email.");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        User user = userRepository.save(new User(request.getName(), request.getEmail(), hashedPassword));

        return new AuthResponse(jwtUtil.generateToken(user), user.getId(), user.getName(), user.getEmail());
    }

    /**
     * Verifica las credenciales y devuelve un token junto con los datos del usuario.
     *
     * <p>Solo lee, por eso la transacción es {@code readOnly}.
     *
     * @throws ResponseStatusException 401 si el email no existe o la contraseña es incorrecta
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, CREDENCIALES_INVALIDAS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, CREDENCIALES_INVALIDAS);
        }

        return new AuthResponse(jwtUtil.generateToken(user), user.getId(), user.getName(), user.getEmail());
    }
}
