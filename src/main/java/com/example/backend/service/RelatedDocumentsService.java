package com.example.backend.service;

import com.example.backend.api.dto.RelatedDocumentDto;

import java.util.List;

/**
 * Service for finding related documents based on content similarity
 */
public interface RelatedDocumentsService {
    
    /**
     * Find documents related to the specified document
     *
     * @param documentPath Path of the document to find related documents for
     * @param limit        Maximum number of related documents to return
     * @param skipCache
     * @return List of related documents with relevance scores
     */
    List<RelatedDocumentDto> findRelatedDocuments(String documentPath, int limit, boolean skipCache);
}
