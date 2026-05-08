package com.memoire.assistant.repository;

import com.memoire.assistant.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByApplicationIdAndCompanyIdOrderByCreatedAtDesc(UUID applicationId, UUID companyId);

    List<Task> findByAssigneeIdAndCompanyIdOrderByDueDateAsc(UUID assigneeId, UUID companyId);

    List<Task> findByCompanyIdAndStatusNotAndDueDateBeforeOrderByDueDateAsc(
            UUID companyId, Task.Status status, LocalDate date);

    List<Task> findByApplicationIdAndCompanyId(UUID applicationId, UUID companyId);
}
