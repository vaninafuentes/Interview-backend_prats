package com.example.interviewback.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Beans de infraestructura compartidos por la aplicación.
 */
@Configuration
public class AppConfig {

    /**
     * Cliente HTTP para llamar a la API de Groq desde {@code GroqService}.
     * Spring lo registra como un único bean reutilizable (alcance singleton).
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
