package com.example.backend;

import com.example.backend.repository.ContentRepository;
import com.example.backend.repository.FileSystemContentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.nio.file.Paths;

@Configuration
public class TestConfig {
    
    @Bean
    @Primary
    public ContentRepository contentRepository() {
        // Use a test-specific directory for content
        return new FileSystemContentRepository(Paths.get("target/test-content"));
    }
}
