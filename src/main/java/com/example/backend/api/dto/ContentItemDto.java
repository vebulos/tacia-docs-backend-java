package com.example.backend.api.dto;

import com.example.backend.model.ContentItem;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Data Transfer Object for content items (files and directories)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContentItemDto(
    @JsonProperty("name") String name,                // Name of the file or directory
    @JsonProperty("path") String path,                // Relative path of the item from the current directory
    @JsonProperty("type") String type,                // 'file' or 'directory'
    @JsonIgnore Long size,                             // Size in bytes (exposed via getter)
    @JsonIgnore Instant lastModified,                  // Last modification timestamp (formatted via getter)
    @JsonIgnore String content,                        // File content (only for files, not in listings)
    @JsonIgnore List<ContentItemDto> children,         // Child items (only for directories)
    @JsonIgnore ContentMetadataDto metadata            // Metadata extracted from content (for markdown files)
) {
    
    @JsonProperty("isDirectory")
    public boolean isDirectory() {
        return "directory".equals(type);
    }
    
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
        return size != null ? size : 0L;
    }
    /**
     * Creates a DTO from a domain model with relative path
     */
    public static ContentItemDto fromDomain(ContentItem item, String basePath) {
        // Calculate relative path
        String relativePath = item.path();
        if (basePath != null && !basePath.isEmpty() && !"/".equals(basePath) 
                && relativePath.startsWith(basePath)) {
            relativePath = relativePath.substring(basePath.length());
            // Remove leading slash if present
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
        }
        
        // If it's a directory, ensure the path ends with a slash
        if ("directory".equals(item.type()) && !relativePath.endsWith("/") && !relativePath.isEmpty()) {
            relativePath = relativePath + "/";
        }
        
        return new ContentItemDto(
            item.name(),
            relativePath,
            item.type(),
            "file".equals(item.type()) ? item.size() : 0,
            item.lastModified(),
            null,  // Content is loaded separately
            null,  // Children are loaded separately
            null   // Metadata is extracted separately
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
            metadata
        );
    }
    
    /**
     * Creates a DTO with child items (for directories)
     */
    public static ContentItemDto withChildren(ContentItem item, List<ContentItemDto> children) {
        return new ContentItemDto(
            item.name(),
            item.name(),  // Just the name for directory listings
            item.type(),
            0L,  // Directories have size 0
            item.lastModified(),
            null,  // No content for directories
            children,
            null   // No metadata for directories
        );
    }
}
