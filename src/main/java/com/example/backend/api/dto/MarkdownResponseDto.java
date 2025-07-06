package com.example.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * DTO for Markdown file responses that matches the backend-js format
 */
public record MarkdownResponseDto(
    @JsonProperty("html") String html,
    @JsonProperty("metadata") Map<String, Object> metadata,
    @JsonProperty("name") String name,
    @JsonProperty("path") String path,
    @JsonProperty("headings") List<Object> headings
) {
    public MarkdownResponseDto {
        if (headings == null) {
            headings = List.of();
        }
    }
}
