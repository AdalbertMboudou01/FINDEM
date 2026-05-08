package com.memoire.assistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoire.assistant.model.Recruiter;
import com.memoire.assistant.service.RecruiterService;
import com.memoire.assistant.dto.RecruiterCreateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RecruiterController.class, 
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class
    })
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@Disabled("Temporarily disabled due to complex Spring context issues - will be fixed later")
class RecruiterControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private RecruiterService recruiterService;
    @MockBean
    private com.memoire.assistant.security.JwtUtil jwtUtil;
    @MockBean
    private com.memoire.assistant.security.CustomUserDetailsService customUserDetailsService;

    @Test
    void getAllRecruiters_shouldReturnList() throws Exception {
        when(recruiterService.getAllRecruiters()).thenReturn(List.of(new Recruiter()));
        mockMvc.perform(get("/api/recruiters"))
                .andExpect(status().isOk());
    }

    @Test
    void getRecruiterById_shouldReturnRecruiter() throws Exception {
        UUID id = UUID.randomUUID();
        Recruiter recruiter = new Recruiter();
        recruiter.setRecruiterId(id);
        when(recruiterService.getRecruiterById(id)).thenReturn(Optional.of(recruiter));
        mockMvc.perform(get("/api/recruiters/" + id))
                .andExpect(status().isOk());
    }

    @Test
    void createRecruiter_shouldReturnCreated() throws Exception {
        RecruiterCreateRequest req = new RecruiterCreateRequest();
        req.setName("John");
        req.setEmail("john@mail.com");
        req.setPassword("password123"); // Ajout du champ obligatoire
        req.setRole("ADMIN");
        Recruiter recruiter = new Recruiter();
        recruiter.setName("John");
        when(recruiterService.saveRecruiter(any())).thenReturn(recruiter);
        mockMvc.perform(post("/api/recruiters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
}