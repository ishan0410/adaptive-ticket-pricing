package com.adaptiveticket.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventControllerTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/events → 200 with paginated list (public, no auth required)")
    void listEventsIsPublic() throws Exception {
        mockMvc.perform(get("/api/events")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /api/events/{id} → 200 with event detail including tiers")
    void getEventDetailReturnsTiers() throws Exception {
        // Assumes seed data has event id=1
        mockMvc.perform(get("/api/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").isNotEmpty())
                .andExpect(jsonPath("$.tiers").isArray())
                .andExpect(jsonPath("$.tiers[0].currentPrice").isNumber());
    }

    @Test
    @DisplayName("GET /api/events/99999 → 404 for nonexistent event")
    void getEventReturns404ForMissing() throws Exception {
        mockMvc.perform(get("/api/events/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/events → 401 without authentication")
    void createEventRequiresAuth() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Unauthorized Event\"}"))
                .andExpect(status().isUnauthorized());
    }
}
