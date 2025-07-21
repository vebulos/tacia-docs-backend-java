package net.tacia.backend.config;

import net.tacia.backend.repository.ContentRepository;
import net.tacia.backend.repository.FileSystemContentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class ContentRepositoryConfig {
    
    @Bean
    public ContentRepository contentRepository() {
        String contentDir = ContentDirProvider.contentDir();
        if (contentDir == null) {
            throw new IllegalStateException("Content directory not set. Please specify --contentDir parameter or set CONTENT_ROOT environment variable.");
        }
        return new FileSystemContentRepository(Paths.get(contentDir));
    }
}
