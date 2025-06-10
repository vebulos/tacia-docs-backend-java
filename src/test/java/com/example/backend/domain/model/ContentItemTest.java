package com.example.backend.domain.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ContentItemTest {

    @Test
    void createDirectory() {
        Instant now = Instant.now();
        ContentItem dir = ContentItem.directory(
            "docs",
            "/docs",
            now,
            Map.of("title", "Documentation")
        );

        assertEquals("docs", dir.name());
        assertEquals("/docs", dir.path());
        assertEquals("directory", dir.type());
        assertNull(dir.mimeType());
        assertEquals(0, dir.size());
        assertEquals(now, dir.lastModified());
        assertEquals("Documentation", dir.metadata().get("title"));
    }

    @Test
    void createFile() {
        Instant now = Instant.now();
        ContentItem file = ContentItem.file(
            "readme.md",
            "/readme.md",
            "text/markdown",
            1024,
            now,
            Map.of("title", "Read Me")
        );

        assertEquals("readme.md", file.name());
        assertEquals("/readme.md", file.path());
        assertEquals("file", file.type());
        assertEquals("text/markdown", file.mimeType());
        assertEquals(1024, file.size());
        assertEquals(now, file.lastModified());
        assertEquals("Read Me", file.metadata().get("title"));
    }
}
