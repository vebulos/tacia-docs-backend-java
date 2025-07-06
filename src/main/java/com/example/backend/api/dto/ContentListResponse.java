package com.example.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for directory listings
 */
public record ContentListResponse(
    @JsonProperty("count") int count,
    @JsonProperty("items") List<ContentItemDto> items,
    @JsonProperty("path") String path
) {
    public static ContentListResponse of(List<ContentItemDto> items, String path) {
        // Ensure path doesn't end with a slash unless it's the root
        String normalizedPath = path;
        if (normalizedPath.endsWith("/") && normalizedPath.length() > 1) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        } else if (normalizedPath.isEmpty()) {
            normalizedPath = "/";
        }
        
        return new ContentListResponse(items.size(), items, normalizedPath);
    }
}
