package com.memoire.assistant.service;

import com.memoire.assistant.model.ApplicationStatus;
import com.memoire.assistant.repository.ApplicationStatusRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ApplicationStatusServiceTest {
    @Mock
    private ApplicationStatusRepository repository;
    @InjectMocks
    private ApplicationStatusService service;

    @Test
    void getAllStatuses_returnsList() {
        when(repository.findAll()).thenReturn(List.of(new ApplicationStatus()));
        assertEquals(1, service.getAllStatuses().size());
    }

    @Test
    void getStatusById_returnsOptional() {
        UUID id = UUID.randomUUID();
        ApplicationStatus status = new ApplicationStatus();
        when(repository.findById(id)).thenReturn(Optional.of(status));
        assertTrue(service.getStatusById(id).isPresent());
    }

    @Test
    void saveStatus_returnsSaved() {
        ApplicationStatus status = new ApplicationStatus();
        when(repository.save(status)).thenReturn(status);
        assertEquals(status, service.saveStatus(status));
    }

    @Test
    void deleteStatus_callsRepository() {
        UUID id = UUID.randomUUID();
        service.deleteStatus(id);
        verify(repository).deleteById(id);
    }
}
