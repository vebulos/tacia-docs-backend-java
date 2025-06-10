package com.example.backend;

import com.example.backend.domain.repository.ContentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
class MxcBackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContentRepository contentRepository;

    @Test
    void contextLoads() {
        // Test that the application context loads
    }

    @Test
    void shouldReturnOkWhenAccessingRootEndpoint() throws Exception {
        // Mock the repository to return empty list for root
        when(contentRepository.findChildren("/")).thenReturn(Collections.emptyList());
        
        mockMvc.perform(get("/api/content"))
               .andExpect(status().isOk())
               .andDo(result -> System.out.println("Response: " + result.getResponse().getContentAsString()));
    }
}
