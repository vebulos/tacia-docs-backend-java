package com.example.backend.api.dto;

import com.example.backend.domain.model.ContentItem;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContentItemDto(
    String name,
    String path,
    String type,  // 'file' or 'directory'
    Long size,    // null for directories
    Instant lastModified,
    String content,  // Only populated when getting file content
    @JsonProperty("children") List<ContentItemDto> children  // Only for directories
) {
    public static ContentItemDto fromDomain(ContentItem item) {
        return new ContentItemDto(
            item.name(),
            item.path(),
            item.type(),
            item.type().equals("file") ? item.size() : null,
            item.lastModified(),
            null,  // Content is loaded separately
            null   // Children are loaded separately
        );
    }
    
    public static ContentItemDto withContent(ContentItem item, String content) {
        return new ContentItemDto(
            item.name(),
            item.path(),
            item.type(),
            item.size(),
            item.lastModified(),
            content,
            null
        );
    }
    
    public static ContentItemDto withChildren(ContentItem item, List<ContentItemDto> children) {
        return new ContentItemDto(
            item.name(),
            item.path(),
            item.type(),
            null,  // Size is not needed for directories with children
            item.lastModified(),
            null,
            children
        );
    }
}
