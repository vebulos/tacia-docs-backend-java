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
    @JsonProperty("path") String path,                // Full path of the item from content root
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
    
    @JsonProperty("title")
    public String getTitle() {
        // For directories, return null to match JS backend
        if (isDirectory()) {
            return null;
        }
        
        // If we have metadata with a title, use that first
        if (metadata != null && metadata.title() != null && !metadata.title().isEmpty()) {
            return metadata.title();
        }
        
        // For Markdown files, try to get title from content
        if (type != null && type.equals("file") && content != null) {
            if (content.startsWith("# ")) {
                // Extract first line after # for title
                int endOfFirstLine = content.indexOf("\n");
                if (endOfFirstLine > 2) {
                    return content.substring(2, endOfFirstLine).trim();
                }
            }
            // Fallback to filename without extension for Markdown files
            if (name.endsWith(".md")) {
                return name.substring(0, name.length() - 3);
            }
        }
        
        // For non-markdown files, return null to match JS backend
        return null;
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
     * Creates a DTO from a domain model with full path
     */
    public static ContentItemDto fromDomain(ContentItem item, String basePath) {
        // Use full path for the item
        String fullPath = item.path();
        
        // Remove leading slash if present (to match JS backend)
        if (fullPath.startsWith("/")) {
            fullPath = fullPath.substring(1);
        }
        
        // For directories, ensure the path does NOT end with a slash to match JS backend
        if (fullPath.endsWith("/")) {
            fullPath = fullPath.substring(0, fullPath.length() - 1);
        }
        
        // For markdown files, set the title from the filename as fallback
        ContentMetadataDto metadata = null;
        if ("file".equals(item.type()) && item.name().toLowerCase().endsWith(".md")) {
            String title = item.name().substring(0, item.name().length() - 3); // Remove .md
            metadata = new ContentMetadataDto(title, List.of());
        }
        
        return new ContentItemDto(
            item.name(),
            fullPath,
            item.type(),
            "file".equals(item.type()) ? item.size() : 0,
            item.lastModified(),
            null,  // Content is loaded separately
            null,  // Children are loaded separately
            metadata
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
