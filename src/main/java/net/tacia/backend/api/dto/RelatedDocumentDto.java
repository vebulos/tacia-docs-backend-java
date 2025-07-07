package net.tacia.backend.api.dto;

import java.time.Instant;

/**
 * DTO representing a document related to another document
 */
public record RelatedDocumentDto(
    /** The path to the related document */
    String path,
    
    /** The title of the related document */
    String title,
    
    /** 
     * Relevance score indicating how related the document is (matching backend-js implementation)
     * - 2.0: Document is in the same directory
     * - 1.0: Document is in a different directory
     */
    @com.fasterxml.jackson.annotation.JsonProperty("relevance")
    double relevance,
    
    /** Last modification time */
    Instant lastModified
) {
    /**
     * Creates a new RelatedDocumentDto with the current time as lastModified
     */
    public RelatedDocumentDto(String path, String title, double relevance) {
        this(path, title, relevance, Instant.now());
    }
}
