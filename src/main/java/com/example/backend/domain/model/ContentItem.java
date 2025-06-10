package com.example.backend.domain.model;

import java.time.Instant;
import java.util.Map;

public record ContentItem(
    String name,
    String path,
    String type,
    String mimeType,
    long size,
    Instant lastModified,
    Map<String, Object> metadata
) {
    public static ContentItem directory(String name, String path, Instant lastModified, Map<String, Object> metadata) {
        return new ContentItem(name, path, "directory", null, 0, lastModified, metadata);
    }

    public static ContentItem file(String name, String path, String mimeType, long size, Instant lastModified, Map<String, Object> metadata) {
        return new ContentItem(name, path, "file", mimeType, size, lastModified, metadata);
    }
}
