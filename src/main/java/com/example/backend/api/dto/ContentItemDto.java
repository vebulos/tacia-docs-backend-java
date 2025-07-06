package com.example.backend.api.dto;

import com.example.backend.model.ContentItem;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

/**
 * Data Transfer Object for content items (files and directories)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContentItemDto(
    String name,                // Name of the file or directory
    String path,                // Full path of the item
    String type,                // 'file' or 'directory'
    Long size,                  // Size in bytes (null for directories)
    Instant lastModified,       // Last modification timestamp
    String content,             // File content (only for files)
    @JsonProperty("children") List<ContentItemDto> children,  // Child items (only for directories)
    ContentMetadataDto metadata // Metadata extracted from content (for markdown files)
) {
    /**
     * Creates a basic DTO from a domain model without content or children
     */
    public static ContentItemDto fromDomain(ContentItem item) {
        return new ContentItemDto(
            item.name(),
            item.path(),
            item.type(),
            "file".equals(item.type()) ? item.size() : null,
            item.lastModified(),
            null,  // Content is loaded separately
            null,  // Children are loaded separately
            null   // Metadata is extracted separately
        );
    }
    
    /**
     * Creates a DTO with file content and metadata
     */
    public static ContentItemDto withContent(ContentItem item, String content, ContentMetadataDto metadata) {
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
            item.path(),
            item.type(),
            null,  // Size is not needed for directories with children
            item.lastModified(),
            null,  // No content for directories
            children,
            null   // No metadata for directories
        );
    }
}
