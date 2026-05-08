package com.memoire.assistant.service;

import com.memoire.assistant.dto.CandidateSummaryDTO;
import com.memoire.assistant.model.Candidate;
import com.memoire.assistant.model.Application;
import com.memoire.assistant.repository.CandidateRepository;
import com.memoire.assistant.repository.ApplicationRepository;
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
public class PoolManagementServiceTest {
    
    @Mock
    private CandidateRepository candidateRepository;
    
    @Mock
    private ApplicationRepository applicationRepository;
    
    @InjectMocks
    private PoolManagementService poolManagementService;
    
    @Test
    @DisplayName("Devrait ajouter un candidat au vivier avec succès")
    void testAddToPool_Success() {
        // Given
        UUID candidateId = UUID.randomUUID();
        String reason = "Profil intéressant pour future opportunités";
        Candidate candidate = createMockCandidate(candidateId, false);
        Candidate updatedCandidate = createMockCandidate(candidateId, true);
        
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(candidateRepository.save(any(Candidate.class))).thenReturn(updatedCandidate);
        
        // When
        Candidate result = poolManagementService.addToPool(candidateId, reason);
        
        // Then
        assertNotNull(result);
        assertTrue(result.getInPool());
        verify(candidateRepository).findById(candidateId);
        verify(candidateRepository).save(any(Candidate.class));
    }
    
    @Test
    @DisplayName("Devrait lancer une exception si candidat non trouvé pour l'ajout au vivier")
    void testAddToPool_CandidateNotFound() {
        // Given
        UUID candidateId = UUID.randomUUID();
        String reason = "Profil intéressant";
        
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.empty());
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> poolManagementService.addToPool(candidateId, reason));
        
        assertEquals("Candidat non trouvé", exception.getMessage());
        verify(candidateRepository).findById(candidateId);
        verify(candidateRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Devrait retirer un candidat du vivier avec succès")
    void testRemoveFromPool_Success() {
        // Given
        UUID candidateId = UUID.randomUUID();
        Candidate candidate = createMockCandidate(candidateId, true);
        Candidate updatedCandidate = createMockCandidate(candidateId, false);
        
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(candidateRepository.save(any(Candidate.class))).thenReturn(updatedCandidate);
        
        // When
        Candidate result = poolManagementService.removeFromPool(candidateId);
        
        // Then
        assertNotNull(result);
        assertFalse(result.getInPool());
        verify(candidateRepository).findById(candidateId);
        verify(candidateRepository).save(any(Candidate.class));
    }
    
    @Test
    @DisplayName("Devrait lancer une exception si candidat non trouvé pour le retrait du vivier")
    void testRemoveFromPool_CandidateNotFound() {
        // Given
        UUID candidateId = UUID.randomUUID();
        
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.empty());
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> poolManagementService.removeFromPool(candidateId));
        
        assertEquals("Candidat non trouvé", exception.getMessage());
        verify(candidateRepository).findById(candidateId);
        verify(candidateRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Devrait récupérer les candidats du vivier")
    void testGetPoolCandidates_Success() {
        // Given
        Candidate candidate1 = createMockCandidate(UUID.randomUUID(), true);
        Candidate candidate2 = createMockCandidate(UUID.randomUUID(), true);
        List<Candidate> poolCandidates = Arrays.asList(candidate1, candidate2);
        
        Application app1 = createMockApplication(UUID.randomUUID(), candidate1);
        Application app2 = createMockApplication(UUID.randomUUID(), candidate2);
        Application app3 = createMockApplication(UUID.randomUUID(), candidate2);
        
        when(candidateRepository.findByInPoolTrue()).thenReturn(poolCandidates);
        when(applicationRepository.findByCandidate_CandidateId(candidate1.getCandidateId()))
            .thenReturn(Arrays.asList(app1));
        when(applicationRepository.findByCandidate_CandidateId(candidate2.getCandidateId()))
            .thenReturn(Arrays.asList(app2, app3));
        
        // When
        List<CandidateSummaryDTO> result = poolManagementService.getPoolCandidates();
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(candidateRepository).findByInPoolTrue();
        verify(applicationRepository).findByCandidate_CandidateId(candidate1.getCandidateId());
        verify(applicationRepository).findByCandidate_CandidateId(candidate2.getCandidateId());
    }
    
    @Test
    @DisplayName("Devrait gérer le vivier vide")
    void testGetPoolCandidates_EmptyPool() {
        // Given
        List<Candidate> emptyPool = Arrays.asList();
        
        when(candidateRepository.findByInPoolTrue()).thenReturn(emptyPool);
        
        // When
        List<CandidateSummaryDTO> result = poolManagementService.getPoolCandidates();
        
        // Then
        assertNotNull(result);
        assertEquals(0, result.size());
        verify(candidateRepository).findByInPoolTrue();
        verify(applicationRepository, never()).findByCandidate_CandidateId(any());
    }
    
    @Test
    @DisplayName("Devrait gérer les candidats sans applications")
    void testGetPoolCandidates_NoApplications() {
        // Given
        Candidate candidate = createMockCandidate(UUID.randomUUID(), true);
        List<Candidate> poolCandidates = Arrays.asList(candidate);
        
        when(candidateRepository.findByInPoolTrue()).thenReturn(poolCandidates);
        when(applicationRepository.findByCandidate_CandidateId(candidate.getCandidateId()))
            .thenReturn(Arrays.asList());
        
        // When
        List<CandidateSummaryDTO> result = poolManagementService.getPoolCandidates();
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        CandidateSummaryDTO summary = result.get(0);
        assertEquals(0, summary.getApplicationsCount());
        verify(candidateRepository).findByInPoolTrue();
        verify(applicationRepository).findByCandidate_CandidateId(candidate.getCandidateId());
    }
    
    @Test
    @DisplayName("Devrait créer le résumé correct du candidat")
    void testGetPoolCandidates_SummaryCreation() {
        // Given
        Candidate candidate = createMockCandidate(UUID.randomUUID(), true);
        List<Candidate> poolCandidates = Arrays.asList(candidate);
        
        Application app1 = createMockApplication(UUID.randomUUID(), candidate);
        Application app2 = createMockApplication(UUID.randomUUID(), candidate);
        List<Application> applications = Arrays.asList(app1, app2);
        
        when(candidateRepository.findByInPoolTrue()).thenReturn(poolCandidates);
        when(applicationRepository.findByCandidate_CandidateId(candidate.getCandidateId()))
            .thenReturn(applications);
        
        // When
        List<CandidateSummaryDTO> result = poolManagementService.getPoolCandidates();
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        CandidateSummaryDTO summary = result.get(0);
        assertEquals(candidate.getCandidateId(), summary.getCandidateId());
        assertEquals(candidate.getFirstName(), summary.getFirstName());
        assertEquals(candidate.getLastName(), summary.getLastName());
        assertEquals(candidate.getEmail(), summary.getEmail());
        assertTrue(summary.getInPool());
        assertEquals(2, summary.getApplicationsCount());
    }
    
    // Méthodes utilitaires pour les tests
    private Candidate createMockCandidate(UUID candidateId, boolean inPool) {
        Candidate candidate = new Candidate();
        candidate.setCandidateId(candidateId);
        candidate.setFirstName("John");
        candidate.setLastName("Doe");
        candidate.setEmail("john@example.com");
        candidate.setInPool(inPool);
        return candidate;
    }
    
    private Application createMockApplication(UUID applicationId, Candidate candidate) {
        Application app = new Application();
        app.setApplicationId(applicationId);
        app.setCandidate(candidate);
        app.setCreatedAt(new java.util.Date());
        return app;
    }
}
