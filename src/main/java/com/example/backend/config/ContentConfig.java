package com.example.backend.config;

import com.example.backend.repository.ContentRepository;
import com.example.backend.repository.FileSystemContentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class ContentConfig {

    @Bean
    @Primary
    public ContentRepository contentRepository(Path contentPath) {
        return new FileSystemContentRepository(contentPath);
    }

    @Bean
    public Path contentPath(@Value("${app.content.root-directory:./DATA/content}") String contentRoot) {
        return Paths.get(contentRoot).toAbsolutePath().normalize();
    }
}
