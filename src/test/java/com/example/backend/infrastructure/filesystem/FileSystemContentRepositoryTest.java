package com.example.backend.infrastructure.filesystem;

import com.example.backend.model.ContentItem;
import com.example.backend.repository.ContentRepository;
import com.example.backend.repository.FileSystemContentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemContentRepositoryTest {

    @TempDir
    Path tempDir;
    private ContentRepository repository;
    private Path contentRoot;

    @BeforeEach
    void setUp() {
        contentRoot = tempDir.resolve("content");
        repository = new FileSystemContentRepository(contentRoot);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up test files
        if (Files.exists(contentRoot)) {
            Files.walk(contentRoot)
                 .sorted((a, b) -> -a.compareTo(b)) // reverse order for directories last
                 .forEach(path -> {
                     try {
                         Files.deleteIfExists(path);
                     } catch (IOException e) {
                         throw new RuntimeException("Failed to delete test file: " + path, e);
                     }
                 });
        }
    }

    @Test
    void shouldSaveAndRetrieveFile() {
        // Given
        Instant now = Instant.now();
        ContentItem item = ContentItem.file(
            "test.txt",
            "/test.txt",
            0,
            now
        );
        String content = "Test content";

        // When
        ContentItem savedItem = repository.save(item, content);
        Optional<ContentItem> foundItem = repository.findByPath("/test.txt");

        // Then
        assertTrue(foundItem.isPresent());
        assertEquals(item.name(), foundItem.get().name());
        assertEquals(item.path(), foundItem.get().path());
    }

    @Test
    void shouldListDirectoryContents() {
        // Given
        Instant now = Instant.now();
        ContentItem file1 = ContentItem.file(
            "file1.txt",
            "/dir1/file1.txt",
            0,
            now
        );
        ContentItem file2 = ContentItem.file(
            "file2.txt",
            "/dir1/file2.txt",
            0,
            now
        );

        // When
        repository.save(file1, "content1");
        repository.save(file2, "content2");
        List<ContentItem> children = repository.findChildren("/dir1/");

        // Then
        assertEquals(2, children.size());
        assertTrue(children.stream().anyMatch(i -> i.name().equals("file1.txt")));
        assertTrue(children.stream().anyMatch(i -> i.name().equals("file2.txt")));
    }

    @Test
    void shouldDeleteFile() {
        // Given
        Instant now = Instant.now();
        ContentItem item = ContentItem.file(
            "toDelete.txt",
            "/toDelete.txt",
            0,
            now
        );
        repository.save(item, "content");
        assertTrue(repository.exists("/toDelete.txt"));

        // When
        boolean deleted = repository.delete("/toDelete.txt");

        // Then
        assertTrue(deleted);
        assertFalse(repository.exists("/toDelete.txt"));
    }
}
