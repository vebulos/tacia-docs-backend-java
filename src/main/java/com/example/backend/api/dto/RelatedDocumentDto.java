package com.example.backend.api.dto;

import java.time.Instant;

/**
 * DTO representing a document related to another document
 */
public record RelatedDocumentDto(
    /** The path to the related document */
    String path,
    
    /** The title of the related document */
    String title,
    
    /** Relevance score (0.0 to 1.0) */
    double score,
    
    /** Last modification time */
    Instant lastModified
) {
    /**
     * Creates a new RelatedDocumentDto with the current time as lastModified
     */
    public RelatedDocumentDto(String path, String title, double score) {
        this(path, title, score, Instant.now());
    }
}
