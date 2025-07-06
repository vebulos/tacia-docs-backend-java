package com.example.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

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
        "/**"  // Allow all endpoints for now
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
            // Disable CSRF
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure CORS to be handled by WebConfig
            .cors(cors -> {})
            
            // Configure session management to be stateless
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Configure security headers
            .headers(headers -> {
                // Disable cache control
                headers.cacheControl(HeadersConfigurer.CacheControlConfig::disable);
                // Disable content type options
                headers.contentTypeOptions(HeadersConfigurer.ContentTypeOptionsConfig::disable);
                // Disable HSTS
                headers.httpStrictTransportSecurity(HeadersConfigurer.HstsConfig::disable);
                // Disable frame options
                headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable);
                // Disable XSS protection
                headers.xssProtection(HeadersConfigurer.XXssConfig::disable);
                
                // Set a minimal CSP that allows everything (effectively disabling CSP)
                headers.contentSecurityPolicy(csp -> 
                    csp.policyDirectives("default-src * 'unsafe-inline' 'unsafe-eval'; script-src * 'unsafe-inline' 'unsafe-eval'; connect-src * 'unsafe-inline'; img-src * data: blob: 'unsafe-inline'; frame-src *; style-src * 'unsafe-inline';") 
                );
                
                // Set minimal referrer policy
                headers.referrerPolicy(referrer -> 
                    referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)
                );
            })
            
            // Configure request authorization
            .authorizeHttpRequests(auth -> {
                // All requests are permitted
                auth.anyRequest().permitAll();
            });
            
            // TODO: Uncomment and configure JWT authentication filter when ready
            // .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
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
