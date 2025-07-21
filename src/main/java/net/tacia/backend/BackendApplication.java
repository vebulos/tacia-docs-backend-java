package net.tacia.backend;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import net.tacia.backend.config.ContentDirProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class for the MXC Backend service.
 * <p>
 * This class serves as the entry point for the Spring Boot application.
 * It enables various Spring Boot features like caching, async processing,
 * and component scanning.
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@ConfigurationPropertiesScan("net.tacia.backend.config")
@OpenAPIDefinition(
    info = @Info(
        title = "MXC Backend API",
        version = "1.0",
        description = "Documentation for MXC Backend API"
    )
)
public class BackendApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(BackendApplication.class);
    
    public static void main(String[] args) {
        try {
            // Check if contentDir parameter is provided
            String contentDir = null;

            if(args.length == 3 && "=".equals(args[1]) && "--contentDir".equals(args[0].trim())) {
                // case 1, space separated: --contentDir = ../DATA/content
                contentDir = args[2].trim();
            } else if(args.length == 1 && args[0].startsWith("--contentDir=")) {
                // case 2, monolith: contentDir=../DATA/content
                contentDir = args[0].substring("--contentDir=".length()).trim();
            } else if(args.length == 2 && "--contentDir".equals(args[0])) {
                // case 3, without equal sign: --contentDir ../DATA/content
                contentDir = args[1].trim();
            }

            if (contentDir == null || contentDir.trim().isEmpty()) {
                contentDir = System.getenv("CONTENT_ROOT");
                if (contentDir == null || contentDir.trim().isEmpty()) {
                    System.err.println("Error: Content directory not specified");
                    System.err.println("Usage: java -jar app.jar --contentDir=/path/to/content");
                    System.err.println("Environment variable CONTENT_ROOT can also be used");
                    System.exit(1);
                }
            }

            ContentDirProvider.setContentDir(contentDir);
            logger.info("Using content directory: {}", contentDir);

            logger.info("Starting MXC Backend Application...");
            SpringApplication.run(BackendApplication.class, args);
            logger.info("MXC Backend Application started successfully!");

        } catch (Exception e) {
            logger.error("Failed to start MXC Backend Application", e);
            System.exit(1);
        }
    }
}
