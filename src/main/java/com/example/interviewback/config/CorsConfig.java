package com.example.interviewback.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Política de CORS.
 *
 * <p>Solo afecta a clientes web: las aplicaciones móviles nativas no envían
 * cabecera {@code Origin} y por lo tanto el navegador no aplica esta política.
 * Los orígenes se configuran con la variable de entorno
 * {@code CORS_ALLOWED_ORIGINS} (lista separada por comas); el valor por defecto
 * es permisivo para facilitar el desarrollo.
 *
 * <p>Nota: permitir cualquier origen es aceptable en esta API porque no usa
 * cookies de sesión —la autenticación viaja en la cabecera {@code Authorization},
 * que un sitio ajeno no puede agregar—, así que el escenario que CORS previene
 * (que otro sitio aproveche las credenciales del navegador) no existe. Al
 * desplegar un cliente web conviene restringir la lista igualmente.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins:*}") String allowedOrigins) {
        this.allowedOrigins = allowedOrigins.split(",");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
