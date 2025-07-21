package net.tacia.backend;

import net.tacia.backend.repository.ContentRepository;
import net.tacia.backend.model.ContentItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(net.tacia.backend.api.controller.StructureController.class)
class MxcBackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContentRepository contentRepository;

    @Test
    void contextLoads() {
        // Test that the application context loads
    }

    @Test
    void shouldReturnOkWhenAccessingRootStructure() throws Exception {
        // Mock the repository to return empty list for root structure
        when(contentRepository.findByPath("/")).thenReturn(java.util.Optional.of(new ContentItem("", "directory", "/", 0, Instant.now(), null, Map.of())));
        when(contentRepository.findChildren("/")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/structure"))
               .andExpect(status().isOk())
               .andDo(result -> System.out.println("Root Structure Response: " + result.getResponse().getContentAsString()));
    }

    @Test
    void shouldReturnOkWhenAccessingSubdirectoryStructure() throws Exception {
        // Mock the repository to return a directory and its children for a subdirectory
        String subdirPath = "/docs";
        when(contentRepository.findByPath(subdirPath)).thenReturn(java.util.Optional.of(new ContentItem("docs", "directory", subdirPath, 0, Instant.now(), null, Map.of())));
        when(contentRepository.findChildren(subdirPath)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/structure/docs"))
               .andExpect(status().isOk())
               .andDo(result -> System.out.println("Subdirectory Structure Response: " + result.getResponse().getContentAsString()));
    }
}
