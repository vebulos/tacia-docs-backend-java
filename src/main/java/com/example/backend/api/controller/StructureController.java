package com.example.backend.api.controller;

import com.example.backend.api.dto.ContentItemDto;
import com.example.backend.api.exception.ContentNotFoundException;
import com.example.backend.model.ContentItem;
import com.example.backend.repository.ContentRepository;
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
     */
    @GetMapping
    public ResponseEntity<ContentItemDto> getStructure(
            @RequestParam String path) {
        
        String normalizedPath = normalizePath(path);
        ContentItem item = contentRepository.findByPath(normalizedPath)
            .orElseThrow(() -> new ContentNotFoundException("Content not found: " + normalizedPath));
        
        ContentItemDto dto = ContentItemDto.fromDomain(item);
        
        // For directories, include children
        if ("directory".equals(item.type())) {
            List<ContentItem> children = contentRepository.findChildren(normalizedPath);
            List<ContentItemDto> childDtos = children.stream()
                .map(ContentItemDto::fromDomain)
                .collect(Collectors.toList());
            dto = ContentItemDto.withChildren(item, childDtos);
        }
        
        return ResponseEntity.ok(dto);
    }
    
    /**
     * Get content structure recursively from the specified path
     */
    @GetMapping("/recursive")
    public ResponseEntity<ContentItemDto> getStructureRecursive(
            @RequestParam String path) {
        
        String normalizedPath = normalizePath(path);
        ContentItem item = contentRepository.findByPath(normalizedPath)
            .orElseThrow(() -> new ContentNotFoundException("Content not found: " + normalizedPath));
        
        // For files, return just the item
        if ("file".equals(item.type())) {
            return ResponseEntity.ok(ContentItemDto.fromDomain(item));
        }
        
        // For directories, build the full tree
        ContentItemDto dto = buildDirectoryTree(item, normalizedPath);
        return ResponseEntity.ok(dto);
    }
    
    /**
     * Builds a directory tree recursively
     */
    private ContentItemDto buildDirectoryTree(ContentItem directory, String basePath) {
        List<ContentItem> children = contentRepository.findChildren(basePath);
        
        List<ContentItemDto> childDtos = children.stream()
            .map(child -> {
                String childPath = basePath + "/" + child.name();
                return "directory".equals(child.type()) ? 
                    buildDirectoryTree(child, childPath) : 
                    ContentItemDto.fromDomain(child);
            })
            .collect(Collectors.toList());
            
        return ContentItemDto.withChildren(directory, childDtos);
    }
    
    /**
     * Normalizes the given path
     */
    private String normalizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "/";
        }
        
        // Remove leading/trailing whitespace and slashes
        path = path.trim();
        
        // Ensure path starts with a single slash
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        // Remove trailing slash unless it's the root
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        
        return path;
    }
}
