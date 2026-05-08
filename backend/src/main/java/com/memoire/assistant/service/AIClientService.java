package com.memoire.assistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AIClientService {

    private static final Logger log = LoggerFactory.getLogger(AIClientService.class);

    @Value("${app.ai.enabled:${app.semantic-extractor.enabled:false}}")
    private boolean enabled;

    @Value("${app.ai.base-url:${app.semantic-extractor.base-url:https://api.openai.com}}")
    private String baseUrl;

    @Value("${app.ai.api-key:${app.semantic-extractor.api-key:}}")
    private String apiKey;

    @Value("${app.ai.model:${app.semantic-extractor.model:gpt-4o-mini}}")
    private String model;

    @Autowired
    private RestTemplate restTemplate;

    public boolean isEnabled() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    public String complete(String userPrompt, double temperature) {
        return complete(null, userPrompt, temperature);
    }

    @SuppressWarnings("unchecked")
    public String complete(String systemPrompt, String userPrompt, double temperature) {
        if (!isEnabled()) {
            return null;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            List<Map<String, String>> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(Map.of("role", "system", "content", systemPrompt));
            }
            messages.add(Map.of("role", "user", "content", userPrompt));

            Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", messages,
                "temperature", temperature
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            Map<String, Object> response = restTemplate.postForObject(chatCompletionsUrl(), request, Map.class);
            if (response == null) {
                return null;
            }

            Object choicesObj = response.get("choices");
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

            Object content = messageMap.get("content");
            return content != null ? String.valueOf(content) : null;
        } catch (Exception e) {
            log.warn("AI completion failed: {}", e.getMessage());
            return null;
        }
    }

    private String chatCompletionsUrl() {
        return baseUrl.endsWith("/") ? baseUrl + "v1/chat/completions" : baseUrl + "/v1/chat/completions";
    }
}
