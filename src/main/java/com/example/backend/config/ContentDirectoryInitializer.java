package com.example.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class ContentDirectoryInitializer {
    private static final Logger log = LoggerFactory.getLogger(ContentDirectoryInitializer.class);

    @Value("${app.content.root-directory:./DATA/content}")
    private String contentRoot;

    @Bean
    public CommandLineRunner initContentDirectory() {
        return args -> {
            Path contentPath = Paths.get(contentRoot).toAbsolutePath().normalize();
            // Ensure parent directory (DATA) exists
            Path parentDir = contentPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                log.info("Creating parent directory: {}", parentDir);
                Files.createDirectories(parentDir);
            }
            // Ensure content directory exists
            if (!Files.exists(contentPath)) {
                log.info("Creating content directory: {}", contentPath);
                Files.createDirectories(contentPath);
            }
            log.info("Using content directory: {}", contentPath);
            log.info("Content directory exists: {}", Files.exists(contentPath));
            log.info("Content directory is writable: {}", Files.isWritable(contentPath));
        };
    }
}
