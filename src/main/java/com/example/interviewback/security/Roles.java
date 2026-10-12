package com.example.interviewback.security;

/**
 * Roles del sistema.
 *
 * <p>El sistema tiene un único tipo de usuario, por lo que no existen niveles de
 * acceso distintos y las reglas de autorización solo exigen estar autenticado.
 * El rol se conserva porque Spring Security lo necesita para construir el
 * usuario autenticado y porque el detalle del endpoint de salud está limitado a
 * quien lo posee.
 *
 * <p>Spring espera el prefijo {@code ROLE_}. En la configuración de Actuator se
 * escribe sin el prefijo ({@code management.endpoint.health.roles=USER}).
 */
public final class Roles {

    public static final String USER = "ROLE_USER";

    private Roles() {
    }
}
