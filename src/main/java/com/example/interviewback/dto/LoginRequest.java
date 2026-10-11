package com.example.interviewback.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para el endpoint POST /api/auth/login.
 *
 * @NotBlank → no puede ser null, vacío ni solo espacios
 * @Email    → valida formato de email
 * @Size     → valida longitud mínima de contraseña
 */
public class LoginRequest {

    @NotBlank(message = "El email es requerido.")
    @Email(message = "Ingresa un email válido.")
    private String email;

    @NotBlank(message = "La contraseña es requerida.")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres.")
    private String password;

    public LoginRequest() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
