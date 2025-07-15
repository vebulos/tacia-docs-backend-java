package net.tacia.backend.domain.model;

import net.tacia.backend.model.ContentItem;
import org.junit.jupiter.api.Test;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ContentItemTest {

    @Test
    void createDirectory() {
        Instant now = Instant.now();
        ContentItem dir = ContentItem.directory(
            "docs",
            "/docs",
            now
        );

        assertEquals("docs", dir.name());
        assertEquals("/docs", dir.path());  // Should NOT end with slash for directories
        assertEquals("directory", dir.type());
        assertEquals(0, dir.size());
        assertEquals(now, dir.lastModified());
    }

    @Test
    void createFile() {
        Instant now = Instant.now();
        ContentItem file = ContentItem.file(
            "readme.md",
            "/readme.md",
            1024,
            now
        );

        assertEquals("readme.md", file.name());
        assertEquals("/readme.md", file.path());
        assertEquals("file", file.type());
        assertEquals(1024, file.size());
        assertEquals(now, file.lastModified());
    }
    
    @Test
    void directoryPath_shouldBePreservedAsIs() {
        // A path without a trailing slash should be preserved.
        ContentItem dir1 = ContentItem.directory("test", "/test", Instant.now());
        assertEquals("/test", dir1.path());
        
        // A path with a trailing slash should also be preserved.
        ContentItem dir2 = ContentItem.directory("test", "/test/", Instant.now());
        assertEquals("/test/", dir2.path());
    }
}
