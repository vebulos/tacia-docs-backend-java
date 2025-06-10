package com.example.backend.domain.model;

import java.time.Instant;

public record ContentItem(
    String name,
    String type,  // 'file' or 'directory'
    String path,  // Relative path from content root
    long size,    // Size in bytes (0 for directories)
    Instant lastModified
) {
    public static ContentItem directory(String name, String path, Instant lastModified) {
        return new ContentItem(
            name, 
            "directory", 
            ensurePathEndsWithSlash(path), 
            0, 
            lastModified
        );
    }

    public static ContentItem file(String name, String path, long size, Instant lastModified) {
        return new ContentItem(
            name, 
            "file", 
            path, 
            size, 
            lastModified
        );
    }

    private static String ensurePathEndsWithSlash(String path) {
        return path.endsWith("/") ? path : path + "/";
    }
}
