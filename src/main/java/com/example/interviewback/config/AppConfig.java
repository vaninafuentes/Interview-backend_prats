package com.example.interviewback.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuración de beans de infraestructura de la aplicación.
 *
 * Capa: CONFIG
 * ------------
 * Define los beans compartidos que no pertenecen a ningún módulo específico.
 *
 * RestTemplate:
 *   Cliente HTTP síncrono de Spring para hacer llamadas a APIs externas.
 *   Lo usamos en GrokService para llamar a la API de xAI (Grok).
 *   Al definirlo como @Bean, Spring lo gestiona como singleton y podemos
 *   inyectarlo por constructor en cualquier servicio.
 */
@Configuration
public class AppConfig {

    /**
     * Bean de RestTemplate para llamadas HTTP a APIs externas (Grok AI).
     * Singleton: una sola instancia compartida por toda la aplicación.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
