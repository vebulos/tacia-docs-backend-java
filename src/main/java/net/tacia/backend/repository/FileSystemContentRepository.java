package net.tacia.backend.repository;

import net.tacia.backend.model.ContentItem;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.*;
import java.nio.file.StandardOpenOption;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.HashMap;

public class FileSystemContentRepository implements ContentRepository {
    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile("^---\\s*\\n([\\s\\S]*?)\\n---");
    private static final Set<String> MARKDOWN_EXTENSIONS = Set.of(".md", ".markdown");
    private final Yaml yaml = new Yaml();
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
            
            // Create basic content item with empty metadata
            ContentItem item = new ContentItem(
                getFileName(fullPath),
                Files.isDirectory(fullPath) ? "directory" : "file",
                "/" + getRelativePath(fullPath) + (Files.isDirectory(fullPath) ? "/" : ""),
                attrs.size(),
                attrs.lastModifiedTime().toInstant(),
                null,  // order will be set by load*Metadata
                new HashMap<>()  // empty metadata
            );
            
            // Load metadata if available
            if (Files.isDirectory(fullPath)) {
                item = loadDirectoryMetadata(fullPath, item);
            } else if (isMarkdownFile(fullPath.getFileName().toString())) {
                item = loadMarkdownMetadata(fullPath, item);
            }
            
            return Optional.of(item);
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

                        descendants.add(new ContentItem(name, type, relativePath, size, lastModified, null, new HashMap<>()));
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

                            descendants.add(new ContentItem(name, type, relativePath, size, lastModified, null, new HashMap<>()));
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

        // Normalize the parent path - ensure it doesn't end with a slash
        String parentPath = path;
        if (parentPath.endsWith("/")) {
            parentPath = parentPath.substring(0, parentPath.length() - 1);
        }
        if (parentPath.isEmpty()) {
            parentPath = "/";
        } else if (!parentPath.startsWith("/")) {
            parentPath = "/" + parentPath;
        }

        try (var stream = Files.list(dirPath)) {
            for (Path childPath : stream.collect(Collectors.toList())) {
                try {
                    String name = childPath.getFileName().toString();
                    boolean isDirectory = Files.isDirectory(childPath);
                    
                    // Skip hidden files except .metadata
                    if (name.startsWith(".") && !name.equals(".metadata")) {
                        continue;
                    }
                    
                    // Skip non-markdown files
                    if (!isDirectory && !isMarkdownFile(name)) {
                        continue;
                    }
                    
                    BasicFileAttributes attrs = Files.readAttributes(childPath, BasicFileAttributes.class);
                    
                    // Build the full path for the child
                    String childPathStr = parentPath.equals("/") 
                        ? "/" + name 
                        : parentPath + "/" + name;
                    
                    // Ensure directory paths end with a slash
                    if (isDirectory && !childPathStr.endsWith("/")) {
                        childPathStr += "/";
                    }
                    
                    // Create base content item with empty metadata
                    ContentItem item = new ContentItem(
                        name,
                        isDirectory ? "directory" : "file",
                        childPathStr,
                        attrs.size(),
                        attrs.lastModifiedTime().toInstant(),
                        null,  // order will be set by load*Metadata
                        new HashMap<>()  // empty metadata
                    );
                    
                    // Load metadata if available
                    if (isDirectory) {
                        item = loadDirectoryMetadata(childPath, item);
                    } else if (isMarkdownFile(name)) {
                        item = loadMarkdownMetadata(childPath, item);
                    }
                    
                    // Skip .metadata files from the result
                    if (!name.equals(".metadata")) {
                        children.add(item);
                    }
                } catch (IOException e) {
                    // Skip files we can't read
                    continue;
                }
            }
            
            // Sort items by order (nulls last), then by type (directories first), then by name
            children.sort((a, b) -> {
                // First compare by order (nulls last)
                if (a.order() != null && b.order() != null) {
                    int orderCompare = Integer.compare(a.order(), b.order());
                    if (orderCompare != 0) return orderCompare;
                } else if (a.order() != null) {
                    return -1;
                } else if (b.order() != null) {
                    return 1;
                }
                
                // Then by type (directories first)
                if (!a.type().equals(b.type())) {
                    return a.type().equals("directory") ? -1 : 1;
                }
                
                // Finally by name (case insensitive)
                return a.name().compareToIgnoreCase(b.name());
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
                attrs.lastModifiedTime().toInstant(),
                item.order(),
                item.metadata() != null ? new HashMap<>(item.metadata()) : new HashMap<>()
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
        
        return new ContentItem(
            name, 
            type, 
            "/" + contentRoot.relativize(fullPath).toString().replace("\\", "/"), 
            size, 
            lastModified,
            null,  // order not available in this context
            new HashMap<>()  // empty metadata
        );
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
    
    /**
     * Checks if a file is a markdown file based on its extension.
     */
    private boolean isMarkdownFile(String filename) {
        if (filename == null) {
            return false;
        }
        String lowerName = filename.toLowerCase();
        return MARKDOWN_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
    }
    
    /**
     * Loads metadata from a directory's .metadata file if it exists.
     */
    private ContentItem loadDirectoryMetadata(Path dirPath, ContentItem item) {
        Path metadataPath = dirPath.resolve(".metadata");
        if (!Files.exists(metadataPath)) {
            return item;
        }
        
        try {
            String content = Files.readString(metadataPath);
            Map<String, Object> metadata = parseMetadata(content);
            return applyMetadataToItem(item, metadata);
        } catch (IOException e) {
            // If we can't read the metadata file, just return the original item
            return item;
        }
    }
    
    /**
     * Loads metadata from a markdown file's frontmatter.
     */
    private ContentItem loadMarkdownMetadata(Path filePath, ContentItem item) {
        try {
            String content = Files.readString(filePath);
            var matcher = FRONTMATTER_PATTERN.matcher(content);
            
            if (matcher.find()) {
                String yamlContent = matcher.group(1);
                Map<String, Object> metadata = parseMetadata(yamlContent);
                return applyMetadataToItem(item, metadata);
            }
            return item;
        } catch (IOException e) {
            // If we can't read the file, just return the original item
            return item;
        }
    }
    
    /**
     * Parses YAML metadata content into a Map.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMetadata(String content) {
        try {
            // Try to parse as YAML first
            Object parsed = yaml.load(content);
            if (parsed instanceof Map) {
                return (Map<String, Object>) parsed;
            }
            
            // Fallback to simple key:value parsing if YAML parsing fails
            return parseSimpleKeyValue(content);
        } catch (Exception e) {
            // If YAML parsing fails, fall back to simple key:value parsing
            return parseSimpleKeyValue(content);
        }
    }
    
    /**
     * Simple key:value parser as a fallback when YAML parsing fails.
     */
    private Map<String, Object> parseSimpleKeyValue(String content) {
        Map<String, Object> result = new HashMap<>();
        if (content == null || content.trim().isEmpty()) {
            return result;
        }
        
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            // Match key: value pattern
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                
                // Remove surrounding quotes if present
                if ((value.startsWith("\"") && value.endsWith("\"")) || 
                    (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                
                // Try to parse values appropriately
                Object parsedValue = value;
                if ("true".equalsIgnoreCase(value)) {
                    parsedValue = true;
                } else if ("false".equalsIgnoreCase(value)) {
                    parsedValue = false;
                } else if ("null".equalsIgnoreCase(value) || value.isEmpty()) {
                    parsedValue = null;
                } else {
                    try {
                        // Try to parse as number
                        if (value.contains(".")) {
                            parsedValue = Double.parseDouble(value);
                        } else {
                            parsedValue = Long.parseLong(value);
                        }
                    } catch (NumberFormatException e) {
                        // Not a number, keep as string
                    }
                }
                
                result.put(key, parsedValue);
            }
        }
        
        return result;
    }
    
    /**
     * Applies metadata to a content item, setting order and other metadata fields.
     */
    private ContentItem applyMetadataToItem(ContentItem item, Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return item;
        }
        
        // Extract order if present
        Integer order = null;
        if (metadata.containsKey("order")) {
            Object orderObj = metadata.get("order");
            if (orderObj instanceof Number) {
                order = ((Number) orderObj).intValue();
            } else if (orderObj instanceof String) {
                try {
                    order = Integer.parseInt((String) orderObj);
                } catch (NumberFormatException e) {
                    // Ignore invalid order values
                }
            }
        }
        
        // Create a copy of metadata without special fields
        Map<String, Object> filteredMetadata = new HashMap<>(metadata);
        filteredMetadata.remove("order");
        
        // Apply updates
        ContentItem updatedItem = item;
        if (order != null) {
            updatedItem = updatedItem.withOrder(order);
        }
        if (!filteredMetadata.isEmpty()) {
            updatedItem = updatedItem.withMetadata(filteredMetadata);
        }
        
        return updatedItem;
    }
}
