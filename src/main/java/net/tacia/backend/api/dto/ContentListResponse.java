package net.tacia.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
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
        
        // Remove leading slash if present (to match JS backend)
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        
        // Remove trailing slash unless it's the root
        if (normalizedPath.endsWith("/") && normalizedPath.length() > 1) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        } else if (normalizedPath.isEmpty()) {
            normalizedPath = "";
        }
        
        // Sort items: directories first, then files, both alphabetically
        List<ContentItemDto> sortedItems = new ArrayList<>(items);
        sortedItems.sort((a, b) -> {
            // Directories first
            if (a.isDirectory() && !b.isDirectory()) {
                return -1;
            } else if (!a.isDirectory() && b.isDirectory()) {
                return 1;
            }
            // Then sort alphabetically by name (case insensitive)
            return a.name().compareToIgnoreCase(b.name());
        });
        
        return new ContentListResponse(sortedItems.size(), sortedItems, normalizedPath);
    }
}
