package com.example.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;


/**
 * Security configuration for the application.
 * <p>
 * This class configures security settings including:
 * - CSRF protection
 * - CORS configuration
 * - Session management
 * - Request authorization rules
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Public endpoints that don't require authentication
    private static final String[] PUBLIC_ENDPOINTS = {
        "/api/health/**",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/actuator/health/**",
        "/actuator/info"
    };

    /**
     * Configures security filters and rules.
     *
     * @param http HttpSecurity to configure
     * @return Configured SecurityFilterChain
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF as we're using token-based authentication
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure CORS
            .cors(cors -> cors.configure(http))
            
            // Configure session management to be stateless
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Configure request authorization
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    "/api/health/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/actuator/health/**",
                    "/actuator/info"
                ).permitAll()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            // TODO: Uncomment and configure JWT authentication filter when ready
            // .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Configure exception handling
            .exceptionHandling(exception -> 
                exception
                    // Handle access denied
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        response.sendError(403, "Access Denied: " + accessDeniedException.getMessage());
                    })
            );

        return http.build();
    }

    // TODO: Uncomment and configure CORS when needed
    /*
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    */
}
