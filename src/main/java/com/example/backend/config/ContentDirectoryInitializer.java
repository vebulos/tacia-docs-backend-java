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

    @Value("${contentDir:./content}")
    private String contentRoot;

    @Bean
    public CommandLineRunner initContentDirectory() {
        return args -> {
            Path contentPath = Paths.get(contentRoot).toAbsolutePath().normalize();
            
            // Check if directory exists
            boolean exists = Files.exists(contentPath);
            boolean isDirectory = exists && Files.isDirectory(contentPath);
            boolean isWritable = exists && Files.isWritable(contentPath);
            
            log.info("Content directory: {}", contentPath);
            log.info("Content directory exists: {}", exists);
            
            if (exists) {
                log.info("Content directory is directory: {}", isDirectory);
                log.info("Content directory is writable: {}", isWritable);
                
                if (!isDirectory) {
                    log.error("Content path exists but is not a directory: {}", contentPath);
                }
                if (!isWritable) {
                    log.error("Content directory is not writable: {}", contentPath);
                }
            } else {
                log.error("Content directory does not exist: {}", contentPath);
                log.error("Please create the directory manually and ensure it has the correct permissions.");
            }
        };
    }
}
