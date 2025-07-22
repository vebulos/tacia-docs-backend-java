package net.tacia.backend.service;

import net.tacia.backend.api.dto.ContentMetadataDto;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for processing Markdown content and extracting metadata
 */
@Service
public class MarkdownService {

    private static final Pattern FRONT_MATTER_PATTERN = Pattern.compile(
        "^---\\s*\\n(?<frontmatter>.*?)\\n---\\s*\\n(?<content>.*)$", 
        Pattern.DOTALL
    );
    
    private static final Pattern FRONT_MATTER_LINE_PATTERN = Pattern.compile("^([a-zA-Z0-9_-]+):\\s*(.*)$", Pattern.MULTILINE);
    private static final Pattern HEADING_PATTERN = Pattern.compile("^#+\\s+(.+)$", Pattern.MULTILINE);

    /**
     * Process markdown content and extract metadata and content
     */
    public Map<String, Object> processMarkdown(String markdown) {
        Map<String, Object> result = new HashMap<>();
        ContentMetadataDto metadata = new ContentMetadataDto();
        String content = markdown;
        List<String> headings = new ArrayList<>();
        
        // Extract front matter if present
        Matcher frontMatterMatcher = FRONT_MATTER_PATTERN.matcher(markdown);
        if (frontMatterMatcher.find()) {
            String frontMatter = frontMatterMatcher.group("frontmatter");
            content = frontMatterMatcher.group("content").trim();
            
            // Parse all front matter properties
            String[] lines = frontMatter.split("\\r?\\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // Skip empty lines and comments
                }
                
                Matcher matcher = FRONT_MATTER_LINE_PATTERN.matcher(line);
                if (matcher.find()) {
                    String key = matcher.group(1).trim();
                    String value = matcher.group(2).trim();
                    
                    // Remove surrounding quotes if present
                    if ((value.startsWith("\"") && value.endsWith("\"")) || 
                        (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    
                    // Try to parse values appropriately
                    Object parsedValue = value;
                    if (value.equalsIgnoreCase("true")) {
                        parsedValue = true;
                    } else if (value.equalsIgnoreCase("false")) {
                        parsedValue = false;
                    } else if (value.equalsIgnoreCase("null") || value.isEmpty()) {
                        parsedValue = null;
                    } else if (value.matches("^-?\\d+$")) {
                        try {
                            parsedValue = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            // Keep as string if parsing fails
                        }
                    } else if (value.matches("^-?\\d+\\.\\d+$")) {
                        try {
                            parsedValue = Double.parseDouble(value);
                        } catch (NumberFormatException e) {
                            // Keep as string if parsing fails
                        }
                    } else if (value.startsWith("[") && value.endsWith("]")) {
                        // Simple array parsing
                        String arrayContent = value.substring(1, value.length() - 1);
                        parsedValue = Arrays.stream(arrayContent.split(","))
                            .map(String::trim)
                            .map(s -> s.replaceAll("^['\"]|['\"]$", ""))
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());
                    }
                    
                    metadata.setProperty(key, parsedValue);
                }
            }
            
            // Special handling for tags if it's a string
            Object tags = metadata.getProperties().get("tags");
            if (tags instanceof String) {
                String tagsStr = ((String) tags).trim();
                if (tagsStr.startsWith("[") && tagsStr.endsWith("]")) {
                    tagsStr = tagsStr.substring(1, tagsStr.length() - 1);
                }
                List<String> tagList = Arrays.stream(tagsStr.split(","))
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty())
                    .collect(Collectors.toList());
                metadata.setProperty("tags", tagList);
            }
        }
        
        // Extract headings from content
        Matcher headingMatcher = HEADING_PATTERN.matcher(content);
        while (headingMatcher.find()) {
            String heading = headingMatcher.group(1).trim();
            // Remove markdown links from headings
            heading = heading.replaceAll("\\[(.*?)\\]\\(.*?\\)", "$1");
            headings.add(heading);
        }
        
        // If no title found in front matter, use first heading
        if ((metadata.getTitle() == null || metadata.getTitle().isEmpty()) && !headings.isEmpty()) {
            metadata.setProperty("title", headings.get(0));
        }
        
        // Build the result
        result.put("markdown", content);
        result.put("metadata", metadata);
        result.put("headings", headings);
        
        return result;
    }
}
