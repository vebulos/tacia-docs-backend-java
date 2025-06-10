package com.example.backend.config;

import com.example.backend.domain.repository.ContentRepository;
import com.example.backend.infrastructure.filesystem.FileSystemContentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class ContentConfig {

    @Bean
    public ContentRepository contentRepository(
            @Value("${app.content.root-directory:./DATA}") String contentRoot) {
        Path contentPath = Paths.get(contentRoot).toAbsolutePath().normalize();
        return new FileSystemContentRepository(contentPath);
    }
}
