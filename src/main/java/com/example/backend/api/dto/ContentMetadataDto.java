package com.example.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ContentMetadataDto(
    @JsonProperty("title") String title,
    @JsonProperty("tags") List<String> tags
) {
    public static ContentMetadataDto empty() {
        return new ContentMetadataDto("", List.of());
    }
}
