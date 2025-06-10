package com.example.backend.api.controller;

import com.example.backend.api.dto.ContentItemDto;
import com.example.backend.api.exception.ContentNotFoundException;
import com.example.backend.domain.model.ContentItem;
import com.example.backend.domain.repository.ContentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/content")
public class ContentController {

    private final ContentRepository contentRepository;

    public ContentController(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    @GetMapping("")
    public ResponseEntity<List<ContentItemDto>> listRoot() {
        return listContent("/");
    }

    @GetMapping("/**")
    public ResponseEntity<List<ContentItemDto>> listContent(@RequestParam(required = false) String path) {
        String normalizedPath = normalizePath(path);
        List<ContentItem> items = contentRepository.findChildren(normalizedPath);
        List<ContentItemDto> dtos = items.stream()
            .map(ContentItemDto::fromDomain)
            .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/item/**")
    public ResponseEntity<ContentItemDto> getContent(@RequestParam(required = false) String path) {
        String normalizedPath = normalizePath(path);
        return contentRepository.findByPath(normalizedPath)
            .map(item -> {
                try {
                    if ("file".equals(item.type())) {
                        String content = contentRepository.readContent(normalizedPath);
                        return ResponseEntity.ok(ContentItemDto.withContent(item, content));
                    }
                    return ResponseEntity.ok(ContentItemDto.fromDomain(item));
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read content: " + normalizedPath, e);
                }
            })
            .orElseThrow(() -> new ContentNotFoundException(normalizedPath));
    }

    @PostMapping("/**")
    public ResponseEntity<ContentItemDto> saveContent(
            @RequestParam(required = false) String path,
            @RequestBody String content) {
        String normalizedPath = normalizePath(path);
        // This is a simplified version - in a real app, you'd want to parse the content
        // and handle different content types appropriately
        ContentItem item = ContentItem.file(
            getFileName(normalizedPath),
            normalizedPath,
            "text/plain",
            content.getBytes().length,
            null,
            Map.of()
        );
        
        ContentItem savedItem = contentRepository.save(item, content);
        return ResponseEntity.ok(ContentItemDto.fromDomain(savedItem));
    }

    @DeleteMapping("/**")
    public ResponseEntity<Void> deleteContent(@RequestParam(required = false) String path) {
        String normalizedPath = normalizePath(path);
        if (!contentRepository.exists(normalizedPath)) {
            throw new ContentNotFoundException(normalizedPath);
        }
        contentRepository.delete(normalizedPath);
        return ResponseEntity.noContent().build();
    }

    private String normalizePath(String path) {
        if (path == null || path.trim().isEmpty() || "/".equals(path.trim())) {
            return "/";
        }
        // Remove leading/trailing slashes and normalize
        String normalized = path.replaceAll("^/+|/+$", "");
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
