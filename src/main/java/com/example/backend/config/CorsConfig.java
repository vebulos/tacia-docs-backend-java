package com.example.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // Alle Endpoints erlauben
                .allowedOrigins("http://localhost:4200") // Nur Angular erlauben
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Wichtig: OPTIONS für Preflight!
                .allowedHeaders("*") // Alle Header erlauben
                .exposedHeaders("Content-Length", "Content-Type", "Cache-Control", "Expires")
                .allowCredentials(true); // Falls Cookies/Auth-Header verwendet werden
    }
}