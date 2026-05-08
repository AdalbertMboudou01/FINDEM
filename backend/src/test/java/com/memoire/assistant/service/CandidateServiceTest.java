package com.memoire.assistant.service;

import com.memoire.assistant.model.Candidate;
import com.memoire.assistant.model.Application;
import com.memoire.assistant.model.ApplicationStatus;
import com.memoire.assistant.model.Job;
import com.memoire.assistant.dto.CandidateSummaryDTO;
import com.memoire.assistant.repository.CandidateRepository;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CandidateServiceTest {
    
    private UUID testCompanyId = UUID.randomUUID();
    
    @Mock
    private CandidateRepository candidateRepository;
    
    @Mock
    private ApplicationRepository applicationRepository;
    
    @InjectMocks
    private CandidateService candidateService;
    
    @BeforeEach
    void setUp() {
        // Configurer le TenantContext pour les tests
        TenantContext.set(UUID.randomUUID(), UUID.randomUUID(), testCompanyId, "RECRUITER");
    }
    
    @AfterEach
    void tearDown() {
        // Nettoyer le TenantContext après chaque test
        TenantContext.clear();
    }
    
    @Test
    @DisplayName("Devrait récupérer tous les candidats")
    void testGetAllCandidates() {
        // Given
        List<Candidate> expectedCandidates = Arrays.asList(
            createMockCandidate("John", "Doe", "john@example.com"),
            createMockCandidate("Jane", "Smith", "jane@example.com")
        );
        
        when(candidateRepository.findAllByCompanyId(testCompanyId)).thenReturn(expectedCandidates);
        
        // When
        List<Candidate> result = candidateService.getAllCandidates();
        
        // Then
        assertEquals(expectedCandidates, result);
        verify(candidateRepository).findAllByCompanyId(testCompanyId);
    }
    
    @Test
    @DisplayName("Devrait récupérer un candidat par ID")
    void testGetCandidateById_Found() {
        // Given
        UUID candidateId = UUID.randomUUID();
        Candidate expectedCandidate = createMockCandidate("John", "Doe", "john@example.com");
        
        when(candidateRepository.findByCandidateIdAndCompanyId(candidateId, testCompanyId)).thenReturn(Optional.of(expectedCandidate));
        
        // When
        Optional<Candidate> result = candidateService.getCandidateById(candidateId);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(expectedCandidate, result.get());
        verify(candidateRepository).findByCandidateIdAndCompanyId(candidateId, testCompanyId);
    }
    
    @Test
    @DisplayName("Devrait retourner empty si candidat non trouvé")
    void testGetCandidateById_NotFound() {
        // Given
        UUID candidateId = UUID.randomUUID();
        
        when(candidateRepository.findByCandidateIdAndCompanyId(candidateId, testCompanyId)).thenReturn(Optional.empty());
        
        // When
        Optional<Candidate> result = candidateService.getCandidateById(candidateId);
        
        // Then
        assertFalse(result.isPresent());
        verify(candidateRepository).findByCandidateIdAndCompanyId(candidateId, testCompanyId);
    }
    
    @Test
    @DisplayName("Devrait sauvegarder un candidat")
    void testSaveCandidate() {
        // Given
        Candidate candidate = createMockCandidate("John", "Doe", "john@example.com");
        Candidate savedCandidate = createMockCandidate("John", "Doe", "john@example.com");
        savedCandidate.setCandidateId(UUID.randomUUID());
        
        when(candidateRepository.save(candidate)).thenReturn(savedCandidate);
        
        // When
        Candidate result = candidateService.saveCandidate(candidate);
        
        // Then
        assertEquals(savedCandidate, result);
        verify(candidateRepository).save(candidate);
    }
    
    @Test
    @DisplayName("Devrait supprimer un candidat")
    void testDeleteCandidate() {
        // Given
        UUID candidateId = UUID.randomUUID();
        Candidate candidate = createMockCandidate("John", "Doe", "john@example.com");
        
        when(candidateRepository.findByCandidateIdAndCompanyId(candidateId, testCompanyId)).thenReturn(Optional.of(candidate));
        
        // When
        candidateService.deleteCandidate(candidateId);
        
        // Then
        verify(candidateRepository).deleteById(candidateId);
    }
    
    @Test
    @DisplayName("Devrait récupérer les candidats du vivier")
    void testGetPoolCandidates() {
        // Given
        List<Candidate> expectedPoolCandidates = Arrays.asList(
            createMockCandidate("John", "Doe", "john@example.com"),
            createMockCandidate("Jane", "Smith", "jane@example.com")
        );
        
        when(candidateRepository.findInPoolByCompanyId(testCompanyId)).thenReturn(expectedPoolCandidates);
        
        // When
        List<Candidate> result = candidateService.getPoolCandidates();
        
        // Then
        assertEquals(expectedPoolCandidates, result);
        verify(candidateRepository).findInPoolByCompanyId(testCompanyId);
    }
    
    @Test
    @DisplayName("Devrait récupérer le résumé d'un candidat")
    void testGetCandidateSummary_Found() {
        // Given
        UUID candidateId = UUID.randomUUID();
        Candidate candidate = createMockCandidate("John", "Doe", "john@example.com");
        candidate.setCandidateId(candidateId);
        candidate.setInPool(true);
        
        Application application = createMockApplication("ENTRETIEN", candidate);
        Application app2 = createMockApplication("REJETE", candidate);
        
        // S'assurer que ENTRETIEN est plus récent que REJETE pour le tri
        application.setCreatedAt(new java.util.Date(System.currentTimeMillis() - 1000)); // Plus récent
        app2.setCreatedAt(new java.util.Date(System.currentTimeMillis() - 5000)); // Plus ancien
        
        List<Application> applications = Arrays.asList(application, app2);
        
        when(candidateRepository.findByCandidateIdAndCompanyId(candidateId, testCompanyId)).thenReturn(Optional.of(candidate));
        when(applicationRepository.findByCandidate_CandidateId(candidateId)).thenReturn(applications);
        
        // When
        Optional<CandidateSummaryDTO> result = candidateService.getCandidateSummary(candidateId);
        
        // Then
        assertTrue(result.isPresent());
        CandidateSummaryDTO summary = result.get();
        assertEquals(candidateId, summary.getCandidateId());
        assertEquals("John", summary.getFirstName());
        assertEquals("Doe", summary.getLastName());
        assertEquals("john@example.com", summary.getEmail());
        assertTrue(summary.getInPool());
        assertEquals(2, summary.getApplicationsCount());
        assertEquals("ENTRETIEN", summary.getLastStatus());
        verify(candidateRepository).findByCandidateIdAndCompanyId(candidateId, testCompanyId);
        verify(applicationRepository).findByCandidate_CandidateId(candidateId);
    }
    
    @Test
    @DisplayName("Devrait retourner empty si candidat non trouvé pour résumé")
    void testGetCandidateSummary_NotFound() {
        // Given
        UUID candidateId = UUID.randomUUID();
        
        when(candidateRepository.findByCandidateIdAndCompanyId(candidateId, testCompanyId)).thenReturn(Optional.empty());
        
        // When
        Optional<CandidateSummaryDTO> result = candidateService.getCandidateSummary(candidateId);
        
        // Then
        assertFalse(result.isPresent());
        verify(candidateRepository).findByCandidateIdAndCompanyId(candidateId, testCompanyId);
    }
    
    @Test
    @DisplayName("Devrait gérer le cas sans applications")
    void testGetCandidateSummary_NoApplications() {
        // Given
        UUID candidateId = UUID.randomUUID();
        Candidate candidate = createMockCandidate("John", "Doe", "john@example.com");
        candidate.setCandidateId(candidateId);
        
        when(candidateRepository.findByCandidateIdAndCompanyId(candidateId, testCompanyId)).thenReturn(Optional.of(candidate));
        when(applicationRepository.findByCandidate_CandidateId(candidateId)).thenReturn(Arrays.asList());
        
        // When
        Optional<CandidateSummaryDTO> result = candidateService.getCandidateSummary(candidateId);
        
        // Then
        assertTrue(result.isPresent());
        CandidateSummaryDTO summary = result.get();
        assertEquals(candidateId, summary.getCandidateId());
        assertEquals("John", summary.getFirstName());
        assertEquals("Doe", summary.getLastName());
        assertEquals("john@example.com", summary.getEmail());
        assertEquals(0, summary.getApplicationsCount());
        assertNull(summary.getLastStatus());
        verify(candidateRepository).findByCandidateIdAndCompanyId(candidateId, testCompanyId);
        verify(applicationRepository).findByCandidate_CandidateId(candidateId);
    }
    
    // Méthodes utilitaires pour les tests
    private Candidate createMockCandidate(String firstName, String lastName, String email) {
        Candidate candidate = new Candidate();
        candidate.setFirstName(firstName);
        candidate.setLastName(lastName);
        candidate.setEmail(email);
        candidate.setInPool(false);
        return candidate;
    }
    
    private Application createMockApplication(String statusLabel, Candidate candidate) {
        Application application = new Application();
        ApplicationStatus status = new ApplicationStatus();
        status.setCode(statusLabel);
        status.setLabel(statusLabel);
        application.setStatus(status);
        application.setCreatedAt(new java.util.Date());
        application.setCandidate(candidate);
        
        // Ajouter Job avec Company pour le filtrage par companyId
        Job job = new Job();
        job.setJobId(UUID.randomUUID());
        job.setTitle("Test Job");
        
        com.memoire.assistant.model.Company company = new com.memoire.assistant.model.Company();
        company.setCompanyId(testCompanyId);
        company.setName("Test Company");
        job.setCompany(company);
        
        application.setJob(job);
        return application;
    }
}
