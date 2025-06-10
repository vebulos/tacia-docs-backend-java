package com.example.backend.api.controller;

import com.example.backend.api.dto.ContentItemDto;
import com.example.backend.api.exception.ContentNotFoundException;
import com.example.backend.domain.model.ContentItem;
import com.example.backend.domain.repository.ContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ContentController {

    private static final Logger logger = LoggerFactory.getLogger(ContentController.class);
    private final ContentRepository contentRepository;

    public ContentController(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
        logger.info("ContentController initialized");
    }

    @GetMapping("/content")
    public ResponseEntity<List<ContentItemDto>> listContent(
            @RequestParam(required = false, defaultValue = "/") String path) {
        String normalizedPath = normalizePath(path);
        logger.info("Listing content at path: {}", normalizedPath);

        List<ContentItem> items = contentRepository.findChildren(normalizedPath);
        List<ContentItemDto> dtos = items.stream()
            .map(ContentItemDto::fromDomain)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/content/item")
    public ResponseEntity<ContentItemDto> getContent(
            @RequestParam String path,
            @RequestParam(required = false, defaultValue = "false") boolean recursive) {
        String normalizedPath = normalizePath(path);
        logger.info("Getting content at path: {} (recursive: {})", normalizedPath, recursive);

        return contentRepository.findByPath(normalizedPath)
            .map(item -> {
                try {
                    if ("file".equals(item.type())) {
                        String content = contentRepository.readContent(normalizedPath);
                        return ResponseEntity.ok(ContentItemDto.withContent(item, content));
                    } else {
                        // For directories, always include children when recursive is true
                        if (recursive) {
                            List<ContentItemDto> children = getChildrenRecursively(normalizedPath);
                            return ResponseEntity.ok(ContentItemDto.withChildren(item, children));
                        } else {
                            // For non-recursive, just return the directory info
                            return ResponseEntity.ok(ContentItemDto.fromDomain(item));
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read content: " + normalizedPath, e);
                }
            })
            .orElseThrow(() -> new ContentNotFoundException(normalizedPath));
    }

    @PostMapping("/content")
    public ResponseEntity<ContentItemDto> saveContent(
            @RequestParam String path,
            @RequestBody String content) {
        String normalizedPath = normalizePath(path);
        logger.info("Saving content to: {}", normalizedPath);

        ContentItem item = contentRepository.save(
            new ContentItem(
                getFileName(normalizedPath),
                "file",
                normalizedPath,
                content.getBytes().length,
                java.time.Instant.now()
            ),
            content
        );
        
        return ResponseEntity.ok(ContentItemDto.withContent(item, content));
    }

    @DeleteMapping("/content")
    public ResponseEntity<Void> deleteContent(@RequestParam String path) {
        String normalizedPath = normalizePath(path);
        logger.info("Deleting content at: {}", normalizedPath);

        if (!contentRepository.exists(normalizedPath)) {
            throw new ContentNotFoundException(normalizedPath);
        }
        contentRepository.delete(normalizedPath);
        return ResponseEntity.noContent().build();
    }

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
                return ContentItemDto.fromDomain(item);
            })
            .collect(Collectors.toList());
    }

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

    private String getFileName(String path) {
        if (path == null || path.equals("/")) {
            return "";
        }
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
}
