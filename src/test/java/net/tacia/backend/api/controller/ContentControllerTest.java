package net.tacia.backend.api.controller;

import net.tacia.backend.model.ContentItem;
import net.tacia.backend.repository.ContentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;
import net.tacia.backend.api.exception.GlobalExceptionHandler;
import net.tacia.backend.config.TestConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContentController.class)
@ContextConfiguration(classes = {ContentController.class, TestConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = true)
class ContentControllerTest {
    private static final Logger logger = LoggerFactory.getLogger(ContentControllerTest.class);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContentRepository contentRepository;

    @MockitoBean
    private net.tacia.backend.service.MarkdownService markdownService;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(contentRepository, markdownService);
        // Default stub for MarkdownService
        when(markdownService.processMarkdown(anyString())).thenReturn(new HashMap<>());
    }

    /**
     * Extracts and decodes the path from the request URL
     * Handles URL-encoded characters like %20 for spaces
     */
    private String extractPathFromRequest(String requestURI) {
        try {
            // Remove context path if present
            String contextPath = "/api";
            if (requestURI.startsWith(contextPath)) {
                requestURI = requestURI.substring(contextPath.length());
            }
            
            // Find the /content/ part
            int contentIndex = requestURI.indexOf("/content/");
            if (contentIndex != -1) {
                String rawPath = requestURI.substring(contentIndex + 9); // 9 is the length of "/content/"
                // URL decode the path to handle spaces (%20) and other encoded characters
                String decodedPath = java.net.URLDecoder.decode(rawPath, StandardCharsets.UTF_8);
                logger.debug("Extracted path - raw: '{}', decoded: '{}'", rawPath, decodedPath);
                
                // Normalize and remove leading slash to match JS backend
                String normalized = normalizePath(decodedPath);
                if (normalized.startsWith("/")) {
                    normalized = normalized.substring(1);
                }
                return normalized;
            }
            return "";
        } catch (Exception e) {
            logger.error("Error extracting path from request", e);
            return "";
        }
    }

    /**
     * Normalizes a path to a consistent format
     * - Removes duplicate slashes
     * - Removes leading and trailing slashes
     * - Handles null or empty paths
     */
    private String normalizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "";
        }
        
        // Trim and normalize slashes
        String normalized = path.trim()
            .replaceAll("/+", "/")  // Replace multiple slashes with a single slash
            .replaceAll("^/|/$", ""); // Remove leading and trailing slashes
        
        return normalized;
    }
    
    @Test
    void listRoot_shouldReturnRootContent() throws Exception {
        // Given
        Instant now = Instant.now();
        ContentItem file1 = ContentItem.file("file1.txt", "/file1.txt", 100, now);
        ContentItem dir1 = ContentItem.directory("dir1", "/dir1", now);
        
        // For the root, the controller looks for a ContentItem with path ""
        // and then its children with findChildren("").
        when(contentRepository.findByPath("")).thenReturn(Optional.of(ContentItem.directory("", "", now)));
        // Sort results for a predictable order.
        when(contentRepository.findChildren("")).thenReturn(List.of(dir1, file1));

        // When/Then
        mockMvc.perform(get("/api/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].name", is("dir1")))
                .andExpect(jsonPath("$.items[0].type", is("directory")))
                .andExpect(jsonPath("$.items[1].name", is("file1.txt")))
                .andExpect(jsonPath("$.items[1].type", is("file")));
    }
    
    @Test
    void getContent_shouldReturnFileContent() throws Exception {
        // Given
        Instant now = Instant.now();
        ContentItem file = ContentItem.file("test.txt", "/test.txt", 12, now);
        String lookupPath = "test.txt";
        when(contentRepository.findByPath(lookupPath)).thenReturn(Optional.of(file));
        when(contentRepository.getContent(lookupPath)).thenReturn(Optional.of("Test content"));

        // When/Then
        mockMvc.perform(get("/api/content/test.txt"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Test content"));
    }
    
    @Test
    void getContent_shouldReturnDirectoryContentRecursively() throws Exception {
        // Given
        Instant now = Instant.now();
        // Structure: parent (dir) -> file1.txt, child (dir) -> file2.txt
        ContentItem parentDir = ContentItem.directory("parent", "/parent", now);
        ContentItem file1 = ContentItem.file("file1.txt", "/parent/file1.txt", 10, now);
        ContentItem childDir = ContentItem.directory("child", "/parent/child", now);
        ContentItem file2 = ContentItem.file("file2.txt", "/parent/child/file2.txt", 20, now);
        
        String parentLookupPath = "parent";
        when(contentRepository.findByPath(parentLookupPath)).thenReturn(Optional.of(parentDir));
        when(contentRepository.findDescendants(parentLookupPath)).thenReturn(List.of(childDir, file1, file2));

        // When/Then
        mockMvc.perform(get("/api/content/parent?recursive=true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path", is("parent")))
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[0].name", is("child")))
                .andExpect(jsonPath("$.items[1].name", is("file1.txt")))
                .andExpect(jsonPath("$.items[2].name", is("file2.txt")));
    }
    
    @Test
    void listContent_shouldNormalizePaths() throws Exception {
        // Given
        String[] testPaths = {
            "dir1",              // relative path
            "dir1/",             // path with trailing slash
            "dir1/subdir",        // nested path
            "dir1/subdir/"      // nested path with trailing slash
        };
        Instant now = Instant.now();
        ContentItem dir = ContentItem.directory("dir1", "/dir1", now);
        ContentItem file = ContentItem.file("file.txt", "/dir1/file.txt", 10, now);
        when(contentRepository.findByPath(anyString())).thenReturn(Optional.of(dir));
        when(contentRepository.findChildren(anyString())).thenReturn(List.of(file));

        for (String testPath : testPaths) {
            String url = "/api/content/" + testPath.replaceAll("^/|/$", "");
            mockMvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray());
        }
    }
    
    @Test
    void saveContent_shouldSaveFile() throws Exception {
        // Given
        String path = "/new/file.txt";
        String content = "This is the new content.";
        String normalizedPath = "new/file.txt";
        String parentPath = "new";

        ContentItem savedItem = ContentItem.file("file.txt", normalizedPath, content.length(), Instant.now());

        // Mock the parent directory check
        when(contentRepository.findByPath(parentPath)).thenReturn(Optional.of(ContentItem.directory(parentPath, parentPath, Instant.now())));
        // Mock the save operation
        when(contentRepository.saveContent(normalizedPath, content)).thenReturn(savedItem);

        // When/Then
        mockMvc.perform(post("/api/content" + path)
                .contentType(MediaType.TEXT_PLAIN)
                .content(content))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("file.txt")))
                .andExpect(jsonPath("$.path", is(normalizedPath)))
                .andExpect(jsonPath("$.type", is("file")));

        // Verify that the save method was called with the correct arguments.
        verify(contentRepository).saveContent(normalizedPath, content);
    }
    
    @Test
    void deleteContent_shouldDeleteFile() throws Exception {
        // Given
        String path = "test.txt";
        when(contentRepository.exists(path)).thenReturn(true);

        // When/Then
        mockMvc.perform(delete("/api/content/test.txt"))
                .andExpect(status().isNoContent());
    }
    
    @Test
    void deleteContent_nonExistentFile_shouldReturn404() throws Exception {
        // Given
        String path = "nonexistent.txt";
        when(contentRepository.exists(path)).thenReturn(false);
        
        // When/Then
        mockMvc.perform(delete("/api/content/nonexistent.txt")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("not found")));
                
        verify(contentRepository, never()).delete(anyString());
    }
}
