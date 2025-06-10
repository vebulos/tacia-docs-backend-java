package com.example.backend.api.controller;

import com.example.backend.api.dto.ContentItemDto;
import com.example.backend.domain.model.ContentItem;
import com.example.backend.domain.repository.ContentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;
import com.example.backend.api.controller.ContentController;
import com.example.backend.api.exception.GlobalExceptionHandler;
import com.example.backend.config.TestConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContentController.class)
@ContextConfiguration(classes = {ContentController.class, TestConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = true)
class ContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContentRepository contentRepository;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(contentRepository);
    }
    
    @Test
    void listRoot_shouldReturnRootContent() throws Exception {
        // Given
        ContentItem file1 = ContentItem.file("file1.txt", "/file1.txt", "text/plain", 100, Instant.now(), Map.of("title", "File 1"));
        ContentItem dir1 = ContentItem.directory("dir1", "/dir1", Instant.now(), Map.of("title", "Directory 1"));
        
        when(contentRepository.findChildren("/")).thenReturn(List.of(file1, dir1));
        
        // When/Then
        mockMvc.perform(get("/api/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("file1.txt")))
                .andExpect(jsonPath("$[0].type", is("file")))
                .andExpect(jsonPath("$[1].name", is("dir1")))
                .andExpect(jsonPath("$[1].type", is("directory")));
    }
    
    @Test
    void getContent_shouldReturnFileContent() throws Exception {
        // Given
        ContentItem file = ContentItem.file("test.txt", "/test.txt", "text/plain", 12, Instant.now(), Map.of("title", "Test File"));
        when(contentRepository.findByPath("/test.txt")).thenReturn(Optional.of(file));
        when(contentRepository.readContent("/test.txt")).thenReturn("Test content");
        
        // When/Then
        mockMvc.perform(get("/api/content/item")
                .param("path", "/test.txt")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("test.txt")))
                .andExpect(jsonPath("$.content", is("Test content")));
    }
    
    @Test
    void saveContent_shouldSaveFile() throws Exception {
        // Given
        String content = "Test content";
        String path = "/test.txt";
        ContentItem savedItem = ContentItem.file(
            "test.txt", 
            path, 
            "text/plain", 
            content.length(), 
            Instant.now(), 
            Map.of()
        );
        
        when(contentRepository.save(any(ContentItem.class), eq(content))).thenReturn(savedItem);
        
        // When/Then
        mockMvc.perform(post("/api/content")
                .param("path", path)
                .contentType(MediaType.TEXT_PLAIN)
                .content(content))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("test.txt")));
                
        verify(contentRepository).save(any(ContentItem.class), eq(content));
    }
    
    @Test
    void deleteContent_shouldDeleteFile() throws Exception {
        // Given
        when(contentRepository.exists("/file.txt")).thenReturn(true);
        when(contentRepository.delete("/file.txt")).thenReturn(true);
        
        // When/Then
        mockMvc.perform(delete("/api/content")
                .param("path", "/file.txt"))
                .andExpect(status().isNoContent());
                
        verify(contentRepository).delete("/file.txt");
    }
    
    @Test
    void deleteContent_nonExistentFile_shouldReturn404() throws Exception {
        // Given
        String path = "/nonexistent.txt";
        when(contentRepository.exists(path)).thenReturn(false);
        
        // When/Then
        mockMvc.perform(delete("/api/content")
                .param("path", path)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("not found")));
                
        verify(contentRepository, never()).delete(anyString());
    }
}
