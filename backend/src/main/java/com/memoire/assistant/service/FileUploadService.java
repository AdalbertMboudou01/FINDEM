package com.memoire.assistant.service;

import com.memoire.assistant.dto.FileUploadDTO;
import com.memoire.assistant.model.CandidateFile;
import com.memoire.assistant.model.Candidate;
import com.memoire.assistant.repository.CandidateFileRepository;
import com.memoire.assistant.repository.CandidateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class FileUploadService {
    
    @Autowired
    private CandidateFileRepository candidateFileRepository;
    
    @Autowired
    private CandidateRepository candidateRepository;
    
    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;
    
    public CandidateFile uploadFile(MultipartFile file, UUID candidateId, String fileType) throws IOException {
        // Vérifier que le candidat existe
        Candidate candidate = candidateRepository.findById(candidateId)
            .orElseThrow(() -> new RuntimeException("Candidat non trouvé"));
        
        // Créer le répertoire d'upload s'il n'existe pas
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Générer un nom de fichier unique
        String originalFileName = file.getOriginalFilename();
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        
        // Sauvegarder le fichier
        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), filePath);
        
        // Créer l'entité CandidateFile
        CandidateFile candidateFile = new CandidateFile(
            candidate,
            uniqueFileName,
            originalFileName,
            fileType,
            filePath.toString(),
            file.getSize(),
            file.getContentType()
        );

        if ("CV".equalsIgnoreCase(fileType)) {
            candidate.setCvPath(filePath.toString());
            candidate.setCvFileName(originalFileName);
            candidate.setCvContentType(file.getContentType());
            candidateRepository.save(candidate);
        }
        
        return candidateFileRepository.save(candidateFile);
    }
    
    public Candidate updateCandidateLinks(UUID candidateId, String githubUrl, String portfolioUrl) {
        Candidate candidate = candidateRepository.findById(candidateId)
            .orElseThrow(() -> new RuntimeException("Candidat non trouvé"));
        
        if (githubUrl != null && !githubUrl.trim().isEmpty()) {
            candidate.setGithubUrl(githubUrl.trim());
        }
        
        if (portfolioUrl != null && !portfolioUrl.trim().isEmpty()) {
            candidate.setPortfolioUrl(portfolioUrl.trim());
        }
        
        return candidateRepository.save(candidate);
    }
    
    public CandidateFile processFileUpload(FileUploadDTO fileUploadDTO, MultipartFile file) throws IOException {
        if (file != null && !file.isEmpty()) {
            return uploadFile(file, fileUploadDTO.getCandidateId(), fileUploadDTO.getFileType());
        } else {
            // Si pas de fichier, mettre à jour juste les liens
            updateCandidateLinks(
                fileUploadDTO.getCandidateId(), 
                fileUploadDTO.getGithubUrl(), 
                fileUploadDTO.getPortfolioUrl()
            );
            return null;
        }
    }
    
    public List<CandidateFile> getCandidateFiles(UUID candidateId) {
        return candidateFileRepository.findByCandidate_CandidateId(candidateId);
    }
    
    public CandidateFile getCandidateCV(UUID candidateId) {
        List<CandidateFile> files = candidateFileRepository.findByCandidate_CandidateIdAndFileType(candidateId, "CV");
        return files.isEmpty() ? null : files.get(0);
    }
    
    public void deleteCandidateFiles(UUID candidateId) {
        List<CandidateFile> files = candidateFileRepository.findByCandidate_CandidateId(candidateId);
        
        for (CandidateFile file : files) {
            try {
                Path filePath = Paths.get(file.getFilePath());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }
            } catch (IOException e) {
                // Logger l'erreur mais continuer
                System.err.println("Erreur lors de la suppression du fichier: " + file.getFilePath());
            }
        }
        
        candidateFileRepository.deleteByCandidate_CandidateId(candidateId);
    }
}
