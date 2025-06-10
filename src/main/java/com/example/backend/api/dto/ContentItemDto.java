package com.example.backend.api.dto;

import com.example.backend.domain.model.ContentItem;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContentItemDto(
    String name,
    String path,
    String type,
    String mimeType,
    Long size,
    Instant lastModified,
    Map<String, Object> metadata,
    String content
) {
    public static ContentItemDto fromDomain(ContentItem item) {
        return new ContentItemDto(
            item.name(),
            item.path(),
            item.type(),
            item.mimeType(),
            item.size(),
            item.lastModified(),
            item.metadata(),
            null // Content is loaded separately
        );
    }
    
    public static ContentItemDto withContent(ContentItem item, String content) {
        return new ContentItemDto(
            item.name(),
            item.path(),
            item.type(),
            item.mimeType(),
            item.size(),
            item.lastModified(),
            item.metadata(),
            content
        );
    }
}
