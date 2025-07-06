package com.example.backend.api.controller;

import com.example.backend.api.dto.ContentItemDto;
import com.example.backend.api.dto.ContentListResponse;
import com.example.backend.api.dto.ContentMetadataDto;
import com.example.backend.api.exception.ContentNotFoundException;
import com.example.backend.model.ContentItem;
import com.example.backend.repository.ContentRepository;
import com.example.backend.service.MarkdownService;
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
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
     * List content at the specified path
     */
    @GetMapping("/content")
    public ResponseEntity<ContentListResponse> listContent(
            @RequestParam(required = false, defaultValue = "/") String directory) {
        
        String normalizedPath = normalizePath(directory);
        logger.debug("Listing content for directory: {}", normalizedPath);
        
        List<ContentItem> items = contentRepository.findChildren(normalizedPath);
        
        List<ContentItemDto> dtos = items.stream()
            .map(ContentItemDto::fromDomain)
            .collect(Collectors.toList());
        
        // Ensure the path ends with a slash for directories
        String responsePath = normalizedPath.equals("/") ? "/" : normalizedPath + "/";
        ContentListResponse response = ContentListResponse.of(dtos, responsePath);
        
        logger.debug("Returning {} items for path: {}", dtos.size(), responsePath);
        return ResponseEntity.ok(response);
    }

    /**
     * Get content at the specified path
     */
    /**
     * Extracts the path from the request URL
     */
    private String extractPathFromRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
            String requestURI = request.getRequestURI();
            
            // Remove context path if present
            String contextPath = request.getContextPath();
            if (contextPath != null && !contextPath.isEmpty() && requestURI.startsWith(contextPath)) {
                requestURI = requestURI.substring(contextPath.length());
            }
            
            // Remove /api if present (since controller is mapped to /api)
            if (requestURI.startsWith("/api")) {
                requestURI = requestURI.substring(4); // Remove "/api"
            }
            
            // Extract path after /content/
            int contentIndex = requestURI.indexOf("/content/");
            if (contentIndex >= 0) {
                String path = requestURI.substring(contentIndex + 8); // +8 to skip "/content/"
                return path.isEmpty() ? "/" : path;
            }
        }
        return "/";
    }

    @GetMapping("/content/**")
    public ResponseEntity<?> getContent(
            @RequestParam(required = false, defaultValue = "false") boolean recursive) {
        
        String path = extractPathFromRequest();
        String normalizedPath = normalizePath(path);
        logger.debug("Requested path: '{}', normalized: '{}'", path, normalizedPath);

        ContentItem item = contentRepository.findByPath(normalizedPath)
            .orElseThrow(() -> new ContentNotFoundException("Content not found: " + normalizedPath));
        
        try {
            // Handle markdown files
            if (normalizedPath.toLowerCase().endsWith(".md")) {
                String content = contentRepository.getContent(normalizedPath);
                Map<String, Object> processed = markdownService.processMarkdown(content);
                
                // Extract file name without extension
                String fileName = item.name();
                if (fileName.endsWith(".md")) {
                    fileName = fileName.substring(0, fileName.length() - 3);
                }
                
                // Create response map to match the expected structure
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("headings", processed.get("headings"));
                response.put("markdown", processed.get("markdown"));
                
                // Add metadata with proper structure
                ContentMetadataDto metadata = (ContentMetadataDto) processed.get("metadata");
                Map<String, Object> metadataMap = new LinkedHashMap<>();
                metadataMap.put("title", metadata.title());
                metadataMap.put("tags", metadata.tags());
                response.put("metadata", metadataMap);
                
                // Add name and path
                response.put("name", fileName);
                response.put("path", normalizedPath);
                
                logger.debug("Returning markdown content for: {}", normalizedPath);
                return ResponseEntity.ok(response);
            }
            
            // Handle non-markdown files
            if ("file".equals(item.type())) {
                String content = contentRepository.getContent(normalizedPath);
                ContentItemDto dto = ContentItemDto.withContent(item, content, null);
                logger.debug("Returning file content for: {}", normalizedPath);
                return ResponseEntity.ok(dto);
            } 
            
            // Handle directories - always return a ContentListResponse for directories
            List<ContentItem> children = recursive ? 
                contentRepository.findDescendants(normalizedPath) : 
                contentRepository.findChildren(normalizedPath);
                
            // Convert to DTOs with the normalized path as base for relative paths
            List<ContentItemDto> childDtos = children.stream()
                .map(child -> ContentItemDto.fromDomain(child, normalizedPath))
                .collect(Collectors.toList());
            
            // Create the response with the directory listing
            ContentListResponse response = ContentListResponse.of(childDtos, normalizedPath);
            logger.debug("Returning directory listing for: {} ({} items)", normalizedPath, childDtos.size());
            
            // Set content type with UTF-8 encoding
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_ENCODING, "UTF-8")
                .body(response);
            
        } catch (IOException e) {
            logger.error("Error reading content for path {}: {}", normalizedPath, e.getMessage(), e);
            throw new RuntimeException("Failed to read content: " + e.getMessage(), e);
        }
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
     * - Ensures path starts with a single slash
     * - Removes trailing slashes (except for root)
     * - Handles null or empty paths
     * 
     * @param path The path to normalize
     * @return The normalized path
     */
    private String normalizePath(String path) {
        if (path == null || path.trim().isEmpty() || "/".equals(path.trim())) {
            return "/";
        }
        
        // Normalize the path
        String normalized = path.trim()
            .replaceAll("/+", "/")  // Replace multiple slashes with a single slash
            .replaceAll("^/|/$", ""); // Remove leading and trailing slashes
            
        // Handle root path
        if (normalized.isEmpty()) {
            return "/";
        }
        
        // Ensure path starts with a single slash
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        
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
