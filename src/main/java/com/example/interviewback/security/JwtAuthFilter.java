package com.example.interviewback.security;

import com.example.interviewback.model.User;
import com.example.interviewback.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Autentica cada petición a partir del token JWT.
 *
 * <p>Lee la cabecera {@code Authorization: Bearer <token>}, la valida y registra
 * al usuario en el contexto de seguridad. Si el token falta, es inválido o está
 * vencido, <b>no rechaza la petición</b>: la deja pasar sin autenticar y es
 * {@code SecurityConfig} quien decide según las reglas de acceso, de modo que la
 * autorización queda definida en un solo lugar.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extraerToken(request);
        if (token != null) {
            autenticar(token, request);
        }
        filterChain.doFilter(request, response);
    }

    private String extraerToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            return null;
        }
        return header.substring(PREFIX.length());
    }

    private void autenticar(String token, HttpServletRequest request) {
        if (!jwtUtil.isTokenValid(token)) {
            return;
        }

        // La firma ya se validó criptográficamente. Esta consulta sirve para
        // obtener los datos del usuario y para que un cambio de permisos tenga
        // efecto de inmediato, sin esperar a que el token expire.
        Optional<User> encontrado = userRepository.findByEmail(jwtUtil.extractEmail(token));
        if (encontrado.isEmpty()) {
            return;
        }

        User user = encontrado.get();
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getName(), user.getEmail());

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(Roles.USER))
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
