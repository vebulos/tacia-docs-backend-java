package net.tacia.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for directory structure
 * Matches the structure of the JS backend response
 */
public record StructureResponseDto(
    @JsonProperty("path") String path,
    @JsonProperty("items") List<ContentItemDto> items,
    @JsonProperty("count") int count
) {
    public static StructureResponseDto of(String path, List<ContentItemDto> items) {
        return new StructureResponseDto(path, items, items != null ? items.size() : 0);
    }
}
