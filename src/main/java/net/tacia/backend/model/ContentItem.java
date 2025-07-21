package net.tacia.backend.model;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents a content item in the file system (file or directory).
 * Contains metadata and ordering information for display purposes.
 */
public record ContentItem(
    String name,            // Name of the file or directory
    String type,            // 'file' or 'directory'
    String path,            // Relative path from content root
    long size,              // Size in bytes (0 for directories)
    Instant lastModified,   // Last modification time
    Integer order,          // Custom sort order (optional)
    Map<String, Object> metadata  // Additional metadata (frontmatter or .metadata file)
) {
    
    // Default empty metadata map
    private static final Map<String, Object> EMPTY_METADATA = new HashMap<>();
    
    // Default order when not specified
    private static final Integer DEFAULT_ORDER = null;
    /**
     * Creates a directory content item with default order and empty metadata.
     */
    public static ContentItem directory(String name, String path, Instant lastModified) {
        return new ContentItem(
            name, 
            "directory", 
            path, // Do not add trailing slash
            0, 
            lastModified,
            DEFAULT_ORDER,
            EMPTY_METADATA
        );
    }
    
    /**
     * Creates a directory content item with custom order and metadata.
     */
    public static ContentItem directory(String name, String path, Instant lastModified, 
                                      Integer order, Map<String, Object> metadata) {
        return new ContentItem(
            name,
            "directory",
            path,
            0,
            lastModified,
            order,
            metadata != null ? new HashMap<>(metadata) : EMPTY_METADATA
        );
    }

    /**
     * Creates a file content item with default order and empty metadata.
     */
    public static ContentItem file(String name, String path, long size, Instant lastModified) {
        return new ContentItem(
            name, 
            "file", 
            path, 
            size, 
            lastModified,
            DEFAULT_ORDER,
            EMPTY_METADATA
        );
    }
    
    /**
     * Creates a file content item with custom order and metadata.
     */
    public static ContentItem file(String name, String path, long size, Instant lastModified,
                                 Integer order, Map<String, Object> metadata) {
        return new ContentItem(
            name,
            "file",
            path,
            size,
            lastModified,
            order,
            metadata != null ? new HashMap<>(metadata) : EMPTY_METADATA
        );
    }

    /**
     * Returns a new ContentItem with updated metadata.
     */
    public ContentItem withMetadata(Map<String, Object> newMetadata) {
        if (newMetadata == null || newMetadata.isEmpty()) {
            return this;
        }
        
        Map<String, Object> mergedMetadata = new HashMap<>(this.metadata);
        mergedMetadata.putAll(newMetadata);
        
        return new ContentItem(
            this.name,
            this.type,
            this.path,
            this.size,
            this.lastModified,
            this.order,
            mergedMetadata
        );
    }
    
    /**
     * Returns a new ContentItem with updated order.
     */
    public ContentItem withOrder(Integer newOrder) {
        return new ContentItem(
            this.name,
            this.type,
            this.path,
            this.size,
            this.lastModified,
            newOrder,
            this.metadata
        );
    }
}
