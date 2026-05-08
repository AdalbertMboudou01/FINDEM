package com.memoire.assistant.repository;

import com.memoire.assistant.model.CandidateFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CandidateFileRepository extends JpaRepository<CandidateFile, UUID> {
    List<CandidateFile> findByCandidate_CandidateId(UUID candidateId);
    List<CandidateFile> findByCandidate_CandidateIdAndFileType(UUID candidateId, String fileType);
    void deleteByCandidate_CandidateId(UUID candidateId);
}
