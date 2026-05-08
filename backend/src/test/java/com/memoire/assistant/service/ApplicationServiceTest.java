package com.memoire.assistant.service;

import com.memoire.assistant.model.Application;
import com.memoire.assistant.repository.ApplicationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {
    @Mock
    private ApplicationRepository repository;
    @InjectMocks
    private ApplicationService service;

    @Test
    void getAllApplications_returnsList() {
        when(repository.findAll()).thenReturn(List.of(new Application()));
        assertEquals(1, service.getAllApplications().size());
    }

    @Test
    void getApplicationById_returnsOptional() {
        UUID id = UUID.randomUUID();
        Application application = new Application();
        when(repository.findById(id)).thenReturn(Optional.of(application));
        assertTrue(service.getApplicationById(id).isPresent());
    }

    @Test
    void saveApplication_returnsSaved() {
        Application application = new Application();
        when(repository.save(application)).thenReturn(application);
        assertEquals(application, service.saveApplication(application));
    }

    @Test
    void deleteApplication_callsRepository() {
        UUID id = UUID.randomUUID();
        service.deleteApplication(id);
        verify(repository).deleteById(id);
    }
}
