package com.memoire.assistant.controller;

import com.memoire.assistant.dto.AcceptInvitationRequest;
import com.memoire.assistant.dto.AuthResponse;
import com.memoire.assistant.dto.CompanyInvitationDTO;
import com.memoire.assistant.dto.InviteRecruiterRequest;
import com.memoire.assistant.service.CompanyInvitationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/company-members")
public class CompanyMemberController {
    private final CompanyInvitationService invitationService;

    public CompanyMemberController(CompanyInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @GetMapping("/invitations")
    public List<CompanyInvitationDTO> listInvitations() {
        return invitationService.listForCurrentCompany();
    }

    @PostMapping("/invitations")
    public ResponseEntity<CompanyInvitationDTO> invite(@Valid @RequestBody InviteRecruiterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invitationService.invite(request));
    }

    @DeleteMapping("/invitations/{invitationId}")
    public ResponseEntity<Void> cancel(@PathVariable UUID invitationId) {
        invitationService.cancel(invitationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/invitations/{token}")
    public CompanyInvitationDTO getInvitation(@PathVariable String token) {
        return invitationService.getByToken(token);
    }

    @PostMapping("/accept-invitation")
    public AuthResponse acceptInvitation(@Valid @RequestBody AcceptInvitationRequest request) {
        return invitationService.accept(request);
    }
}
