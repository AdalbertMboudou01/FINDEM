package com.memoire.assistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoire.assistant.dto.AnalysisFactDTO;
import com.memoire.assistant.model.ChatAnswer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SemanticExtractionService {

    private static final Logger log = LoggerFactory.getLogger(SemanticExtractionService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.semantic-extractor.enabled:false}")
    private boolean enabled;

    @Value("${app.semantic-extractor.base-url:https://api.openai.com}")
    private String baseUrl;

    @Value("${app.semantic-extractor.api-key:}")
    private String apiKey;

    @Value("${app.semantic-extractor.model:gpt-4o-mini}")
    private String model;

    public List<AnalysisFactDTO> extractFacts(List<ChatAnswer> answers) {
        if (!enabled) {
            log.warn("Semantic extractor disabled by configuration (app.semantic-extractor.enabled=false)");
            return List.of();
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Semantic extractor API key missing; cannot call LLM");
            return List.of();
        }
        if (answers == null || answers.isEmpty()) {
            log.warn("Semantic extractor called with no answers");
            return List.of();
        }

        try {
            String url = baseUrl.endsWith("/") ? baseUrl + "v1/chat/completions" : baseUrl + "/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", model);
            payload.put("temperature", 0);
            payload.put("response_format", Map.of("type", "json_object"));

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of(
                "role", "system",
                "content", "Tu extrais des constats factuels de prequalification. Retourne UNIQUEMENT un JSON valide au format {\"facts\":[...]} avec chaque fait: dimension (motivation|projects|technical|availability|location|general), finding, evidence, confidence(0..1), sourceQuestion."
            ));
            messages.add(Map.of("role", "user", "content", buildAnswersPrompt(answers)));
            payload.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Semantic extractor HTTP failure status={} bodyPresent={}", response.getStatusCode(), response.getBody() != null);
                return List.of();
            }

            String content = extractAssistantContent(response.getBody());
            if (content == null || content.isBlank()) {
                log.warn("Semantic extractor returned empty content from LLM response");
                return List.of();
            }

            List<AnalysisFactDTO> facts = parseFacts(content);
            if (facts.isEmpty()) {
                log.warn("Semantic extractor parsed zero valid facts from LLM payload");
            }
            return facts;
        } catch (Exception e) {
            log.warn("Semantic extractor call failed: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildAnswersPrompt(List<ChatAnswer> answers) {
        StringBuilder sb = new StringBuilder();
        sb.append("Reponses candidat:\n");
        int index = 1;
        for (ChatAnswer answer : answers) {
            String q = answer.getQuestionText() == null ? "" : answer.getQuestionText().trim();
            String a = answer.getAnswerText() == null ? "" : answer.getAnswerText().trim();
            if (a.isBlank()) {
                continue;
            }
            sb.append(index++)
                .append(") Q: ")
                .append(q)
                .append("\n")
                .append("A: ")
                .append(a)
                .append("\n\n");
        }

        sb.append("Consignes:\n")
            .append("- Ne pas inventer.\n")
            .append("- Chaque fact doit contenir une evidence textuelle citee depuis la reponse.\n")
            .append("- confidence entre 0 et 1.\n")
            .append("- Max 8 facts.\n");

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String extractAssistantContent(Map<String, Object> body) {
        Object choicesObj = body.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            return null;
        }

        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map<?, ?> choiceMap)) {
            return null;
        }

        Object messageObj = choiceMap.get("message");
        if (!(messageObj instanceof Map<?, ?> messageMap)) {
            return null;
        }

        Object contentObj = messageMap.get("content");
        if (contentObj == null) {
            return null;
        }

        return stripCodeFences(String.valueOf(contentObj));
    }

    private List<AnalysisFactDTO> parseFacts(String jsonContent) {
        try {
            JsonNode root = objectMapper.readTree(jsonContent);
            JsonNode factsNode = root.get("facts");
            if (factsNode == null || !factsNode.isArray()) {
                return List.of();
            }

            List<AnalysisFactDTO> facts = new ArrayList<>();
            for (JsonNode node : factsNode) {
                String dimension = readText(node, "dimension", "general");
                String finding = readText(node, "finding", "");
                String evidence = readText(node, "evidence", "");
                String sourceQuestion = readText(node, "sourceQuestion", "");
                double confidence = clamp(node.path("confidence").asDouble(0.6));

                if (finding.isBlank() || evidence.isBlank()) {
                    continue;
                }

                facts.add(new AnalysisFactDTO(dimension, finding, evidence, confidence, sourceQuestion));
            }
            return facts;
        } catch (Exception e) {
            log.warn("Semantic extractor JSON parsing failed: {}", e.getMessage());
            return List.of();
        }
    }

    private String readText(JsonNode node, String field, String fallback) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return fallback;
        }
        String text = value.asText("").trim();
        return text.isBlank() ? fallback : text;
    }

    private String stripCodeFences(String content) {
        String text = content.trim();
        if (text.startsWith("```") && text.endsWith("```")) {
            text = text.substring(3, text.length() - 3).trim();
            if (text.startsWith("json")) {
                text = text.substring(4).trim();
            }
        }
        return text;
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
