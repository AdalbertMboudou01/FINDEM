package com.memoire.assistant.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import com.memoire.assistant.repository.*;
import com.memoire.assistant.service.*;

@TestConfiguration
@Profile("test")
public class TestConfig {

    // Repository mocks
    @Bean
    @Primary
    public CandidateRepository candidateRepository() {
        return org.mockito.Mockito.mock(CandidateRepository.class);
    }

    @Bean
    @Primary
    public JobRepository jobRepository() {
        return org.mockito.Mockito.mock(JobRepository.class);
    }

    @Bean
    @Primary
    public RecruiterRepository recruiterRepository() {
        return org.mockito.Mockito.mock(RecruiterRepository.class);
    }

    @Bean
    @Primary
    public UserRepository userRepository() {
        return org.mockito.Mockito.mock(UserRepository.class);
    }

    @Bean
    @Primary
    public CompanyRepository companyRepository() {
        return org.mockito.Mockito.mock(CompanyRepository.class);
    }

    @Bean
    @Primary
    public ApplicationRepository applicationRepository() {
        return org.mockito.Mockito.mock(ApplicationRepository.class);
    }

    @Bean
    @Primary
    public ChatAnswerRepository chatAnswerRepository() {
        return org.mockito.Mockito.mock(ChatAnswerRepository.class);
    }

    @Bean
    @Primary
    public PasswordResetTokenRepository passwordResetTokenRepository() {
        return org.mockito.Mockito.mock(PasswordResetTokenRepository.class);
    }

    @Bean
    @Primary
    public InterviewRepository interviewRepository() {
        return org.mockito.Mockito.mock(InterviewRepository.class);
    }

    // Service mocks
    @Bean
    @Primary
    public ApplicationService applicationService() {
        return org.mockito.Mockito.mock(ApplicationService.class);
    }

    @Bean
    @Primary
    public ApplicationStatusService applicationStatusService() {
        return org.mockito.Mockito.mock(ApplicationStatusService.class);
    }

    @Bean
    @Primary
    public ApplicationActivityService applicationActivityService() {
        return org.mockito.Mockito.mock(ApplicationActivityService.class);
    }

    @Bean
    @Primary
    public CandidateService candidateService() {
        return org.mockito.Mockito.mock(CandidateService.class);
    }

    @Bean
    @Primary
    public CompanyService companyService() {
        return org.mockito.Mockito.mock(CompanyService.class);
    }

    @Bean
    @Primary
    public JobService jobService() {
        return org.mockito.Mockito.mock(JobService.class);
    }

    @Bean
    @Primary
    public RecruiterService recruiterService() {
        return org.mockito.Mockito.mock(RecruiterService.class);
    }

    @Bean
    @Primary
    public TeamViewService teamViewService() {
        return org.mockito.Mockito.mock(TeamViewService.class);
    }

    @Bean
    @Primary
    public AuthOnboardingService authOnboardingService() {
        return org.mockito.Mockito.mock(AuthOnboardingService.class);
    }

    @Bean
    @Primary
    public PasswordResetService passwordResetService() {
        return org.mockito.Mockito.mock(PasswordResetService.class);
    }

    @Bean
    @Primary
    public SemanticExtractionService semanticExtractionService() {
        return org.mockito.Mockito.mock(SemanticExtractionService.class);
    }

    @Bean
    @Primary
    public ChatMessageService chatMessageService() {
        return org.mockito.Mockito.mock(ChatMessageService.class);
    }

    @Bean
    @Primary
    public ChatAnswerService chatAnswerService() {
        return org.mockito.Mockito.mock(ChatAnswerService.class);
    }

    // Security mocks
    @Bean
    @Primary
    public com.memoire.assistant.security.CustomUserDetailsService customUserDetailsService() {
        return org.mockito.Mockito.mock(com.memoire.assistant.security.CustomUserDetailsService.class);
    }

    @Bean
    @Primary
    public com.memoire.assistant.security.JwtUtil jwtUtil() {
        return org.mockito.Mockito.mock(com.memoire.assistant.security.JwtUtil.class);
    }

    @Bean
    @Primary
    public org.springframework.security.authentication.AuthenticationManager authenticationManager() {
        return org.mockito.Mockito.mock(org.springframework.security.authentication.AuthenticationManager.class);
    }

    @Bean
    @Primary
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return org.mockito.Mockito.mock(org.springframework.security.crypto.password.PasswordEncoder.class);
    }
}
