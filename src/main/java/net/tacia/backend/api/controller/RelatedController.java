package net.tacia.backend.api.controller;

import net.tacia.backend.api.dto.RelatedDocumentDto;
import net.tacia.backend.service.RelatedDocumentsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for finding related documents
 */
@RestController
@RequestMapping("/api/related")
public class RelatedController {
    
    private static final Logger logger = LoggerFactory.getLogger(RelatedController.class);
    private final RelatedDocumentsService relatedDocumentsService;
    
    public RelatedController(RelatedDocumentsService relatedDocumentsService) {
        this.relatedDocumentsService = relatedDocumentsService;
    }
    
    /**
     * Get documents related to the specified document
     * 
     * @param path Path of the document to find related documents for (required)
     * @param limit Maximum number of related documents to return (default: 5)
     * @param skipCache Whether to skip the cache (default: false)
     * @return ResponseEntity containing related documents or an error message
     */
    @GetMapping
    public ResponseEntity<?> getRelatedDocuments(
            @RequestParam(required = false) String path,
            @RequestParam(required = false, defaultValue = "5") int limit,
            @RequestParam(required = false, defaultValue = "false") boolean skipCache) {
        
        logger.debug("Getting related documents for path: {}, limit: {}, skipCache: {}", path, limit, skipCache);
        
        // Validate required parameters
        if (path == null || path.trim().isEmpty()) {
            logger.error("Missing required parameter: path");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Missing document path");
            errorResponse.put("related", List.of());
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        try {
            // Find related documents using the service
            List<RelatedDocumentDto> related = relatedDocumentsService.findRelatedDocuments(path, limit, skipCache);
            
            // Build the response
            Map<String, Object> response = new HashMap<>();
            response.put("related", related);
            response.put("fromCache", !skipCache); // Assuming service uses cache when skipCache is false
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error finding related documents for path: " + path, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get related documents");
            errorResponse.put("details", e.getMessage());
            errorResponse.put("related", List.of());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
