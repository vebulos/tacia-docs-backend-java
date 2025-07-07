package net.tacia.backend.config;

import net.tacia.backend.repository.ContentRepository;
import net.tacia.backend.repository.FileSystemContentRepository;
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
