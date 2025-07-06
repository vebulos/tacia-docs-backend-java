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
        
        logger.info("Finding first document in directory: '{}'", directory);
        
        try {
            String normalizedDir = normalizeDirectoryPath(directory);
            logger.debug("Normalized directory path: '{}'", normalizedDir);
            
            Path searchDir = contentRepository.getAbsolutePath(normalizedDir);
            logger.debug("Absolute search path: '{}'", searchDir);
            
            // Verify the directory exists and is accessible
            if (!Files.exists(searchDir)) {
                logger.warn("Directory does not exist: {}", searchDir);
                return ResponseEntity.status(404).body(Map.of(
                    "error", "Directory not found",
                    "message", "The requested directory was not found: " + directory,
                    "absolutePath", searchDir.toString()
                ));
            }
            
            if (!Files.isDirectory(searchDir)) {
                logger.warn("Path is not a directory: {}", searchDir);
                return ResponseEntity.status(400).body(Map.of(
                    "error", "Not a directory",
                    "message", "The specified path is not a directory: " + directory,
                    "absolutePath", searchDir.toString()
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
     * Matches the behavior of the backend-js implementation:
     * - Sorts entries (directories first, then files)
     * - Skips hidden directories (starting with '.')
     */
    private Path findFirstMarkdownFile(Path directory) throws IOException {
        logger.debug("Searching for markdown files in: {}", directory);
        
        // Get all entries in the directory
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            // Sort entries (directories first, then files, both alphabetically)
            Map<Boolean, java.util.List<Path>> entries = new TreeMap<>();
            entries.put(Boolean.TRUE, new java.util.ArrayList<>());  // Directories
            entries.put(Boolean.FALSE, new java.util.ArrayList<>()); // Files
            
            for (Path entry : stream) {
                boolean isDir = Files.isDirectory(entry);
                // Skip hidden directories
                if (isDir && entry.getFileName().toString().startsWith(".")) {
                    continue;
                }
                entries.get(isDir).add(entry);
            }
            
            // Sort each list alphabetically
            Comparator<Path> pathComparator = Comparator.comparing(p -> p.getFileName().toString().toLowerCase());
            entries.get(true).sort(pathComparator);
            entries.get(false).sort(pathComparator);
            
            // First check files in the current directory
            for (Path file : entries.get(false)) {
                String fileName = file.getFileName().toString();
                if (fileName.toLowerCase().endsWith(".md")) {
                    logger.debug("Found markdown file: {}", file);
                    return file;
                } else {
                    logger.trace("Skipping non-markdown file: {}", file);
                }
            }
            
            // Then check directories recursively
            for (Path dir : entries.get(true)) {
                logger.debug("Searching in subdirectory: {}", dir);
                try {
                    Path found = findFirstMarkdownFile(dir);
                    if (found != null) {
                        return found;
                    }
                } catch (Exception e) {
                    logger.warn("Error searching in directory {}: {}", dir, e.getMessage(), e);
                    // Continue with next directory on error
                }
            }
            
            return null;
        }
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
