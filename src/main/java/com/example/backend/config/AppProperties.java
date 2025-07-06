package com.example.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the application.
 * <p>
 * This class maps properties from application.yml with the prefix 'app'.
 */
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    
    private Content content = new Content();
    private Cache cache = new Cache();

    public static class Content {
        private String rootDirectory = "./content";

        public String getRootDirectory() {
            return rootDirectory;
        }

        public void setRootDirectory(String rootDirectory) {
            this.rootDirectory = rootDirectory;
        }
    }

    public static class Cache {
        private long ttl = 3600; // 1 hour in seconds
        private int maxSize = 1000;

        public long getTtl() {
            return ttl;
        }

        public void setTtl(long ttl) {
            this.ttl = ttl;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }
    }

    public Content getContent() {
        return content;
    }

    public void setContent(Content content) {
        this.content = content;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }
}
