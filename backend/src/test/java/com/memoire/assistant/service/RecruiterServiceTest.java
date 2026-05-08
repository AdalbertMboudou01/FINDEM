package com.memoire.assistant.service;

import com.memoire.assistant.model.Recruiter;
import com.memoire.assistant.repository.RecruiterRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RecruiterServiceTest {
    @Mock
    private RecruiterRepository repository;
    @InjectMocks
    private RecruiterService service;

    @Test
    void getAllRecruiters_returnsList() {
        when(repository.findAll()).thenReturn(List.of(new Recruiter()));
        assertEquals(1, service.getAllRecruiters().size());
    }

    @Test
    void getRecruiterById_returnsOptional() {
        UUID id = UUID.randomUUID();
        Recruiter recruiter = new Recruiter();
        when(repository.findById(id)).thenReturn(Optional.of(recruiter));
        assertTrue(service.getRecruiterById(id).isPresent());
    }

    @Test
    void saveRecruiter_returnsSaved() {
        Recruiter recruiter = new Recruiter();
        when(repository.save(recruiter)).thenReturn(recruiter);
        assertEquals(recruiter, service.saveRecruiter(recruiter));
    }

    @Test
    void deleteRecruiter_callsRepository() {
        UUID id = UUID.randomUUID();
        service.deleteRecruiter(id);
        verify(repository).deleteById(id);
    }
}
