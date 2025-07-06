package com.example.backend.api.dto;

import java.util.List;

public record ContentMetadataDto(
    String title,
    List<String> tags
) {
    public static ContentMetadataDto empty() {
        return new ContentMetadataDto("", List.of());
    }
}
