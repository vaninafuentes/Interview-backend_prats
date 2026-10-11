package com.example.interviewback.model;

import jakarta.persistence.*;

/**
 * Entidad JPA que representa un usuario registrado en el sistema.
 *
 * Tabla: users
 * Campos:
 *   - id       → clave primaria auto-generada
 *   - name     → nombre del usuario
 *   - email    → email único (usado para login)
 *   - password → contraseña hasheada con BCrypt (NUNCA texto plano)
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // unique = true garantiza que no haya dos usuarios con el mismo email
    @Column(nullable = false, unique = true)
    private String email;

    // La contraseña se almacena hasheada. BCrypt genera hashes de 60 chars.
    @Column(nullable = false)
    private String password;

    /** Constructor vacío requerido por JPA */
    public User() {}

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    // --- Getters y Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
