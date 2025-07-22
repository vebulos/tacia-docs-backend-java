package net.tacia.backend.api.controller;

import net.tacia.backend.api.dto.ContentItemDto;
import net.tacia.backend.api.dto.ContentListResponse;
import net.tacia.backend.api.dto.ContentMetadataDto;
import net.tacia.backend.api.exception.ContentNotFoundException;
import net.tacia.backend.model.ContentItem;
import net.tacia.backend.repository.ContentRepository;
import net.tacia.backend.service.MarkdownService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for managing content (files and directories)
 */
@RestController
@RequestMapping("/api")
public class ContentController {

    private static final Logger logger = LoggerFactory.getLogger(ContentController.class);
    private final ContentRepository contentRepository;
    private final MarkdownService markdownService;

    public ContentController(ContentRepository contentRepository, MarkdownService markdownService) {
        this.contentRepository = contentRepository;
        this.markdownService = markdownService;
    }

    /**
     * Get raw file content at the specified path
     */
    @GetMapping("/file-content/{*path}")
    public ResponseEntity<String> getFileContent(@PathVariable String path) {
        String normalizedPath = normalizePath(path);
        logger.debug("Getting raw content for path: {}", normalizedPath);
        
        // This method handles raw file content retrieval
        ContentItem item = contentRepository.findByPath(normalizedPath)
            .orElseThrow(() -> new ContentNotFoundException("Content not found: " + normalizedPath));
            
        if ("directory".equals(item.type())) {
            throw new ContentNotFoundException("Cannot get content of a directory: " + normalizedPath);
        }
        
        // Get the file content using the repository
        String content = contentRepository.getContent(normalizedPath)
            .orElseThrow(() -> new ContentNotFoundException("Content not found for path: " + normalizedPath));
        
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(content);
    }

    /**
     * Get content at the specified path
     */
    /**
     * Extracts and decodes the path from the request URL
     * Handles URL-encoded characters like %20 for spaces
     */
    private String extractPathFromRequest() {
        try {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes) {
                HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
                String requestURI = request.getRequestURI();
                String contextPath = request.getContextPath();
                
                // Remove context path if present
                if (contextPath != null && !contextPath.isEmpty() && requestURI.startsWith(contextPath)) {
                    requestURI = requestURI.substring(contextPath.length());
                }
                
                // Find the /content/ part
                int contentIndex = requestURI.indexOf("/content/");
                if (contentIndex != -1) {
                    String rawPath = requestURI.substring(contentIndex + 9); // 9 is the length of "/content/"
                    // URL decode the path to handle spaces (%20) and other encoded characters
                    String decodedPath = URLDecoder.decode(rawPath, StandardCharsets.UTF_8);
                    logger.debug("Extracted path - raw: '{}', decoded: '{}'", rawPath, decodedPath);
                    
                    // Normalize and remove leading slash to match JS backend
                    String normalized = normalizePath(decodedPath);
                    if (normalized.startsWith("/")) {
                        normalized = normalized.substring(1);
                    }
                    return normalized;
                }
            }
            return "";
        } catch (Exception e) {
            logger.error("Error extracting path from request", e);
            return "";
        }
    }

    @GetMapping("/content/**")
    public ResponseEntity<?> getContent(
            @RequestParam(required = false, defaultValue = "false") boolean recursive) {
        
        String path = extractPathFromRequest();
        String normalizedPath = normalizePath(path);
        logger.debug("Requested path: '{}', normalized: '{}'", path, normalizedPath);

        // If the path is empty, list the root directory
        if (normalizedPath.isEmpty()) {
            // Get root item
            ContentItem rootItem = contentRepository.findByPath("").orElseThrow(
                () -> new ContentNotFoundException("Root directory not found")
            );
            
            // Get children of root
            List<ContentItem> children = contentRepository.findChildren("");
            
            // Convert to DTOs
            List<ContentItemDto> childDtos = children.stream()
                .map(child -> ContentItemDto.fromDomain(child, ""))
                .collect(Collectors.toList());
                
            // Create response with the requested path (which is already normalized)
            ContentListResponse response = ContentListResponse.of(childDtos, "");
            logger.debug("Returning root directory listing ({} items)", childDtos.size());
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_ENCODING, StandardCharsets.UTF_8.name())
                .body(response);
        }

        // For root path, ensure we don't have a leading slash
        String lookupPath = normalizedPath.isEmpty() ? "" : normalizedPath;
        
        ContentItem item = contentRepository.findByPath(lookupPath)
            .orElseThrow(() -> new ContentNotFoundException("Content not found: " + normalizedPath));
        
        // Handle regular files
        if ("file".equals(item.type())) {
            // For non-markdown files, return the raw content
            if (!item.name().toLowerCase().endsWith(".md")) {
                Optional<String> contentOpt = contentRepository.getContent(lookupPath);
                if (contentOpt.isEmpty()) {
                    throw new ContentNotFoundException("Content not found: " + lookupPath);
                }
                return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .header(HttpHeaders.CONTENT_ENCODING, StandardCharsets.UTF_8.name())
                    .body(contentOpt.get());
            } else {
                // For markdown files, process the content
                Optional<String> contentOpt = contentRepository.getContent(lookupPath);
                if (contentOpt.isEmpty()) {
                    throw new ContentNotFoundException("Content not found: " + lookupPath);
                }
                String content = contentOpt.get();
                
                // Process markdown to HTML
                Map<String, Object> processed = markdownService.processMarkdown(content);
                
                // Get title from metadata if available
                String title = null;
                if (processed.containsKey("metadata")) {
                    ContentMetadataDto metadata = (ContentMetadataDto) processed.get("metadata");
                    Map<String, String> metadataMap = new HashMap<>();
                    metadataMap.put("title", metadata.getTitle() != null ? metadata.getTitle() : "");
                    title = metadataMap.get("title");
                }
                
                // If no title in metadata, try to extract from content
                if ((title == null || title.isEmpty()) && content.startsWith("# ")) {
                    int endOfFirstLine = content.indexOf("\n");
                    if (endOfFirstLine > 2) {
                        title = content.substring(2, endOfFirstLine).trim();
                    }
                }
                
                // Fallback to filename without extension
                if (title == null || title.isEmpty()) {
                    title = item.name().replace(".md", "");
                }
                
                // Ensure we have a non-null title
                if (title == null) {
                    title = "";
                }
                
                // Create response with content
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("name", item.name().replace(".md", ""));
                response.put("path", item.path());
                
                // Add markdown content and metadata
                response.put("markdown", processed.get("markdown"));
                response.put("headings", processed.get("headings"));
                
                // Add metadata
                if (processed.containsKey("metadata")) {
                    ContentMetadataDto metadata = (ContentMetadataDto) processed.get("metadata");
                    response.put("metadata", metadata.getProperties());
                }
                
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_ENCODING, StandardCharsets.UTF_8.name())
                    .body(response);
            }
        }
        
        // Handle directories
        List<ContentItem> children = recursive ? 
            contentRepository.findDescendants(lookupPath) :
            contentRepository.findChildren(lookupPath);
            
        // Convert to DTOs with full paths (relative to content root)
        List<ContentItemDto> childDtos = new ArrayList<>();
        for (ContentItem child : children) {
            // Skip the current directory itself in recursive mode
            if (recursive && child.path().equals("/" + normalizedPath)) {
                continue;
            }
            childDtos.add(ContentItemDto.fromDomain(child, ""));
        }
            
        // Create response with the requested path (which is already normalized)
        ContentListResponse response = ContentListResponse.of(childDtos, normalizedPath);
        logger.debug("Returning directory listing for: {} ({} items)", normalizedPath, childDtos.size());
        
        // Set content type with UTF-8 encoding
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.CONTENT_ENCODING, "UTF-8")
            .body(response);
    }

    /**
     * Save content to the specified path
     */
    @PostMapping("/content/**")
    public ResponseEntity<ContentItemDto> saveContent(@RequestBody String content) {
        String path = extractPathFromRequest();
        String normalizedPath = normalizePath(path);
        
        logger.debug("Saving content to: {}", normalizedPath);

        // Create or update the file
        ContentItem savedItem;
        try {
            // First check if the parent directory exists
            String parentPath = getParentPath(normalizedPath);
            if (!parentPath.isEmpty() && !"/".equals(parentPath) && 
                !contentRepository.findByPath(parentPath).isPresent()) {
                logger.debug("Creating parent directory: {}", parentPath);
                // Create parent directories if they don't exist
                contentRepository.saveContent(parentPath, "");
            }
            
            // Now save the file
            logger.debug("Saving content to: {}", normalizedPath);
            savedItem = contentRepository.saveContent(normalizedPath, content);
            logger.info("Successfully saved content to: {}", normalizedPath);
            
            ContentItemDto dto = ContentItemDto.withContent(savedItem, content, null);
            return ResponseEntity.ok(dto);
            
        } catch (Exception e) {
            logger.error("Failed to save content to {}: {}", normalizedPath, e.getMessage(), e);
            throw new RuntimeException("Failed to save content: " + e.getMessage(), e);
        }
    }

    /**
     * Delete content at the specified path
     */
    @DeleteMapping("/content/**")
    public ResponseEntity<Void> deleteContent() {
        String path = extractPathFromRequest();
        String normalizedPath = normalizePath(path);

        if (!contentRepository.exists(normalizedPath)) {
            logger.warn("Content not found for deletion: {}", normalizedPath);
            throw new ContentNotFoundException(normalizedPath);
        }
        
        try {
            contentRepository.delete(normalizedPath);
            logger.info("Successfully deleted content at: {}", normalizedPath);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error deleting content at {}: {}", normalizedPath, e.getMessage(), e);
            throw new RuntimeException("Failed to delete content: " + normalizedPath, e);
        }
    }

    /**
     * Recursively gets all children of a directory
     * 
     * @param path The directory path
     * @return List of content item DTOs with all descendants
     */
    private List<ContentItemDto> getChildrenRecursively(String path) {
        logger.debug("Getting children for path: {}", path);
        // Ensure the path is normalized before querying children
        String normalizedPath = normalizePath(path);
        logger.debug("Normalized path for children query: {}", normalizedPath);
        
        List<ContentItem> children = contentRepository.findChildren(normalizedPath);
        logger.debug("Found {} children for path: {}", children.size(), normalizedPath);
        
        return children.stream()
            .map(item -> {
                logger.debug("Processing item: {} (type: {})", item.path(), item.type());
                if ("directory".equals(item.type())) {
                    // Use the item's path directly since it should already be normalized
                    String childPath = normalizePath(item.path());
                    logger.debug("Getting nested children for directory: {}", childPath);
                    List<ContentItemDto> nestedChildren = getChildrenRecursively(childPath);
                    logger.debug("Found {} nested children in {}", nestedChildren.size(), childPath);
                    return ContentItemDto.withChildren(item, nestedChildren);
                }
                // For files, just return the basic info without content
                return ContentItemDto.fromDomain(item);
            })
            .collect(Collectors.toList());
    }

    /**
     * Normalizes a path to a consistent format
     * - Removes duplicate slashes
     * - Removes leading and trailing slashes
     * - Handles null or empty paths
     * - Matches the behavior of the JS backend
     * 
     * @param path The path to normalize
     * @return The normalized path without leading/trailing slashes
     */
    private String normalizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "";
        }
        
        // Trim and normalize slashes
        String normalized = path.trim()
            .replaceAll("/+", "/")  // Replace multiple slashes with a single slash
            .replaceAll("^/|/$", ""); // Remove leading and trailing slashes
        
        return normalized;
    }

    /**
     * Gets the parent path of the given path
     * 
     * @param path The full path
     * @return The parent path or empty string if no parent
     */
    private String getParentPath(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return "";
        }
        
        // Remove trailing slash if present
        String normalizedPath = path.endsWith("/") ? 
            path.substring(0, path.length() - 1) : path;
            
        int lastSlash = normalizedPath.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "/";
        }
        
        return normalizedPath.substring(0, lastSlash);
    }
    
    /**
     * Extracts the file name from a path.
     * 
     * @param path The full path
     * @return The file name or empty string for root
     */
    private String getFileName(String path) {
        if (path == null || path.equals("/")) {
            return "";
        }
        int lastSlash = path.lastIndexOf('/');
        return lastSlash == -1 ? path : path.substring(lastSlash + 1);
    }
}
