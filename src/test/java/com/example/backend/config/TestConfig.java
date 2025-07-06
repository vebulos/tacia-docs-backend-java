package com.example.backend.config;

import com.example.backend.repository.ContentRepository;
import com.example.backend.repository.FileSystemContentRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.nio.file.Paths;

@TestConfiguration
public class TestConfig {
    
    @Bean
    @Primary
    public ContentRepository contentRepository() {
        return new FileSystemContentRepository(Paths.get("target/test-content"));
    }
}
