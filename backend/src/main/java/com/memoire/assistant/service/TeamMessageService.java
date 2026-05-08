package com.memoire.assistant.service;

import com.memoire.assistant.model.AIPersona;
import com.memoire.assistant.model.Job;
import com.memoire.assistant.model.TeamChannel;
import com.memoire.assistant.model.TeamMessage;
import com.memoire.assistant.repository.JobRepository;
import com.memoire.assistant.repository.TeamChannelRepository;
import com.memoire.assistant.repository.TeamMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class TeamMessageService {

    private static final Logger log = LoggerFactory.getLogger(TeamMessageService.class);

    private static final UUID[] AI_IDS = {
        AIPersona.FINDEM_ASSIST_ID,
        AIPersona.FINDEM_LOOKER_ID,
        AIPersona.FINDEM_WORKER_ID
    };

    @Autowired private TeamChannelRepository channelRepository;
    @Autowired private TeamMessageRepository messageRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private AIClientService aiClientService;

    public TeamMessage post(UUID channelId, UUID authorId, String authorName, String content) {
        TeamChannel channel = channelRepository.findById(channelId)
            .orElseThrow(() -> new IllegalArgumentException("Channel introuvable"));

        TeamMessage msg = new TeamMessage();
        msg.setChannel(channel);
        msg.setAuthorId(authorId);
        msg.setAuthorName(authorName);
        msg.setAuthorType("HUMAN");
        msg.setContent(content.strip());
        msg.setMentions(extractMentionsJson(content));
        msg.setCreatedAt(LocalDateTime.now());
        TeamMessage saved = messageRepository.save(msg);

        triggerAIAsync(channel, content, authorName);
        return saved;
    }

    public List<TeamMessage> getMessages(UUID channelId) {
        return messageRepository.findByChannel_ChannelIdOrderByCreatedAtAsc(channelId);
    }

    public List<TeamMessage> getRecentMessages(UUID channelId, int limit) {
        List<TeamMessage> desc = messageRepository.findByChannel_ChannelIdOrderByCreatedAtDesc(
            channelId, PageRequest.of(0, limit));
        List<TeamMessage> asc = new ArrayList<>(desc);
        Collections.reverse(asc);
        return asc;
    }

    // ── AI trigger ────────────────────────────────────────────────────────────

    @Async
    public void triggerAIAsync(TeamChannel channel, String userMessage, String authorName) {
        if (!aiClientService.isEnabled()) return;

        String lower = userMessage.toLowerCase();
        boolean isWorkspace = "WORKSPACE".equals(channel.getType());

        // In workspace every message goes to FindemWorker; elsewhere only on explicit @mention
        if (isWorkspace) {
            respondAs(channel, AIPersona.FINDEM_WORKER_ID, "FindemWorker",
                buildPrompt(AIPersona.FINDEM_WORKER_ID, channel, userMessage, authorName));
            return;
        }

        if (lower.contains("@findemassist")) {
            respondAs(channel, AIPersona.FINDEM_ASSIST_ID, "FindemAssist",
                buildPrompt(AIPersona.FINDEM_ASSIST_ID, channel, userMessage, authorName));
        }
        if (lower.contains("@findemlooker")) {
            respondAs(channel, AIPersona.FINDEM_LOOKER_ID, "FindemLooker",
                buildPrompt(AIPersona.FINDEM_LOOKER_ID, channel, userMessage, authorName));
        }
        if (lower.contains("@findemworker")) {
            respondAs(channel, AIPersona.FINDEM_WORKER_ID, "FindemWorker",
                buildPrompt(AIPersona.FINDEM_WORKER_ID, channel, userMessage, authorName));
        }
    }

    private void respondAs(TeamChannel channel, UUID personaId, String personaName, String prompt) {
        try {
            String systemPrompt = systemPromptFor(personaId);
            String result = aiClientService.complete(systemPrompt, prompt, 0.5);
            if (result == null || result.isBlank()) return;

            TeamMessage aiMsg = new TeamMessage();
            aiMsg.setChannel(channel);
            aiMsg.setAuthorId(personaId);
            aiMsg.setAuthorName(personaName);
            aiMsg.setAuthorType("AI_SYSTEM");
            aiMsg.setContent(result.strip());
            aiMsg.setMentions("[]");
            aiMsg.setCreatedAt(LocalDateTime.now());
            messageRepository.save(aiMsg);
        } catch (Exception e) {
            log.warn("AI response failed for persona {} in channel {}: {}", personaName, channel.getChannelId(), e.getMessage());
        }
    }

    private String buildPrompt(UUID personaId, TeamChannel channel, String userMessage, String authorName) {
        String jobContext = "";
        if ("OFFER".equals(channel.getType()) && channel.getJobId() != null) {
            jobContext = jobRepository.findById(channel.getJobId())
                .map(j -> "Contexte : cette conversation concerne l'offre \"" + j.getTitle() + "\"" +
                    (j.getLocation() != null ? " basée à " + j.getLocation() : "") + ".\n")
                .orElse("");
        }
        return jobContext + "Message de " + authorName + " : " + userMessage;
    }

    private String systemPromptFor(UUID personaId) {
        if (AIPersona.FINDEM_ASSIST_ID.equals(personaId)) {
            return "Tu es FindemAssist, expert en analyse de candidatures RH. " +
                "Tu fournis des analyses précises, factuelles et actionnables en français. " +
                "Sois concis, structuré, et professionnel. Maximum 200 mots par réponse.";
        }
        if (AIPersona.FINDEM_LOOKER_ID.equals(personaId)) {
            return "Tu es FindemLooker, expert en veille marché RH et stratégie de recrutement. " +
                "Tu apportes des insights sur les tendances du marché, les compétences recherchées, " +
                "et les meilleures pratiques de recrutement. Sois analytique et pragmatique. Maximum 200 mots.";
        }
        return "Tu es FindemWorker, assistant RH opérationnel. " +
            "Tu exécutes les demandes de l'équipe : rédaction d'emails, questions d'entretien, " +
            "résumés de profils, fiches de poste. Tes réponses sont directement utilisables. Maximum 300 mots.";
    }

    // ── Mentions ──────────────────────────────────────────────────────────────

    private String extractMentionsJson(String content) {
        List<String> mentions = new ArrayList<>();
        String lower = content.toLowerCase();
        if (lower.contains("@findemassist")) mentions.add("findemassist");
        if (lower.contains("@findemlooker")) mentions.add("findemlooker");
        if (lower.contains("@findemworker")) mentions.add("findemworker");
        // extract @candidat:xxx patterns
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("@candidat:([\\w-]+)", java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(content);
        while (m.find()) mentions.add("candidat:" + m.group(1));
        return "[" + String.join(",", mentions.stream().map(s -> "\"" + s + "\"").toList()) + "]";
    }
}
