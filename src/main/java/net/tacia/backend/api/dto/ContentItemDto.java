package net.tacia.backend.api.dto;

import net.tacia.backend.model.ContentItem;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for content items (files and directories)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContentItemDto(
    @JsonProperty("name") String name,                // Name of the file or directory
    @JsonProperty("path") String path,                // Full path of the item from content root
    @JsonProperty("type") String type,                // 'file' or 'directory'
    @JsonIgnore long size,                             // Size in bytes (exposed via getter)
    @JsonIgnore Instant lastModified,                  // Last modification timestamp (formatted via getter)
    @JsonIgnore String content,                        // File content (only for files, not in listings)
    @JsonIgnore List<ContentItemDto> children,         // Child items (only for directories)
    @JsonProperty("metadata") Map<String, Object> metadata,  // Metadata from .metadata or frontmatter
    @JsonProperty("order") Integer order                    // Custom sort order
) {
    
    @JsonProperty("isDirectory")
    public boolean isDirectory() {
        return "directory".equals(type);
    }
    
    // Title field has been removed to match JS backend response format
    
    @JsonProperty("lastModified")
    public String getLastModifiedFormatted() {
        if (lastModified == null) return null;
        // Format as ISO-8601 with milliseconds and 'Z' timezone
        return DateTimeFormatter.ISO_INSTANT.format(
            lastModified.truncatedTo(ChronoUnit.MILLIS)
        );
    }
    
    @JsonProperty("size")
    public long getSizeOrZero() {
        return size;  // size is now a primitive long, no need to check for null
    }
    /**
     * Creates a DTO from a domain model with full path
     */
    public static ContentItemDto fromDomain(ContentItem item, String basePath) {
        // Create a copy of metadata to avoid modifying the original
        Map<String, Object> metadata = item.metadata() != null ? 
            new HashMap<>(item.metadata()) : new HashMap<>();
        
        // For markdown files, ensure title is set (from metadata or filename)
        if ("file".equals(item.type()) && item.name().toLowerCase().endsWith(".md")) {
            String title = (String) metadata.computeIfAbsent("title", 
                k -> item.name().substring(0, item.name().length() - 3));
            metadata.put("title", title);
        }
        
        // Build the full path
        String fullPath;
        if (basePath != null) {
            fullPath = (basePath.endsWith("/") ? basePath : basePath + "/") + item.name();
        } else {
            fullPath = item.path();
            
            // Remove leading/trailing slashes to match JS backend
            if (fullPath.startsWith("/")) {
                fullPath = fullPath.substring(1);
            }
            if (fullPath.endsWith("/")) {
                fullPath = fullPath.substring(0, fullPath.length() - 1);
            }
        }
            
        return new ContentItemDto(
            item.name(),
            fullPath,
            item.type(),
            item.size(),
            item.lastModified(),
            null,  // Content is loaded separately
            null,  // Children are loaded separately
            metadata,
            item.order()
        );
    }
    
    /**
     * Creates a basic DTO from a domain model without base path (uses item path as is)
     */
    public static ContentItemDto fromDomain(ContentItem item) {
        return fromDomain(item, null);
    }
    
    /**
     * Creates a DTO with file content and metadata
     */
    public static ContentItemDto withContent(ContentItem item, String content, ContentMetadataDto metadata) {
        // For file content, we need to keep the full path
        return new ContentItemDto(
            item.name(),
            item.path(),
            item.type(),
            item.size(),
            item.lastModified(),
            content,
            null,  // No children when loading content
            item.metadata() != null ? new HashMap<>(item.metadata()) : null,
            item.order()
        );
    }
    
    /**
     * Creates a DTO with child items (for directories)
     */
    public static ContentItemDto withChildren(ContentItem item, List<ContentItemDto> children) {
        // For directories, use the full path as the path, not just the name
        String path = item.path();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        
        return new ContentItemDto(
            item.name(),
            path,
            item.type(),
            0L,  // Directories have size 0
            item.lastModified(),
            null,  // No content for directories
            children,
            item.metadata() != null ? new HashMap<>(item.metadata()) : null,
            item.order()
        );
    }
}
