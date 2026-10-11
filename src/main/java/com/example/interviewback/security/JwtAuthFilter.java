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
 * Filtro JWT de Spring Security.
 *
 * Se ejecuta UNA VEZ por request (OncePerRequestFilter).
 * Responsabilidad:
 *   1. Leer el header "Authorization: Bearer <token>"
 *   2. Validar el JWT con JwtUtil
 *   3. Si es válido, cargar el User de la DB y ponerlo en SecurityContextHolder
 *      → Spring Security marca el request como "autenticado"
 *   4. Continuar la cadena de filtros (filterChain.doFilter)
 *
 * Si no hay token o es inválido, el filtro no hace nada y el request
 * llega a SecurityConfig que decide si lo rechaza (401) o lo permite.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

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

        final String authHeader = request.getHeader("Authorization");

        // Si no hay header o no empieza con "Bearer ", skip
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extraer el token quitando "Bearer "
        final String token = authHeader.substring(7);

        // Validar token
        if (!jwtUtil.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extraer email del token y buscar el usuario en la DB
        final String email = jwtUtil.extractEmail(token);
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Crear objeto de autenticación y cargarlo en el SecurityContext.
        //
        // Se asigna ROLE_USER: la MISMA autoridad que otorga el
        // UserDetailsService de SecurityConfig. Antes se pasaba una lista
        // vacía, lo que hacía que un usuario autenticado por JWT tuviera
        // menos permisos que el mismo usuario autenticado por login.
        // Gracias a esta autoridad, una petición con token válido puede
        // acceder al detalle del endpoint de salud de Actuator
        // (configurado con management.endpoint.health.roles=USER).
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userOpt.get(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // Continuar con el siguiente filtro
        filterChain.doFilter(request, response);
    }
}
