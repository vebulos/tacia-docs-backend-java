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
import java.util.stream.Stream;

@Repository
public class FileSystemContentRepository implements ContentRepository {
    private static final Logger log = LoggerFactory.getLogger(FileSystemContentRepository.class);
    private static final Set<String> HIDDEN_DIRS = Set.of(".git", ".idea", "node_modules");
    private static final String METADATA_FILE = ".metadata.json";

    private final Path contentRoot;

    public FileSystemContentRepository(Path contentRoot) {
        this.contentRoot = contentRoot.toAbsolutePath().normalize();
        ensureContentRootExists();
    }

    @Override
    public Optional<ContentItem> findByPath(String path) {
        Path filePath = resolvePath(path);
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }
        return Optional.ofNullable(createContentItem(filePath));
    }

    @Override
    public List<ContentItem> findChildren(String path) {
        try {
            Path dirPath = resolvePath(path);
            if (!Files.isDirectory(dirPath)) {
                return List.of();
            }

            try (Stream<Path> paths = Files.list(dirPath)) {
                return paths
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .filter(p -> !HIDDEN_DIRS.contains(p.getFileName().toString()))
                    .filter(p -> !p.getFileName().toString().equals(METADATA_FILE))
                    .map(this::createContentItem)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            }
        } catch (IOException e) {
            log.error("Error finding children of path: " + path, e);
            return List.of();
        }
    }

    @Override
    public ContentItem save(ContentItem item, String content) {
        try {
            Path filePath = resolvePath(item.path());
            Path parentDir = filePath.getParent();

            if (parentDir != null) {
                Files.createDirectories(parentDir);
                // Save metadata in the parent directory
                saveMetadata(parentDir, item.metadata());
            }

            // Save the actual content
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            
            // Update file metadata
            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
            return ContentItem.file(
                item.name(),
                item.path(),
                item.mimeType(),
                content.getBytes(StandardCharsets.UTF_8).length,
                attrs.lastModifiedTime().toInstant(),
                item.metadata()
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to save content: " + item.path(), e);
        }
    }
    
    private void saveMetadata(Path directory, Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return;
        }
        
        try {
            // TODO: Implement JSON serialization of metadata
            // For now, we'll just store a simple property file
            Path metadataFile = directory.resolve(METADATA_FILE);
            Properties props = new Properties();
            metadata.forEach((key, value) -> props.setProperty(key, String.valueOf(value)));
            
            try (OutputStream os = Files.newOutputStream(metadataFile)) {
                props.store(os, "Content metadata");
            }
        } catch (IOException e) {
            log.warn("Failed to save metadata for directory: " + directory, e);
        }
    }

    @Override
    public boolean delete(String path) {
        try {
            Path filePath = resolvePath(path);
            if (!Files.exists(filePath)) {
                return false;
            }
            
            if (Files.isDirectory(filePath)) {
                // Delete directory recursively
                Files.walkFileTree(filePath, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                Files.delete(filePath);
            }
            return true;
        } catch (IOException e) {
            log.error("Error deleting path: " + path, e);
            return false;
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
        Path filePath = resolvePath(path);
        if (!Files.exists(filePath)) {
            throw new ContentNotFoundException(path);
        }
        if (Files.isDirectory(filePath)) {
            throw new IOException("Cannot read content of a directory: " + path);
        }
        return Files.readString(filePath, StandardCharsets.UTF_8);
    }

    private Path resolvePath(String path) {
        Path resolved = contentRoot.resolve(path.startsWith("/") ? path.substring(1) : path);
        // Security check: prevent directory traversal
        if (!resolved.normalize().startsWith(contentRoot)) {
            throw new SecurityException("Access to requested path is not allowed: " + path);
        }
        return resolved;
    }

    private ContentItem createContentItem(Path path) {
        try {
            if (Files.isHidden(path) || path.getFileName().toString().startsWith(".")) {
                return null;
            }

            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            String relativePath = contentRoot.relativize(path).toString().replace("\\", "/");
            String itemPath = "/" + relativePath;

            if (Files.isDirectory(path)) {
                if (HIDDEN_DIRS.contains(path.getFileName().toString())) {
                    return null;
                }
                return ContentItem.directory(
                    path.getFileName().toString(),
                    itemPath,
                    attrs.lastModifiedTime().toInstant(),
                    loadMetadata(path)
                );
            } else {
                if (path.getFileName().toString().equals(METADATA_FILE)) {
                    return null;
                }
                
                String mimeType = Files.probeContentType(path);
                return ContentItem.file(
                    path.getFileName().toString(),
                    itemPath,
                    mimeType != null ? mimeType : "application/octet-stream",
                    attrs.size(),
                    attrs.lastModifiedTime().toInstant(),
                    loadMetadata(path.getParent())
                );
            }
        } catch (IOException e) {
            log.error("Error creating content item for path: " + path, e);
            return null;
        }
    }

    private Map<String, Object> loadMetadata(Path directory) {
        Path metadataFile = directory.resolve(METADATA_FILE);
        if (!Files.exists(metadataFile)) {
            return Map.of();
        }
        try (InputStream is = Files.newInputStream(metadataFile)) {
            Properties props = new Properties();
            props.load(is);
            
            Map<String, Object> metadata = new HashMap<>();
            props.forEach((key, value) -> 
                metadata.put(key.toString(), value)
            );
            
            return metadata;
        } catch (Exception e) {
            log.warn("Failed to load metadata from: " + metadataFile, e);
            return Map.of();
        }
    }

    private void ensureContentRootExists() {
        try {
            if (!Files.exists(contentRoot)) {
                Files.createDirectories(contentRoot);
                log.info("Created content root directory: {}", contentRoot);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create content root directory: " + contentRoot, e);
        }
    }
}
