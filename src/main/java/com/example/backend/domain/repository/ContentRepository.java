package com.example.backend.domain.repository;

import com.example.backend.domain.model.ContentItem;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface ContentRepository {
    /**
     * Get content item at the specified path
     */
    Optional<ContentItem> findByPath(String path);

    /**
     * Get all direct children of the specified path
     */
    List<ContentItem> findChildren(String path);

    /**
     * Save or update a content item
     */
    ContentItem save(ContentItem item, String content);

    /**
     * Delete a content item
     */
    boolean delete(String path);


    /**
     * Check if a path exists
     */
    boolean exists(String path);

    /**
     * Get the absolute path for a given content path
     */
    Path getAbsolutePath(String path);
    
    /**
     * Read the content of a file
     * @param path Path to the file
     * @return The file content as a string
     * @throws IOException If the file cannot be read
     */
    String readContent(String path) throws IOException;
}
