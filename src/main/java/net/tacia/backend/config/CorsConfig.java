package net.tacia.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for CORS (Cross-Origin Resource Sharing)
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // Allow all endpoints
                .allowedOrigins("http://localhost:4200") // Only allow Angular frontend
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Important: OPTIONS for Preflight requests
                .allowedHeaders("*") // Allow all headers
                .exposedHeaders("Content-Length", "Content-Type", "Cache-Control", "Expires")
                .allowCredentials(true); // Allow cookies/auth headers if used
    }
}