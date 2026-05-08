package com.memoire.assistant.controller;

import com.memoire.assistant.model.TeamChannel;
import com.memoire.assistant.model.TeamMessage;
import com.memoire.assistant.repository.JobRepository;
import com.memoire.assistant.repository.RecruiterRepository;
import com.memoire.assistant.repository.TeamChannelRepository;
import com.memoire.assistant.service.TeamMessageService;
import com.memoire.assistant.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/team")
public class TeamChannelController {

    @Autowired private TeamChannelRepository channelRepo;
    @Autowired private TeamMessageService messageService;
    @Autowired private JobRepository jobRepository;
    @Autowired private RecruiterRepository recruiterRepository;

    // ── Channels ──────────────────────────────────────────────────────────────

    @GetMapping("/channels")
    public ResponseEntity<List<Map<String, Object>>> listChannels() {
        UUID companyId = requireCompanyId();
        UUID recruiterId = TenantContext.getRecruiterId();

        List<Map<String, Object>> result = new ArrayList<>();

        // General channel (auto-create)
        TeamChannel general = getOrCreateGeneral(companyId);
        result.add(channelDto(general));

        // Offer channels — auto-create for every job of the company
        jobRepository.findByCompany_CompanyId(companyId).forEach(job -> {
            TeamChannel ch = channelRepo.findByCompanyIdAndJobId(companyId, job.getJobId())
                .orElseGet(() -> {
                    TeamChannel c = new TeamChannel();
                    c.setName(job.getTitle());
                    c.setType("OFFER");
                    c.setJobId(job.getJobId());
                    c.setCompanyId(companyId);
                    c.setCreatedAt(new Date());
                    return channelRepo.save(c);
                });
            result.add(channelDto(ch));
        });

        // User workspace (auto-create)
        if (recruiterId != null) {
            TeamChannel workspace = getOrCreateWorkspace(companyId, recruiterId);
            result.add(channelDto(workspace));
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/channels/general")
    public ResponseEntity<Map<String, Object>> getGeneralChannel() {
        UUID companyId = requireCompanyId();
        return ResponseEntity.ok(channelDto(getOrCreateGeneral(companyId)));
    }

    @GetMapping("/channels/workspace")
    public ResponseEntity<Map<String, Object>> getWorkspace() {
        UUID companyId = requireCompanyId();
        UUID recruiterId = TenantContext.getRecruiterId();
        if (recruiterId == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(channelDto(getOrCreateWorkspace(companyId, recruiterId)));
    }

    @GetMapping("/channels/job/{jobId}")
    public ResponseEntity<Map<String, Object>> getJobChannel(@PathVariable UUID jobId) {
        UUID companyId = requireCompanyId();
        TeamChannel channel = channelRepo.findByCompanyIdAndJobId(companyId, jobId)
            .orElseGet(() -> {
                String jobTitle = jobRepository.findById(jobId)
                    .map(j -> j.getTitle())
                    .orElse("Offre");
                TeamChannel c = new TeamChannel();
                c.setName(jobTitle);
                c.setType("OFFER");
                c.setJobId(jobId);
                c.setCompanyId(companyId);
                c.setCreatedAt(new Date());
                return channelRepo.save(c);
            });
        return ResponseEntity.ok(channelDto(channel));
    }

    // ── Messages ──────────────────────────────────────────────────────────────

    @GetMapping("/channels/{channelId}/messages")
    public ResponseEntity<List<Map<String, Object>>> getMessages(@PathVariable UUID channelId) {
        requireCompanyId();
        List<TeamMessage> messages = messageService.getMessages(channelId);
        return ResponseEntity.ok(messages.stream().map(this::messageDto).toList());
    }

    @PostMapping("/channels/{channelId}/messages")
    public ResponseEntity<Map<String, Object>> postMessage(
            @PathVariable UUID channelId,
            @RequestBody Map<String, String> body) {
        UUID recruiterId = TenantContext.getRecruiterId();
        String content = body.getOrDefault("content", "").strip();
        if (content.isBlank()) return ResponseEntity.badRequest().build();

        String authorName = resolveAuthorName(recruiterId);
        TeamMessage saved = messageService.post(channelId, recruiterId, authorName, content);
        return ResponseEntity.ok(messageDto(saved));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TeamChannel getOrCreateGeneral(UUID companyId) {
        return channelRepo.findByCompanyIdAndType(companyId, "GENERAL")
            .orElseGet(() -> {
                TeamChannel c = new TeamChannel();
                c.setName("Général");
                c.setType("GENERAL");
                c.setCompanyId(companyId);
                c.setCreatedAt(new Date());
                return channelRepo.save(c);
            });
    }

    private TeamChannel getOrCreateWorkspace(UUID companyId, UUID recruiterId) {
        return channelRepo.findByCompanyIdAndRecruiterIdAndType(companyId, recruiterId, "WORKSPACE")
            .orElseGet(() -> {
                String name = resolveAuthorName(recruiterId);
                TeamChannel c = new TeamChannel();
                c.setName("Mon espace — " + name);
                c.setType("WORKSPACE");
                c.setRecruiterId(recruiterId);
                c.setCompanyId(companyId);
                c.setCreatedAt(new Date());
                return channelRepo.save(c);
            });
    }

    private String resolveAuthorName(UUID recruiterId) {
        if (recruiterId == null) return "Recruteur";
        return recruiterRepository.findById(recruiterId)
            .map(r -> r.getName() != null ? r.getName() : "Recruteur")
            .orElse("Recruteur");
    }

    private Map<String, Object> channelDto(TeamChannel c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("channelId", c.getChannelId().toString());
        m.put("name", c.getName());
        m.put("type", c.getType());
        m.put("jobId", c.getJobId() != null ? c.getJobId().toString() : null);
        m.put("recruiterId", c.getRecruiterId() != null ? c.getRecruiterId().toString() : null);
        return m;
    }

    private Map<String, Object> messageDto(TeamMessage m) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("messageId", m.getMessageId().toString());
        d.put("authorId", m.getAuthorId() != null ? m.getAuthorId().toString() : null);
        d.put("authorName", m.getAuthorName());
        d.put("authorType", m.getAuthorType());
        d.put("content", m.getContent());
        d.put("mentions", m.getMentions());
        d.put("createdAt", m.getCreatedAt() != null ? m.getCreatedAt().toString() : null);
        return d;
    }

    private UUID requireCompanyId() {
        UUID id = TenantContext.getCompanyId();
        if (id == null) throw new IllegalStateException("Contexte entreprise manquant");
        return id;
    }
}
