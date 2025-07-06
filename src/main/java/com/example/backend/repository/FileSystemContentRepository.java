package com.example.backend.repository;

import com.example.backend.model.ContentItem;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.*;
import java.io.IOException;
import java.nio.file.StandardOpenOption;

public class FileSystemContentRepository implements ContentRepository {

    private final Path contentRoot;

    public FileSystemContentRepository(Path contentRoot) {
        this.contentRoot = contentRoot.toAbsolutePath().normalize();
        createDirectoriesIfNotExists(this.contentRoot);
    }

    @Override
    public Optional<ContentItem> findByPath(String path) {
        try {
            Path fullPath = resolvePath(path);
            if (!Files.exists(fullPath)) {
                return Optional.empty();
            }

            BasicFileAttributes attrs = Files.readAttributes(fullPath, BasicFileAttributes.class);
            String name = getFileName(fullPath);
            String type = Files.isDirectory(fullPath) ? "directory" : "file";
            long size = attrs.size();
            Instant lastModified = attrs.lastModifiedTime().toInstant();

            // Use the normalized path that matches how we store it
            String normalizedPath = "/" + contentRoot.relativize(fullPath).toString().replace("\\", "/");
            
            // For directories, ensure path doesn't end with a slash
            if (normalizedPath.endsWith("/")) {
                normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
            }

            return Optional.of(new ContentItem(name, type, normalizedPath, size, lastModified));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file attributes: " + path, e);
        }
    }

    @Override
    public List<ContentItem> findDescendants(String path) {
        List<ContentItem> descendants = new ArrayList<>();
        Path startPath = resolvePath(path);

        if (!Files.exists(startPath)) {
            return descendants;
        }

        try {
            Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        String relativePath = "/" + contentRoot.relativize(file).toString().replace("\\", "/");
                        String name = getFileName(file);
                        String type = "file";
                        long size = attrs.size();
                        Instant lastModified = attrs.lastModifiedTime().toInstant();

                        descendants.add(new ContentItem(name, type, relativePath, size, lastModified));
                    } catch (Exception e) {
                        // Skip files we can't process
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (!dir.equals(startPath)) { // Don't include the starting directory itself
                        try {
                            String relativePath = "/" + contentRoot.relativize(dir).toString().replace("\\", "/") + "/";
                            String name = getFileName(dir);
                            String type = "directory";
                            long size = 0; // Directories don't have size
                            Instant lastModified = attrs.lastModifiedTime().toInstant();

                            descendants.add(new ContentItem(name, type, relativePath, size, lastModified));
                        } catch (Exception e) {
                            // Skip directories we can't process
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to walk directory tree: " + path, e);
        }

        return descendants;
    }

    @Override
    public List<ContentItem> findChildren(String path) {
        List<ContentItem> children = new ArrayList<>();
        Path dirPath = resolvePath(path);

        if (!Files.isDirectory(dirPath)) {
            return children;
        }

        // Normalize the parent path
        String parentPath = path.endsWith("/") ? path : path + "/";

        try (var stream = Files.list(dirPath)) {
            stream.sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .forEach(childPath -> {
                    try {
                        BasicFileAttributes attrs = Files.readAttributes(childPath, BasicFileAttributes.class);
                        String name = getFileName(childPath);
                        String type = Files.isDirectory(childPath) ? "directory" : "file";
                        String childPathStr = parentPath + name;
                        
                        // Ensure directory paths end with a slash
                        if (type.equals("directory") && !childPathStr.endsWith("/")) {
                            childPathStr += "/";
                        }
                        
                        long size = attrs.size();
                        Instant lastModified = attrs.lastModifiedTime().toInstant();

                        children.add(new ContentItem(name, type, childPathStr, size, lastModified));
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read child: " + childPath, e);
                    }
                });
        } catch (IOException e) {
            throw new RuntimeException("Failed to list directory: " + path, e);
        }

        return children;
    }

    @Override
    public ContentItem save(ContentItem item, String content) {
        try {
            Path fullPath = resolvePath(item.path());
            Files.createDirectories(fullPath.getParent());
            Files.writeString(fullPath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            // Update the item with the actual file attributes
            BasicFileAttributes attrs = Files.readAttributes(fullPath, BasicFileAttributes.class);
            return new ContentItem(
                item.name(),
                item.type(),
                item.path(),
                attrs.size(),
                attrs.lastModifiedTime().toInstant()
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to save content: " + item.path(), e);
        }
    }

    @Override
    public boolean delete(String path) {
        try {
            Path fullPath = resolvePath(path);
            if (!Files.exists(fullPath)) {
                return false;
            }
            
            if (Files.isDirectory(fullPath)) {
                // Delete directory recursively
                Files.walk(fullPath)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to delete: " + p, e);
                        }
                    });
            } else {
                Files.deleteIfExists(fullPath);
            }
            return true;
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete: " + path, e);
        }
    }

    @Override
    public boolean exists(String path) {
        return Files.exists(resolvePath(path));
    }

    @Override
    public Path getAbsolutePath(String path) {
        return resolvePath(path);
    }

    @Override
    public String readContent(String path) throws IOException {
        return Files.readString(resolvePath(path));
    }
    
    @Override
    public Optional<String> getContent(String path) {
        try {
            // Remove .md extension if present for consistency with JS implementation
            String normalizedPath = path;
            if (normalizedPath.endsWith(".md")) {
                normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 3);
            }
            
            // Try with .md extension first
            try {
                return Optional.of(Files.readString(resolvePath(normalizedPath + ".md")));
            } catch (NoSuchFileException e) {
                // If .md file not found, try without extension
                try {
                    return Optional.of(Files.readString(resolvePath(normalizedPath)));
                } catch (NoSuchFileException e2) {
                    return Optional.empty();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read content: " + path, e);
        }
    }
    
    @Override
    public ContentItem saveContent(String path, String content) throws IOException {
        Path fullPath = resolvePath(path);
        
        // Create parent directories if they don't exist
        Files.createDirectories(fullPath.getParent());
        
        // Write the content to the file
        Files.writeString(fullPath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        
        // Get the file attributes to return the updated item
        BasicFileAttributes attrs = Files.readAttributes(fullPath, BasicFileAttributes.class);
        String name = getFileName(fullPath);
        String type = Files.isDirectory(fullPath) ? "directory" : "file";
        long size = attrs.size();
        Instant lastModified = attrs.lastModifiedTime().toInstant();
        
        return new ContentItem(name, type, "/" + contentRoot.relativize(fullPath).toString().replace("\\", "/"), size, lastModified);
    }

    private Path resolvePath(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return contentRoot;
        }
        // Remove leading slash if present
        String relativePath = path.startsWith("/") ? path.substring(1) : path;
        Path resolvedPath = contentRoot.resolve(relativePath).normalize().toAbsolutePath();
        
        // Security check: ensure the resolved path is within the content root
        if (!resolvedPath.startsWith(contentRoot)) {
            throw new SecurityException("Access to requested path is not allowed: " + path);
        }
        
        return resolvedPath;
    }

    private void createDirectoriesIfNotExists(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directory: " + path, e);
        }
    }

    /**
     * Get all markdown files in the content directory
     * @return List of paths to markdown files
     */
    public List<String> getAllMarkdownFiles() {
        List<String> markdownFiles = new ArrayList<>();
        try {
            Files.walkFileTree(contentRoot, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().toLowerCase().endsWith(".md")) {
                        String relativePath = "/" + contentRoot.relativize(file).toString().replace("\\", "/");
                        markdownFiles.add(relativePath);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to find markdown files", e);
        }
        return markdownFiles;
    }

    private String getFileName(Path path) {
        return path.getFileName() != null ? path.getFileName().toString() : "";
    }
    
    @Override
    public String getRelativePath(Path path) {
        // Convert absolute path to relative path from contentRoot
        Path relativePath = contentRoot.relativize(path.normalize().toAbsolutePath());
        // Replace backslashes with forward slashes for consistency
        return relativePath.toString().replace("\\", "/");
    }
}
