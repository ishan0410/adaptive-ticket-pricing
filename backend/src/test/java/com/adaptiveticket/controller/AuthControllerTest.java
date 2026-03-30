package com.adaptiveticket.controller;

import com.adaptiveticket.dto.request.LoginRequest;
import com.adaptiveticket.dto.request.RegisterRequest;
import com.adaptiveticket.entity.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/auth/register → 201 with JWT token")
    void registerReturnsTokenAndUserInfo() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("New User")
                .email("newuser" + System.currentTimeMillis() + "@test.com")
                .password("password123")
                .role(Role.BUYER)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.name").value("New User"))
                .andExpect(jsonPath("$.role").value("BUYER"));
    }

    @Test
    @DisplayName("POST /api/auth/register → 409 for duplicate email")
    void registerRejectsDuplicateEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("First").email("dup@test.com").password("password123").build();

        // First registration succeeds
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second registration with same email fails
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/auth/register → 400 for invalid input")
    void registerRejectsInvalidInput() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("").email("not-an-email").password("12").build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/auth/login → 401 for wrong password")
    void loginRejectsWrongPassword() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("nobody@test.com").password("wrongpassword").build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
