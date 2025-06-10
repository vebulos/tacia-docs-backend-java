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
                    } else if (recursive) {
                        List<ContentItemDto> children = getChildrenRecursively(normalizedPath);
                        return ResponseEntity.ok(ContentItemDto.withChildren(item, children));
                    }
                    return ResponseEntity.ok(ContentItemDto.fromDomain(item));
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
        return contentRepository.findChildren(path).stream()
            .map(item -> {
                if ("directory".equals(item.type())) {
                    List<ContentItemDto> children = getChildrenRecursively(item.path());
                    return ContentItemDto.withChildren(item, children);
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
        String normalized = path.replaceAll("^/|/$", "");
        return "/" + normalized;
    }

    private String getFileName(String path) {
        if (path == null || path.equals("/")) {
            return "";
        }
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
}
