package com.memoire.assistant.controller;

import com.memoire.assistant.dto.FileUploadDTO;
import com.memoire.assistant.model.CandidateFile;
import com.memoire.assistant.model.Candidate;
import com.memoire.assistant.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@Tag(name = "File Upload", description = "Gestion des fichiers des candidats")
public class FileUploadController {
    
    @Autowired
    private FileUploadService fileUploadService;
    
    @PostMapping("/upload")
    @Operation(summary = "Uploader un fichier", description = "Permet d'uploader un fichier (CV, portfolio, etc.) pour un candidat")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("candidateId") UUID candidateId,
            @RequestParam("fileType") String fileType) {
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Le fichier est vide");
            }
            
            CandidateFile uploadedFile = fileUploadService.uploadFile(file, candidateId, fileType);
            return ResponseEntity.ok(uploadedFile);
            
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Erreur lors de l'upload du fichier: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/upload-with-links")
    @Operation(summary = "Uploader un fichier avec liens", description = "Permet d'uploader un fichier et/ou de mettre à jour les liens GitHub/portfolio")
    public ResponseEntity<?> uploadFileWithLinks(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("candidateId") UUID candidateId,
            @RequestParam("fileType") String fileType,
            @RequestParam(value = "githubUrl", required = false) String githubUrl,
            @RequestParam(value = "portfolioUrl", required = false) String portfolioUrl) {
        
        try {
            FileUploadDTO fileUploadDTO = new FileUploadDTO(candidateId, fileType);
            fileUploadDTO.setGithubUrl(githubUrl);
            fileUploadDTO.setPortfolioUrl(portfolioUrl);
            
            CandidateFile uploadedFile = fileUploadService.processFileUpload(fileUploadDTO, file);
            
            if (uploadedFile != null) {
                return ResponseEntity.ok(uploadedFile);
            } else {
                // Seulement mise à jour des liens
                return ResponseEntity.ok("Liens mis à jour avec succès");
            }
            
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Erreur lors de l'upload: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PutMapping("/links")
    @Operation(summary = "Mettre à jour les liens d'un candidat", description = "Met à jour les liens GitHub et/ou portfolio d'un candidat")
    public ResponseEntity<Candidate> updateCandidateLinks(
            @RequestParam UUID candidateId,
            @RequestParam(required = false) String githubUrl,
            @RequestParam(required = false) String portfolioUrl) {
        
        try {
            Candidate updatedCandidate = fileUploadService.updateCandidateLinks(candidateId, githubUrl, portfolioUrl);
            return ResponseEntity.ok(updatedCandidate);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    @GetMapping("/candidate/{candidateId}")
    @Operation(summary = "Récupérer les fichiers d'un candidat", description = "Retourne tous les fichiers uploadés par un candidat")
    public ResponseEntity<List<CandidateFile>> getCandidateFiles(@PathVariable UUID candidateId) {
        List<CandidateFile> files = fileUploadService.getCandidateFiles(candidateId);
        return ResponseEntity.ok(files);
    }
    
    @GetMapping("/candidate/{candidateId}/cv")
    @Operation(summary = "Récupérer le CV d'un candidat", description = "Retourne le fichier CV du candidat s'il existe")
    public ResponseEntity<CandidateFile> getCandidateCV(@PathVariable UUID candidateId) {
        CandidateFile cv = fileUploadService.getCandidateCV(candidateId);
        return cv != null ? ResponseEntity.ok(cv) : ResponseEntity.notFound().build();
    }

    @GetMapping("/candidate/{candidateId}/cv/content")
    @Operation(summary = "Afficher le CV d'un candidat", description = "Retourne le contenu binaire du CV pour affichage ou telechargement")
    public ResponseEntity<Resource> streamCandidateCV(@PathVariable UUID candidateId) {
        CandidateFile cv = fileUploadService.getCandidateCV(candidateId);
        if (cv == null || cv.getFilePath() == null || cv.getFilePath().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path filePath = Paths.get(cv.getFilePath());
            if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
                return ResponseEntity.notFound().build();
            }

            byte[] fileContent = Files.readAllBytes(filePath);
            Resource resource = new ByteArrayResource(fileContent);

            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            if (cv.getMimeType() != null && !cv.getMimeType().isBlank()) {
                try {
                    mediaType = MediaType.parseMediaType(cv.getMimeType());
                } catch (Exception ignored) {
                    mediaType = MediaType.APPLICATION_OCTET_STREAM;
                }
            }

            String fileName = cv.getOriginalFileName() != null && !cv.getOriginalFileName().isBlank()
                ? cv.getOriginalFileName()
                : cv.getFileName();

            return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(fileContent.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @DeleteMapping("/candidate/{candidateId}")
    @Operation(summary = "Supprimer tous les fichiers d'un candidat", description = "Supprime tous les fichiers uploadés par un candidat")
    public ResponseEntity<Void> deleteCandidateFiles(@PathVariable UUID candidateId) {
        try {
            fileUploadService.deleteCandidateFiles(candidateId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
