package com.example.backend.repository;

import com.example.backend.model.ContentItem;

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
     * Get all descendants (recursive) of the specified path
     * @param path The parent path to search under
     * @return List of all descendant content items
     */
    List<ContentItem> findDescendants(String path);

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
     * Get the relative path from the content root
     * @param path The absolute path to convert to relative
     * @return The relative path as a string with forward slashes
     */
    String getRelativePath(Path path);
    
    /**
     * Read the content of a file
     * @param path Path to the file
     * @return The file content as a string
     * @throws IOException If the file cannot be read
     */
    String readContent(String path) throws IOException;
    
    /**
     * Get content as a string for the given path
     * @param path Path to the content
     * @return Content as string
     * @throws IOException If the content cannot be read
     */
    String getContent(String path) throws IOException;
    
    /**
     * Get all markdown files in the content directory
     * @return List of paths to markdown files
     */
    List<String> getAllMarkdownFiles();
    
    /**
     * Save content to the specified path
     * @param path Path where to save the content
     * @param content Content to save
     * @return The saved content item
     * @throws IOException If the content cannot be saved
     */
    ContentItem saveContent(String path, String content) throws IOException;
}
