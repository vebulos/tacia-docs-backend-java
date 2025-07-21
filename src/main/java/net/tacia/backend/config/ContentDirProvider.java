package net.tacia.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ContentDirProvider {
    private static String contentDir;

    public static void setContentDir(String dir) {
        contentDir = dir;
    }

    @Bean
    public static String contentDir() {
        return contentDir;
    }
}
