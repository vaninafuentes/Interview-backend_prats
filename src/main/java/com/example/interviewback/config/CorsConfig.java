package com.example.interviewback.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración global de CORS (Cross-Origin Resource Sharing).
 *
 * Capa: CONFIG
 * ------------
 * Esta clase configura la política de CORS del backend.
 *
 * ¿Por qué es necesaria?
 *   El frontend (Expo/React Native) corre en un origen diferente al backend
 *   (distinto puerto o IP). Sin CORS habilitado, el navegador/WebView bloquea
 *   las peticiones HTTP del front al back por seguridad (Same-Origin Policy).
 *
 * Configuración actual (desarrollo):
 *   - allowedOrigins("*"): permite cualquier origen
 *   - Métodos: GET, POST, PUT, DELETE, OPTIONS
 *   - Headers: cualquiera
 *
 * IMPORTANTE: En producción, cambiar allowedOrigins("*") por los dominios
 * específicos de tu aplicación para mayor seguridad.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry
                // Aplica CORS a todas las rutas de la API
                .addMapping("/**")
                // En desarrollo permitimos todos los orígenes.
                // En producción, restringir a dominios específicos:
                // .allowedOrigins("https://mi-app.com")
                .allowedOrigins("*")
                // Métodos HTTP permitidos
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // Permitir cualquier header (Content-Type, Authorization, etc.)
                .allowedHeaders("*");
    }
}
