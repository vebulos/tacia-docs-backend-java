package net.tacia.backend.service;

import net.tacia.backend.api.dto.ContentMetadataDto;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for processing Markdown content and extracting metadata
 */
@Service
public class MarkdownService {

    private static final Pattern FRONT_MATTER_PATTERN = Pattern.compile(
        "^---\\s*\\n(?<frontmatter>.*?)\\n---\\s*\\n(?<content>.*)$", 
        Pattern.DOTALL
    );
    
    private static final Pattern TITLE_PATTERN = Pattern.compile("^title:\\s*['\"](.*?)['\"]\\s*$", Pattern.MULTILINE);
    private static final Pattern TAGS_PATTERN = Pattern.compile("^tags:\\s*\\[(.*?)\\]$", Pattern.MULTILINE);
    private static final Pattern HEADING_PATTERN = Pattern.compile("^#+\\s+(.+)$", Pattern.MULTILINE);

    /**
     * Process markdown content and extract metadata and content
     */
    public Map<String, Object> processMarkdown(String markdown) {
        Map<String, Object> result = new HashMap<>();
        
        // Default values
        String content = markdown;
        String title = "";
        List<String> tags = new ArrayList<>();
        List<String> headings = new ArrayList<>();
        
        // Extract front matter if present
        Matcher frontMatterMatcher = FRONT_MATTER_PATTERN.matcher(markdown);
        if (frontMatterMatcher.find()) {
            String frontMatter = frontMatterMatcher.group("frontmatter");
            content = frontMatterMatcher.group("content").trim();
            
            // Extract title
            Matcher titleMatcher = TITLE_PATTERN.matcher(frontMatter);
            if (titleMatcher.find()) {
                title = titleMatcher.group(1).trim();
            }
            
            // Extract tags
            Matcher tagsMatcher = TAGS_PATTERN.matcher(frontMatter);
            if (tagsMatcher.find()) {
                String[] tagArray = tagsMatcher.group(1).split(",");
                for (String tag : tagArray) {
                    String cleanedTag = tag.trim().replaceAll("['\"]", "");
                    if (!cleanedTag.isEmpty()) {
                        tags.add(cleanedTag);
                    }
                }
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
        if (title.isEmpty() && !headings.isEmpty()) {
            title = headings.get(0);
        }
        
        // Build the result
        result.put("markdown", content);
        result.put("metadata", new ContentMetadataDto(title, tags));
        result.put("headings", headings);
        
        return result;
    }
}
