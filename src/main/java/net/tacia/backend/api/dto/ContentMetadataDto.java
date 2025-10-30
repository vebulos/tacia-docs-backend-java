package net.tacia.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ContentMetadataDto {
    private final Map<String, Object> properties = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return new HashMap<>(properties);
    }

    @JsonAnySetter
    public void setProperty(String name, Object value) {
        properties.put(name, value);
    }

    @JsonIgnore
    public String getTitle() {
        return (String) properties.getOrDefault("title", "");
    }

    @JsonIgnore
    @SuppressWarnings("unchecked")
    public List<String> getTags() {
        Object tags = properties.get("tags");
        if (tags instanceof List<?> list) {
            return (List<String>) list;
        } else if (tags instanceof String str) {
            return List.of(str.split("\\s*,\\s*"));
        }
        return List.of();
    }

    @JsonIgnore
    public Integer getOrder() {
        Object order = properties.get("order");
        if (order instanceof Integer intValue) {
            return intValue;
        } else if (order instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public static ContentMetadataDto empty() {
        return new ContentMetadataDto();
    }
}
