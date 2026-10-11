package com.example.interviewback.repository;

import com.example.interviewback.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad User.
 *
 * Spring Data genera automáticamente las implementaciones de:
 *   - findByEmail  → SELECT * FROM users WHERE email = ?
 *   - existsByEmail → SELECT COUNT(*) FROM users WHERE email = ? > 0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Busca un usuario por su email. Usado en el proceso de login. */
    Optional<User> findByEmail(String email);

    /** Verifica si ya existe un usuario con ese email. Usado en el registro. */
    boolean existsByEmail(String email);
}
