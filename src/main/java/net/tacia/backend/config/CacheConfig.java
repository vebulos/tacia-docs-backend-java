package net.tacia.backend.config;

import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Configuration class for cache management.
 * <p>
 * This class configures the caching mechanism used throughout the application.
 * It sets up a simple in-memory cache with default settings that can be
 * customized via application properties.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String RELATED_DOCUMENTS_CACHE = "relatedDocuments";
    public static final String DOCUMENT_STRUCTURE_CACHE = "documentStructure";
    public static final String CONTENT_CACHE = "content";

    /**
     * Configures the cache manager with default settings.
     *
     * @return CacheManagerCustomizer that applies the configuration
     */
    @Bean
    public CacheManagerCustomizer<ConcurrentMapCacheManager> cacheManagerCustomizer() {
        return cacheManager -> {
            cacheManager.setCacheNames(Arrays.asList(
                RELATED_DOCUMENTS_CACHE,
                DOCUMENT_STRUCTURE_CACHE,
                CONTENT_CACHE
            ));
            cacheManager.setAllowNullValues(false);
        };
    }

    /**
     * Configures the cache manager with TTL (Time To Live) settings.
     * This is a placeholder - in a production environment, consider using
     * a more sophisticated cache provider like Caffeine or Redis.
     *
     * @return Configured CacheManager
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            RELATED_DOCUMENTS_CACHE,
            DOCUMENT_STRUCTURE_CACHE,
            CONTENT_CACHE
        );
    }
}
