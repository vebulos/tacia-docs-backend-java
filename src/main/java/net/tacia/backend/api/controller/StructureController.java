package net.tacia.backend.api.controller;

import net.tacia.backend.api.dto.ContentItemDto;
import net.tacia.backend.api.dto.StructureResponseDto;
import net.tacia.backend.api.exception.ContentNotFoundException;
import net.tacia.backend.model.ContentItem;
import net.tacia.backend.repository.ContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for handling content structure requests
 */
@RestController
@RequestMapping("/api/structure")
public class StructureController {
    
    private static final Logger logger = LoggerFactory.getLogger(StructureController.class);
    private final ContentRepository contentRepository;
    
    public StructureController(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }
    
    /**
     * Get content structure at the specified path
     * @param path The path to get structure for (path parameter)
     * @return The content item with its children if it's a directory
     */
    @GetMapping("/{*path}")
    public ResponseEntity<StructureResponseDto> getStructure(
            @PathVariable(value = "path", required = false) String path) {
        
        String normalizedPath = normalizePath(path);
        logger.debug("Getting structure for path: {}", normalizedPath);
        
        ContentItem item = contentRepository.findByPath(normalizedPath)
            .orElseThrow(() -> new ContentNotFoundException("Content not found: " + normalizedPath));
        
        if (!"directory".equals(item.type())) {
            // If the path is a file, return a 400 Bad Request
            throw new ContentNotFoundException("Path is not a directory: " + normalizedPath);
        }
        
        // Get all children of the directory
        List<ContentItem> children = contentRepository.findChildren(normalizedPath);
        List<ContentItemDto> childDtos = children.stream()
            .map(ContentItemDto::fromDomain)
            .collect(Collectors.toList());
            
        // Create response with the directory path and its children
        StructureResponseDto response = StructureResponseDto.of(
            normalizedPath.equals("/") ? "" : normalizedPath,
            childDtos
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get content structure at the root path
     */
    @GetMapping
    public ResponseEntity<StructureResponseDto> getRootStructure() {
        return getStructure("");
    }
    
    /**
     * Normalize the path by ensuring it starts with a slash and doesn't end with one
     */
    private String normalizePath(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return "/";
        }
        
        // Remove leading and trailing slashes
        path = path.replaceAll("^/|/$", "");
        
        // Ensure it starts with a single slash
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        return path;
    }
}
