package com.example.backend.api.controller;

import com.example.backend.repository.ContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Controller for finding the first available document in a directory
 */
@RestController
@RequestMapping("/api/first-document")
public class FirstDocumentController {
    
    private static final Logger logger = LoggerFactory.getLogger(FirstDocumentController.class);
    private final ContentRepository contentRepository;
    
    public FirstDocumentController(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
        logger.info("FirstDocumentController initialized");
    }
    
    /**
     * Find the first markdown document in the specified directory
     * 
     * @param directory The directory to search in (optional, defaults to root)
     * @return The path to the first markdown document found
     */
    @GetMapping
    public ResponseEntity<?> getFirstDocument(
            @RequestParam(required = false, defaultValue = "") String directory) {
        
        logger.debug("Finding first document in directory: {}", directory);
        
        try {
            String normalizedDir = normalizeDirectoryPath(directory);
            Path searchDir = contentRepository.getAbsolutePath(normalizedDir);
            
            // Verify the directory exists and is accessible
            if (!Files.exists(searchDir) || !Files.isDirectory(searchDir)) {
                logger.warn("Directory not found or not accessible: {}", searchDir);
                return ResponseEntity.status(404).body(Map.of(
                    "error", "Directory not found",
                    "message", "The requested directory was not found: " + directory
                ));
            }
            
            // Find the first markdown file
            Path firstDocPath = findFirstMarkdownFile(searchDir);
            
            if (firstDocPath == null) {
                logger.warn("No markdown files found in directory: {}", searchDir);
                return ResponseEntity.status(404).body(Map.of(
                    "error", "No markdown files found",
                    "message", "No markdown files found in directory: " + directory
                ));
            }
            
            // Convert to relative path from content root
            String relativePath = contentRepository.getRelativePath(firstDocPath);
            
            logger.info("Found first document in {}: {}", directory, relativePath);
            
            return ResponseEntity.ok(Map.of(
                "path", relativePath,
                "fullPath", firstDocPath.toString()
            ));
            
        } catch (Exception e) {
            logger.error("Error finding first document in {}: {}", directory, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to find first document",
                "details", e.getMessage()
            ));
        }
    }
    
    /**
     * Recursively finds the first markdown file in a directory
     */
    private Path findFirstMarkdownFile(Path directory) throws IOException {
        final Path[] firstMarkdownFile = { null };
        
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().toLowerCase().endsWith(".md")) {
                    firstMarkdownFile[0] = file;
                    return FileVisitResult.TERMINATE;
                }
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                // Skip hidden directories
                if (dir.getFileName().toString().startsWith(".")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                logger.warn("Failed to access file: " + file, exc);
                return FileVisitResult.CONTINUE;
            }
        });
        
        return firstMarkdownFile[0];
    }
    
    /**
     * Normalizes a directory path
     */
    private String normalizeDirectoryPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "";
        }
        // Remove leading/trailing slashes and normalize
        return path.trim()
            .replace("\\", "/")
            .replaceAll("^/|/$", "");
    }
    
    /**
     * Response DTO for the first document endpoint
     */
    public record FirstDocumentResponse(String path, String fullPath) {}
}
