package com.example.backend.infrastructure.filesystem;

import com.example.backend.domain.model.ContentItem;
import com.example.backend.domain.repository.ContentRepository;
import com.example.backend.api.exception.ContentNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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
            if (type.equals("directory") && !normalizedPath.endsWith("/")) {
                normalizedPath += "/";
            }

            return Optional.of(new ContentItem(name, type, normalizedPath, size, lastModified));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file attributes: " + path, e);
        }
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

    private String getFileName(Path path) {
        return path.getFileName() != null ? path.getFileName().toString() : "";
    }
}
