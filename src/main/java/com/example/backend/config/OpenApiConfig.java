package com.example.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAPI (Swagger) documentation.
 */
@Configuration
public class OpenApiConfig {

    @Value("${info.app.version:1.0.0}")
    private String appVersion;

    /**
     * Configures the main OpenAPI definition.
     *
     * @return Configured OpenAPI instance
     */
    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        
        return new OpenAPI()
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(
                new Components()
                    .addSecuritySchemes(securitySchemeName,
                        new SecurityScheme()
                            .name(securitySchemeName)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                    )
            )
            .info(new Info()
                .title("MXC Backend API")
                .description("API documentation for MXC Backend Service")
                .version(appVersion)
                .contact(new Contact()
                    .name("MXC Support")
                    .url("https://mxc.example.com/support")
                    .email("support@mxc.example.com")
                )
                .license(new License()
                    .name("MIT")
                    .url("https://opensource.org/licenses/MIT")
                )
            );
    }

    /**
     * Configures the public API group (endpoints that don't require authentication).
     *
     * @return Configured GroupedOpenApi instance
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
            .group("public")
            .pathsToMatch(
                "/api/health/**",
                "/actuator/health/**"
            )
            .build();
    }

    /**
     * Configures the protected API group (endpoints that require authentication).
     *
     * @return Configured GroupedOpenApi instance
     */
    @Bean
    public GroupedOpenApi protectedApi() {
        return GroupedOpenApi.builder()
            .group("protected")
            .pathsToMatch(
                "/api/content/**",
                "/api/structure/**",
                "/api/related/**",
                "/api/first-document/**"
            )
            .build();
    }
}
