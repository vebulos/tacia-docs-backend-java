package com.example.backend.service;

import com.example.backend.api.dto.RelatedDocumentDto;
import com.example.backend.model.ContentItem;
import com.example.backend.repository.ContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of RelatedDocumentsService that finds related documents based on path similarity.
 * This matches the behavior of the backend-js implementation.
 */
@Service
public class SimpleRelatedDocumentsService implements RelatedDocumentsService {
    
    private static final Logger log = LoggerFactory.getLogger(SimpleRelatedDocumentsService.class);
    
    private final ContentRepository contentRepository;
    
    @Autowired
    public SimpleRelatedDocumentsService(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }
    
    @Override
    public List<RelatedDocumentDto> findRelatedDocuments(String documentPath, int limit, boolean skipCache) {
        try {
            // Normalize the path (similar to the JS implementation)
            String normalizedPath = normalizePath(documentPath);
            log.debug("Finding related documents for: {}", normalizedPath);
            
            // Get the parent directory of the current document
            String parentPath = getParentPath(normalizedPath);
            
            // Get all markdown files in the same directory
            List<ContentItem> allDocs = contentRepository.findDescendants(parentPath);
            
            // Get the current document's directory for relevance calculation
            String currentDir = getParentPath(normalizedPath);
            
            // Process each file to check for path similarity
            List<RelatedDocumentDto> relatedDocs = allDocs.stream()
                .filter(doc -> !doc.path().equals(normalizedPath)) // Skip the current document
                .filter(doc -> isMarkdownFile(doc.path()))
                .map(doc -> {
                    // Calculate relevance based on directory similarity
                    // Files in the same directory are more relevant (relevance = 2)
                    // Files in subdirectories are less relevant (relevance = 1)
                    String docDir = getParentPath(doc.path());
                    double relevance = currentDir.equals(docDir) ? 2.0 : 1.0;
                    
                    // Format the title from the filename (capitalize words, replace hyphens/underscores with spaces)
                    String title = formatTitle(doc.name());
                    
                    return new RelatedDocumentDto(
                        doc.path().replace(".md", ""),
                        title,
                        relevance
                    );
                })
                .sorted((a, b) -> Double.compare(b.relevance(), a.relevance())) // Sort by relevance (highest first)
                .limit(limit)
                .collect(Collectors.toList());
            
            log.debug("Found {} related documents for: {}", relatedDocs.size(), normalizedPath);
            return relatedDocs;
            
        } catch (Exception e) {
            log.error("Error finding related documents for: " + documentPath, e);
            return Collections.emptyList();
        }
    }
    
    private String normalizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "";
        }
        // Replace backslashes, remove leading/trailing slashes, and ensure .md extension
        return path.replace("\\", "/")
                  .replaceAll("^/+|/+$", "")
                  .replaceAll("\\.(md|markdown)$", "") + ".md";
    }
    
    private String getParentPath(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash > 0 ? path.substring(0, lastSlash) : "";
    }
    
    private boolean isMarkdownFile(String path) {
        return path != null && 
              (path.endsWith(".md") || path.endsWith(".markdown"));
    }
    
    private String formatTitle(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        // Remove file extension
        String name = filename.replaceAll("\\.(md|markdown)$", "");
        // Replace hyphens and underscores with spaces
        name = name.replaceAll("[-_]", " ");
        // Capitalize first letter of each word
        return Arrays.stream(name.split("\\s+"))
                    .map(word -> word.isEmpty() ? "" : 
                         Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                    .collect(Collectors.joining(" "));
    }
}
