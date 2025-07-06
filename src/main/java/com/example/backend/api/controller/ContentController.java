package com.example.backend.api.controller;

import com.example.backend.api.dto.ContentItemDto;
import com.example.backend.api.exception.ContentNotFoundException;
import com.example.backend.model.ContentItem;
import com.example.backend.repository.ContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for managing content (files and directories)
 */
@RestController
@RequestMapping("/api")
public class ContentController {

    private static final Logger logger = LoggerFactory.getLogger(ContentController.class);
    private final ContentRepository contentRepository;

    public ContentController(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    /**
     * List content at the specified path
     */
    @GetMapping("/content")
    public ResponseEntity<List<ContentItemDto>> listContent(
            @RequestParam(required = false, defaultValue = "/") String path) {
        
        String normalizedPath = normalizePath(path);
        List<ContentItem> items = contentRepository.findChildren(normalizedPath);
        
        List<ContentItemDto> dtos = items.stream()
            .map(ContentItemDto::fromDomain)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get content at the specified path
     */
    /**
     * Extracts the path from the request URL
     */
    private String extractPathFromRequest() {
        // Get the full request URI and extract the part after /content/
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
            String requestURI = request.getRequestURI();
            int contentIndex = requestURI.indexOf("/content/");
            if (contentIndex >= 0) {
                return requestURI.substring(contentIndex + 8); // +8 to skip "/content/"
            }
        }
        return "/";
    }

    @GetMapping("/content/**")
    public ResponseEntity<ContentItemDto> getContent(
            @RequestParam(required = false, defaultValue = "false") boolean recursive) {
        
        String path = extractPathFromRequest();
        String normalizedPath = normalizePath(path);

        ContentItem item = contentRepository.findByPath(normalizedPath)
            .orElseThrow(() -> new ContentNotFoundException("Content not found: " + normalizedPath));
        
        ContentItemDto dto;
        
        try {
            // Handle files
            if ("file".equals(item.type())) {
                String content = contentRepository.getContent(normalizedPath);
                dto = ContentItemDto.withContent(item, content, null);
            } 
            // Handle directories
            else {
                List<ContentItem> children = recursive ? 
                    contentRepository.findDescendants(normalizedPath) : 
                    contentRepository.findChildren(normalizedPath);
                    
                List<ContentItemDto> childDtos = children.stream()
                    .map(ContentItemDto::fromDomain)
                    .collect(Collectors.toList());
                    
                dto = ContentItemDto.withChildren(item, childDtos);
            }
            
            return ResponseEntity.ok(dto);
            
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

        // Create or update the file
        ContentItem savedItem;
        try {
            // First check if the parent directory exists
            String parentPath = getParentPath(normalizedPath);
            if (!parentPath.isEmpty() && !"/".equals(parentPath) && 
                !contentRepository.findByPath(parentPath).isPresent()) {
                // Create parent directories if they don't exist
                contentRepository.saveContent(parentPath, "");
            }
            
            // Now save the file
            savedItem = contentRepository.saveContent(normalizedPath, content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save content: " + e.getMessage(), e);
        }
        
        ContentItemDto dto = ContentItemDto.withContent(savedItem, content, null);
        
        return ResponseEntity.ok(dto);
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
        // Ensure path starts with a single slash and doesn't end with one (except root)
        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        // Remove trailing slash unless it's the root
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
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
