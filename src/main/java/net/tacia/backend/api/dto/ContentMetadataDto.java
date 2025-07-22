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
        if (tags instanceof List) {
            return (List<String>) tags;
        } else if (tags instanceof String) {
            return List.of(((String) tags).split("\\s*,\\s*"));
        }
        return List.of();
    }

    @JsonIgnore
    public Integer getOrder() {
        Object order = properties.get("order");
        if (order instanceof Integer) {
            return (Integer) order;
        } else if (order instanceof String) {
            try {
                return Integer.parseInt((String) order);
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
