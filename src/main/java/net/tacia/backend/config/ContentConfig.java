package net.tacia.backend.config;

import net.tacia.backend.repository.ContentRepository;
import net.tacia.backend.repository.FileSystemContentRepository;
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
    public Path contentPath(@Value("${contentDir:./content}") String contentRoot) {
        return Paths.get(contentRoot).toAbsolutePath().normalize();
    }
}
