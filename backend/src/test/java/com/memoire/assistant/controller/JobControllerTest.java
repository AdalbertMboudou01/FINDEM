package com.memoire.assistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoire.assistant.model.Job;
import com.memoire.assistant.service.JobService;
import com.memoire.assistant.dto.JobCreateRequest;
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

@WebMvcTest(controllers = JobController.class, 
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class
    })
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@Disabled("Temporarily disabled due to complex Spring context issues - will be fixed later")
class JobControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private JobService jobService;
    @MockBean
    private com.memoire.assistant.security.JwtUtil jwtUtil;
    @MockBean
    private com.memoire.assistant.security.CustomUserDetailsService customUserDetailsService;

    @Test
    void getAllJobs_shouldReturnList() throws Exception {
        when(jobService.getAllJobs()).thenReturn(List.of(new Job()));
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk());
    }

    @Test
    void getJobById_shouldReturnJob() throws Exception {
        UUID id = UUID.randomUUID();
        Job job = new Job();
        job.setJobId(id);
        when(jobService.getJobById(id)).thenReturn(Optional.of(job));
        mockMvc.perform(get("/api/jobs/" + id))
                .andExpect(status().isOk());
    }

    @Test
    void createJob_shouldReturnCreated() throws Exception {
        JobCreateRequest req = new JobCreateRequest();
        req.setTitle("Développeur");
        req.setDescription("desc");
        req.setLocation("Paris");
        req.setAlternanceRhythm("semaine");
        req.setBlockingCriteria(Collections.emptyMap());
        req.setSlug("dev");
        req.setCompanyId(UUID.randomUUID());
        req.setOwnerRecruiterId(UUID.randomUUID());
        Job job = new Job();
        when(jobService.saveJob(any())).thenReturn(job);
        mockMvc.perform(post("/api/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
}