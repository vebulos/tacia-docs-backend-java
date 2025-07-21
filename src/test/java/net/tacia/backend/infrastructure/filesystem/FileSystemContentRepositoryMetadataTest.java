package net.tacia.backend.infrastructure.filesystem;

import net.tacia.backend.model.ContentItem;
import net.tacia.backend.repository.FileSystemContentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemContentRepositoryMetadataTest {

    @TempDir
    Path tempDir;
    private FileSystemContentRepository repository;
    private Path contentRoot;

    @BeforeEach
    void setUp() {
        contentRoot = tempDir.resolve("content");
        repository = new FileSystemContentRepository(contentRoot);
    }

    @Test
    void shouldLoadDirectoryMetadata() throws IOException {
        // Given
        Path dir = Files.createDirectories(contentRoot.resolve("test-dir"));
        String metadataContent = "order: 2\ntitle: Test Directory\ntags: [test, example]";
        Files.write(dir.resolve(".metadata"), metadataContent.getBytes(StandardCharsets.UTF_8));

        // When
        Optional<ContentItem> result = repository.findByPath("/test-dir");

        // Then
        assertTrue(result.isPresent());
        ContentItem item = result.get();
        assertEquals("test-dir", item.name());
        assertEquals("directory", item.type());
        assertEquals(2, item.order());
        
        Map<String, Object> metadata = item.metadata();
        assertNotNull(metadata);
        assertEquals("Test Directory", metadata.get("title"));
        assertTrue(metadata.get("tags") instanceof List);
        assertTrue(((List<?>) metadata.get("tags")).contains("test"));
    }

    @Test
    void shouldLoadMarkdownFrontmatter() throws IOException {
        // Given
        Path file = contentRoot.resolve("test.md");
        String markdownContent = "---\norder: 1\ntitle: Test Markdown\n---\n# Content\nThis is a test";
        Files.write(file, markdownContent.getBytes(StandardCharsets.UTF_8));

        // When
        Optional<ContentItem> result = repository.findByPath("/test.md");

        // Then
        assertTrue(result.isPresent());
        ContentItem item = result.get();
        assertEquals("test.md", item.name());
        assertEquals("file", item.type());
        assertEquals(1, item.order());
        
        Map<String, Object> metadata = item.metadata();
        assertNotNull(metadata);
        assertEquals("Test Markdown", metadata.get("title"));
    }

    @Test
    void shouldSortChildrenByOrderThenTypeThenName() throws IOException {
        // Given
        // Create directories with metadata
        createDirectoryWithMetadata("dir1", 2, "Dir 1");
        createDirectoryWithMetadata("dir2", 1, "Dir 2");
        createDirectoryWithMetadata("dir3", null, "Dir 3");
        
        // Create files with metadata
        createMarkdownFile("file1.md", 4, "File 1");
        createMarkdownFile("file2.md", 3, "File 2");
        createMarkdownFile("file3.md", null, "File 3");

        // When
        List<ContentItem> children = repository.findChildren("/");

        // Then
        assertEquals(6, children.size(), "Should have 6 items (3 directories + 3 files)");
        
        // Convert to list of names for easier debugging
        List<String> childNames = children.stream().map(ContentItem::name).collect(Collectors.toList());
        System.out.println("Children in order: " + childNames);
        
        // First should be dir2 (order=1)
        assertEquals("dir2", children.get(0).name(), "First item should be dir2 (order=1)");
        assertEquals(1, children.get(0).order());
        
        // Then dir1 (order=2)
        assertEquals("dir1", children.get(1).name(), "Second item should be dir1 (order=2)");
        assertEquals(2, children.get(1).order());
        
        // Then file2 (order=3) - Files with order come before directories without order
        assertEquals("file2.md", children.get(2).name(), "Third item should be file2.md (order=3)");
        assertEquals(3, children.get(2).order());
        
        // Then file1 (order=4)
        assertEquals("file1.md", children.get(3).name(), "Fourth item should be file1.md (order=4)");
        assertEquals(4, children.get(3).order());
        
        // Then dir3 (no order) - Directories without order come after files with order
        assertEquals("dir3", children.get(4).name(), "Fifth item should be dir3 (no order)");
        assertNull(children.get(4).order());
        
        // Then file3 (no order)
        assertEquals("file3.md", children.get(5).name(), "Sixth item should be file3.md (no order)");
        assertNull(children.get(5).order());
    }

    private void createDirectoryWithMetadata(String name, Integer order, String title) throws IOException {
        Path dir = Files.createDirectory(contentRoot.resolve(name));
        StringBuilder metadata = new StringBuilder();
        if (order != null) {
            metadata.append("order: ").append(order).append("\n");
        }
        metadata.append("title: ").append(title).append("\n");
        Files.write(dir.resolve(".metadata"), metadata.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void createMarkdownFile(String name, Integer order, String title) throws IOException {
        Path file = contentRoot.resolve(name);
        String content = "---\n";
        if (order != null) {
            content += "order: " + order + "\n";
        }
        content += "title: " + title + "\n";
        content += "---\n# " + title;
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }
}
