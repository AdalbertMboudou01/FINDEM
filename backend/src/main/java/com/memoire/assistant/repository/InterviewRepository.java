package com.memoire.assistant.repository;

import com.memoire.assistant.model.Interview;
import com.memoire.assistant.model.Candidate;
import com.memoire.assistant.model.Job;
import com.memoire.assistant.model.Recruiter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, UUID> {
    
    List<Interview> findByCandidate_CandidateId(UUID candidateId);
    
    List<Interview> findByJob_JobId(UUID jobId);
    
    List<Interview> findByRecruiter_RecruiterId(UUID recruiterId);
    
    List<Interview> findByStatus(String status);
    
    @Query("SELECT i FROM Interview i WHERE i.scheduledAt BETWEEN :start AND :end")
    List<Interview> findByScheduledAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT i FROM Interview i WHERE i.recruiter.recruiterId = :recruiterId AND i.status = 'SCHEDULED'")
    List<Interview> findScheduledInterviewsByRecruiter(@Param("recruiterId") UUID recruiterId);
    
    @Query("SELECT i FROM Interview i WHERE i.candidate.candidateId = :candidateId AND i.job.jobId = :jobId")
    List<Interview> findByCandidateAndJob(@Param("candidateId") UUID candidateId, @Param("jobId") UUID jobId);
    
    @Query("SELECT COUNT(i) FROM Interview i WHERE i.job.jobId = :jobId AND i.status = 'COMPLETED'")
    Long countCompletedInterviewsByJob(@Param("jobId") UUID jobId);
}
