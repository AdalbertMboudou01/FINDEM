package com.memoire.assistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoire.assistant.dto.AnalysisFactDTO;
import com.memoire.assistant.dto.ChatAnswerAnalysisDTO;
import com.memoire.assistant.model.Application;
import com.memoire.assistant.model.ChatAnswer;
import com.memoire.assistant.model.Job;
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
import java.util.Optional;

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

    // ─────────────────────────────────────────────────────────────────────────
    // Analyse complète façon recruteur humain — méthode principale
    // ─────────────────────────────────────────────────────────────────────────

    public Optional<ChatAnswerAnalysisDTO> analyzeApplication(
            List<ChatAnswer> answers, Job job, Application application) {

        if (!enabled) {
            log.warn("Semantic extractor disabled; skipping full AI analysis");
            return Optional.empty();
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Semantic extractor API key missing; skipping full AI analysis");
            return Optional.empty();
        }
        if (answers == null || answers.isEmpty()) {
            log.warn("Full AI analysis called with no answers");
            return Optional.empty();
        }

        try {
            String url = baseUrl.endsWith("/")
                    ? baseUrl + "v1/chat/completions"
                    : baseUrl + "/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", model);
            payload.put("temperature", 0);
            payload.put("response_format", Map.of("type", "json_object"));

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", buildFullAnalysisSystemPrompt()));
            messages.add(Map.of("role", "user",   "content", buildFullAnalysisPrompt(answers, job, application)));
            payload.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Full AI analysis HTTP failure status={}", response.getStatusCode());
                return Optional.empty();
            }

            String content = extractAssistantContent(response.getBody());
            if (content == null || content.isBlank()) {
                log.warn("Full AI analysis returned empty content");
                return Optional.empty();
            }

            Optional<ChatAnswerAnalysisDTO> result = parseAnalysis(content);
            result.ifPresent(dto -> log.info("Full AI analysis succeeded model={} application={}",
                    model, application != null ? application.getApplicationId() : "unknown"));
            return result;

        } catch (Exception e) {
            log.warn("Full AI analysis failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Prompt système — philosophie recruteur humain
    // ─────────────────────────────────────────────────────────────────────────

    private String buildFullAnalysisSystemPrompt() {
        return """
Tu es un recruteur sénior avec 15 ans d'expérience en recrutement tech et ressources humaines.
Ta mission : évaluer la candidature ci-dessous pour le poste indiqué, EXACTEMENT comme tu le ferais avec un vrai dossier sur ton bureau.

TA PHILOSOPHIE D'ÉVALUATION :
- Lis et COMPRENDS les réponses comme un humain. Ne cherche PAS des mots-clés exacts.
- Un candidat peut maîtriser React sans jamais écrire "React" : il peut décrire des composants réutilisables, du state management, du rendu conditionnel. C'est à toi de comprendre.
- Adapte ton jugement AU POSTE SPÉCIFIQUE. Une bonne motivation pour un poste DevOps n'est pas la même que pour un poste data analyst.
- Une réponse vague ou générique ("je cherche un emploi", "je suis motivé") n'est PAS une vraie motivation pour CE poste. Dis-le clairement.
- Si le candidat n'a pas répondu à une dimension, indique-le honnêtement plutôt que de l'inventer.
- Base-toi UNIQUEMENT sur ce que le candidat a écrit. Pas d'extrapolation, pas de suppositions.
- Un candidat junior bien préparé peut valoir plus qu'un sénior qui répond à côté.
- Signale les incohérences entre les réponses si tu en détectes.

FORMAT DE SORTIE :
Retourne UNIQUEMENT un objet JSON valide, sans markdown, sans commentaire, avec cette structure exacte :

{
  "motivationLevel": "HIGH|MEDIUM|LOW",
  "motivationReasoning": "Explication précise : qu'est-ce qui dans les réponses justifie ce niveau ? Cite des éléments concrets.",
  "motivationAssessment": "Phrase synthèse courte pour le recruteur (1-2 lignes)",

  "technicalLevel": "STRONG|MEDIUM|WEAK",
  "technicalReasoning": "Quelles compétences sont démontrées ? Lesquelles manquent pour CE poste spécifique ?",
  "technicalSkills": ["compétence démontrée 1", "compétence démontrée 2"],
  "mentionedProjects": ["Description courte du projet ou réalisation mentionné"],

  "experienceLevel": "SENIOR|INTERMEDIATE|JUNIOR",
  "experienceReasoning": "Justification du niveau estimé basée sur les réponses (durée, contexte, responsabilités).",

  "jobMatchLevel": "HIGH|MEDIUM|LOW",
  "jobMatchReasoning": "En quoi le profil répond ou non aux besoins SPÉCIFIQUES du poste ? Sois précis sur les points de convergence et les écarts.",
  "matchedAspects": ["Aspect du poste que le candidat couvre clairement"],
  "gapsIdentified": ["Lacune ou manque identifié par rapport aux attentes du poste"],

  "availabilityStatus": "IMMEDIATE|FUTURE|UNSPECIFIED",
  "availabilityReasoning": "Ce que le candidat a exprimé sur sa disponibilité. UNSPECIFIED si rien n'a été dit.",

  "locationMatch": "PERFECT|REMOTE_COMPATIBLE|INCOMPATIBLE",
  "locationReasoning": "Analyse de la compatibilité géographique entre le candidat et le poste.",

  "completenessScore": 0.75,
  "completenessReasoning": "Évalue la qualité et la richesse globale des réponses par rapport aux attentes du poste. 1.0 = répond parfaitement à tout ce qu'on attend, 0.0 = réponses vides ou hors sujet.",

  "overallAssessment": "Synthèse de 2-3 phrases sur ce candidat pour CE poste. Ce que tu dirais à un collègue recruteur qui te demande 'alors ce profil ?'",

  "strengths": ["Point fort concret et spécifique au poste", "Autre point fort"],
  "concerns": ["Point d'attention réel basé sur les réponses", "Autre point d'attention"],
  "followUpQuestions": ["Question précise et utile à poser au candidat lors d'un entretien"],
  "recruiterGuidance": "Conseil actionnable et concret pour la prochaine étape du recrutement.",

  "recommendedAction": "PRIORITY|REVIEW|REJECT",
  "recommendationReasoning": "Justification claire et honnête de cette recommandation. PRIORITY = profil convaincant qui mérite d'être vu en priorité. REVIEW = profil intéressant mais des zones d'ombre à éclaircir. REJECT = profil insuffisant ou clairement inadapté à CE poste."
}
""";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Prompt utilisateur — contexte complet du poste + réponses du candidat
    // ─────────────────────────────────────────────────────────────────────────

    private String buildFullAnalysisPrompt(List<ChatAnswer> answers, Job job, Application application) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== FICHE DE POSTE ===\n");
        if (job != null) {
            sb.append("Titre : ").append(nvl(job.getTitle())).append("\n");

            if (isPresent(job.getDescription())) {
                sb.append("Description : ").append(truncate(job.getDescription(), 700)).append("\n");
            }
            if (isPresent(job.getContextePoste())) {
                sb.append("Contexte du poste : ").append(truncate(job.getContextePoste(), 500)).append("\n");
            }
            if (isPresent(job.getMissionsDetaillees())) {
                sb.append("Missions détaillées : ").append(truncate(job.getMissionsDetaillees(), 600)).append("\n");
            }
            if (job.getTechnologies() != null && !job.getTechnologies().isEmpty()) {
                sb.append("Technologies / compétences attendues : ")
                  .append(String.join(", ", job.getTechnologies())).append("\n");
            }
            sb.append("Localisation : ").append(nvl(job.getLocation())).append("\n");
            if (isPresent(job.getAlternanceRhythm())) {
                sb.append("Rythme / type de contrat : ").append(job.getAlternanceRhythm()).append("\n");
            }
            if (isPresent(job.getServiceEntreprise())) {
                sb.append("Service / département : ").append(job.getServiceEntreprise()).append("\n");
            }
        } else {
            sb.append("(Fiche de poste non disponible — évalue le profil global du candidat)\n");
        }

        if (application != null && application.getCandidate() != null) {
            String loc = application.getCandidate().getLocation();
            if (isPresent(loc)) {
                sb.append("\n=== PROFIL CANDIDAT ===\n");
                sb.append("Localisation déclarée : ").append(loc).append("\n");
            }
        }

        sb.append("\n=== RÉPONSES DU CANDIDAT AU QUESTIONNAIRE ===\n");
        int index = 1;
        boolean hasContent = false;
        for (ChatAnswer answer : answers) {
            String q = answer.getQuestionText() == null ? "" : answer.getQuestionText().trim();
            String a = answer.getAnswerText()  == null ? "" : answer.getAnswerText().trim();
            if (!isMeaningfulAnswer(a)) continue;
            hasContent = true;
            sb.append(index++).append(") Question : ").append(q).append("\n");
            sb.append("   Réponse  : ").append(a).append("\n\n");
        }
        if (!hasContent) {
            sb.append("(Aucune réponse exploitable fournie par le candidat)\n");
        }

        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Parsing de la réponse LLM → ChatAnswerAnalysisDTO
    // ─────────────────────────────────────────────────────────────────────────

    private Optional<ChatAnswerAnalysisDTO> parseAnalysis(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            ChatAnswerAnalysisDTO dto = new ChatAnswerAnalysisDTO();

            // Motivation
            dto.setMotivationLevel(validEnum(root, "motivationLevel",
                    List.of("HIGH", "MEDIUM", "LOW"), "MEDIUM"));
            dto.setMotivationReasoning(text(root, "motivationReasoning"));
            dto.setMotivationAssessment(text(root, "motivationAssessment"));
            dto.setMotivationSummary(text(root, "motivationAssessment")); // compat
            dto.setHasSpecificMotivation(!"LOW".equals(dto.getMotivationLevel()));
            dto.setMotivationKeywords(List.of());

            // Technique
            dto.setTechnicalLevel(validEnum(root, "technicalLevel",
                    List.of("STRONG", "MEDIUM", "WEAK"), "MEDIUM"));
            dto.setTechnicalReasoning(text(root, "technicalReasoning"));
            dto.setTechnicalSkills(stringList(root, "technicalSkills"));
            dto.setMentionedProjects(stringList(root, "mentionedProjects"));
            dto.setHasProjectDetails(!dto.getMentionedProjects().isEmpty());

            // Expérience
            dto.setExperienceLevel(validEnum(root, "experienceLevel",
                    List.of("SENIOR", "INTERMEDIATE", "JUNIOR"), "JUNIOR"));
            dto.setExperienceReasoning(text(root, "experienceReasoning"));

            // Adéquation au poste
            dto.setJobMatchLevel(validEnum(root, "jobMatchLevel",
                    List.of("HIGH", "MEDIUM", "LOW"), "MEDIUM"));
            dto.setJobMatchReasoning(text(root, "jobMatchReasoning"));
            dto.setMatchedJobTechnologies(stringList(root, "matchedAspects"));
            dto.setMissingJobTechnologies(stringList(root, "gapsIdentified"));

            // Disponibilité
            dto.setAvailabilityStatus(validEnum(root, "availabilityStatus",
                    List.of("IMMEDIATE", "FUTURE", "UNSPECIFIED"), "UNSPECIFIED"));
            dto.setAvailabilityReasoning(text(root, "availabilityReasoning"));
            dto.setHasClearAvailability(!"UNSPECIFIED".equals(dto.getAvailabilityStatus()));
            dto.setAvailabilityAssessment(text(root, "availabilityReasoning"));

            // Localisation
            dto.setLocationMatch(validEnum(root, "locationMatch",
                    List.of("PERFECT", "REMOTE_COMPATIBLE", "INCOMPATIBLE"), "REMOTE_COMPATIBLE"));
            dto.setLocationReasoning(text(root, "locationReasoning"));
            dto.setHasMobility(!"INCOMPATIBLE".equals(dto.getLocationMatch()));
            dto.setLocationAssessment(text(root, "locationReasoning"));

            // Score de complétude
            JsonNode scoreNode = root.get("completenessScore");
            double score = scoreNode != null ? Math.max(0.0, Math.min(1.0, scoreNode.asDouble(0.5))) : 0.5;
            dto.setCompletenessScore(score);

            // Synthèse globale
            dto.setOverallAssessment(text(root, "overallAssessment"));
            dto.setRecommendationReasoning(text(root, "recommendationReasoning"));

            // Points forts, points d'attention, questions de suivi
            dto.setStrengths(stringList(root, "strengths"));
            dto.setPointsToConfirm(stringList(root, "concerns"));
            dto.setFollowUpQuestions(stringList(root, "followUpQuestions"));
            dto.setRecruiterGuidance(text(root, "recruiterGuidance"));

            // Manques = écarts identifiés par le LLM
            dto.setMissingInformation(stringList(root, "gapsIdentified"));
            dto.setInconsistencies(List.of());

            // Recommandation finale
            dto.setRecommendedAction(validEnum(root, "recommendedAction",
                    List.of("PRIORITY", "REVIEW", "REJECT"), "REVIEW"));

            // GitHub / portfolio — détecté séparément par l'enrichissement heuristique
            dto.setHasGitHubOrPortfolio(false);

            return Optional.of(dto);

        } catch (Exception e) {
            log.warn("Full AI analysis JSON parsing failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rédaction assistée d'une fiche de poste
    // ─────────────────────────────────────────────────────────────────────────

    public Optional<Map<String, Object>> draftJobOffer(
            String title, String location, String rhythm, String userContext) {

        if (!enabled || apiKey == null || apiKey.isBlank()) {
            log.warn("Semantic extractor disabled or API key missing; cannot draft job offer");
            return Optional.empty();
        }
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }

        try {
            String url = baseUrl.endsWith("/")
                    ? baseUrl + "v1/chat/completions"
                    : baseUrl + "/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", model);
            payload.put("temperature", 0.4);
            payload.put("response_format", Map.of("type", "json_object"));

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", buildJobDraftSystemPrompt()));
            messages.add(Map.of("role", "user",   "content", buildJobDraftPrompt(title, location, rhythm, userContext)));
            payload.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Job draft AI HTTP failure status={}", response.getStatusCode());
                return Optional.empty();
            }

            String content = extractAssistantContent(response.getBody());
            if (content == null || content.isBlank()) return Optional.empty();

            return parseJobDraft(content);

        } catch (Exception e) {
            log.warn("Job draft AI call failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String buildJobDraftSystemPrompt() {
        return """
Tu es un expert RH avec 15 ans d'expérience en recrutement tech et en rédaction de fiches de poste.
Ta mission : rédiger une fiche de poste complète, professionnelle et engageante pour une alternance.

Règles :
- Adapte le ton et le contenu au monde de l'alternance (formation, progression, encadrement).
- Sois précis, concret et attractif : le candidat doit comprendre exactement ce qu'on attend de lui.
- La description doit donner envie de postuler.
- Les missions doivent être spécifiques au poste (pas génériques).
- Les technologies doivent correspondre réellement au poste décrit.
- Rédige en français professionnel.

Retourne UNIQUEMENT un objet JSON valide avec cette structure :
{
  "description": "Description complète et attractive du poste (5-8 lignes)",
  "contextePoste": "Contexte de l'équipe, de l'entreprise et de l'environnement de travail (3-5 lignes)",
  "missionsDetaillees": "Liste des missions principales sous forme de paragraphe structuré (5-8 missions concrètes)",
  "technologies": ["tech1", "tech2", "tech3"]
}
""";
    }

    private String buildJobDraftPrompt(String title, String location, String rhythm, String userContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("Poste : ").append(title).append("\n");
        if (isPresent(location))     sb.append("Localisation : ").append(location).append("\n");
        if (isPresent(rhythm))       sb.append("Durée / rythme : ").append(rhythm).append("\n");
        if (isPresent(userContext))  sb.append("Informations complémentaires : ").append(userContext).append("\n");
        sb.append("\nRédige la fiche de poste complète pour cette alternance.");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Optional<Map<String, Object>> parseJobDraft(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            Map<String, Object> result = new HashMap<>();
            result.put("description",       text(root, "description"));
            result.put("contextePoste",      text(root, "contextePoste"));
            result.put("missionsDetaillees", text(root, "missionsDetaillees"));

            JsonNode techNode = root.get("technologies");
            List<String> techs = new ArrayList<>();
            if (techNode != null && techNode.isArray()) {
                for (JsonNode t : techNode) {
                    String val = t.asText("").trim();
                    if (!val.isBlank()) techs.add(val);
                }
            }
            result.put("technologies", techs);
            return Optional.of(result);
        } catch (Exception e) {
            log.warn("Job draft JSON parsing failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // extractFacts() — conservé pour le fallback heuristique existant
    // ─────────────────────────────────────────────────────────────────────────

    public List<AnalysisFactDTO> extractFacts(List<ChatAnswer> answers) {
        return extractFacts(answers, null);
    }

    public List<AnalysisFactDTO> extractFacts(List<ChatAnswer> answers, Job job) {
        if (!enabled) {
            log.warn("Semantic extractor disabled by configuration");
            return List.of();
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Semantic extractor API key missing");
            return List.of();
        }
        if (answers == null || answers.isEmpty()) {
            return List.of();
        }

        try {
            String url = baseUrl.endsWith("/")
                    ? baseUrl + "v1/chat/completions"
                    : baseUrl + "/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", model);
            payload.put("temperature", 0);
            payload.put("response_format", Map.of("type", "json_object"));

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", buildSystemPrompt()));
            messages.add(Map.of("role", "user",   "content", buildEvaluationPrompt(answers, job)));
            payload.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return List.of();
            }

            String content = extractAssistantContent(response.getBody());
            if (content == null || content.isBlank()) return List.of();

            return parseFacts(content);
        } catch (Exception e) {
            log.warn("Semantic extractor call failed: {}", e.getMessage());
            return List.of();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String validEnum(JsonNode root, String field, List<String> valid, String fallback) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) return fallback;
        String val = node.asText("").trim().toUpperCase();
        return valid.contains(val) ? val : fallback;
    }

    private String text(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) return "";
        return node.asText("").trim();
    }

    private List<String> stringList(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || !node.isArray()) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (JsonNode item : node) {
            String t = item.asText("").trim();
            if (!t.isBlank()) result.add(t);
        }
        return result;
    }

    private boolean isPresent(String s) {
        return s != null && !s.isBlank();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    private static final int MIN_MEANINGFUL_LENGTH = 20;

    private boolean isMeaningfulAnswer(String answer) {
        if (answer == null || answer.isBlank()) return false;
        String cleaned = answer.replaceAll("[^\\p{L}\\p{N}]", "");
        return cleaned.length() >= MIN_MEANINGFUL_LENGTH;
    }

    private String nvl(String s) {
        return s == null || s.isBlank() ? "Non précisé" : s;
    }

    private String extractAssistantContent(Map<String, Object> body) {
        Object choicesObj = body.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) return null;
        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map<?, ?> choiceMap)) return null;
        Object messageObj = choiceMap.get("message");
        if (!(messageObj instanceof Map<?, ?> messageMap)) return null;
        Object contentObj = messageMap.get("content");
        if (contentObj == null) return null;
        return stripCodeFences(String.valueOf(contentObj));
    }

    private String stripCodeFences(String content) {
        String text = content.trim();
        if (text.startsWith("```") && text.endsWith("```")) {
            text = text.substring(3, text.length() - 3).trim();
            if (text.startsWith("json")) text = text.substring(4).trim();
        }
        return text;
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Anciens prompts conservés pour extractFacts() (fallback)
    // ─────────────────────────────────────────────────────────────────────────

    private String buildSystemPrompt() {
        return "Tu es un assistant de pré-qualification pour recruteurs. " +
               "Retourne UNIQUEMENT un JSON valide au format {\"facts\":[...]}. " +
               "Chaque fait : dimension (motivation|technical|projects|availability|location|general), " +
               "finding (constat factuel), evidence (citation exacte), " +
               "fit_score (0.0=non adapté..1.0=très adapté), confidence (0.0..1.0), sourceQuestion.";
    }

    private String buildEvaluationPrompt(List<ChatAnswer> answers, Job job) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== POSTE ===\n");
        if (job != null) {
            sb.append("Titre: ").append(nvl(job.getTitle())).append("\n");
            sb.append("Localisation: ").append(nvl(job.getLocation())).append("\n");
            if (job.getTechnologies() != null && !job.getTechnologies().isEmpty()) {
                sb.append("Technologies: ").append(String.join(", ", job.getTechnologies())).append("\n");
            }
            if (isPresent(job.getDescription())) {
                sb.append("Description: ").append(truncate(job.getDescription(), 400)).append("\n");
            }
        }
        sb.append("\n=== RÉPONSES ===\n");
        int i = 1;
        for (ChatAnswer answer : answers) {
            String a = answer.getAnswerText() == null ? "" : answer.getAnswerText().trim();
            if (!isMeaningfulAnswer(a)) continue;
            sb.append(i++).append(") Q: ").append(nvl(answer.getQuestionText()))
              .append("\n   A: ").append(a).append("\n\n");
        }
        sb.append("Max 8 facts. {\"facts\":[]} si rien d'exploitable.\n");
        return sb.toString();
    }

    private List<AnalysisFactDTO> parseFacts(String jsonContent) {
        try {
            JsonNode root = objectMapper.readTree(jsonContent);
            JsonNode factsNode = root.get("facts");
            if (factsNode == null || !factsNode.isArray()) return List.of();
            List<AnalysisFactDTO> facts = new ArrayList<>();
            for (JsonNode node : factsNode) {
                String dimension     = text(node, "dimension");
                String finding       = text(node, "finding");
                String evidence      = text(node, "evidence");
                String sourceQuestion = text(node, "sourceQuestion");
                double confidence    = clamp(node.path("confidence").asDouble(0.6));
                if (finding.isBlank() || evidence.isBlank()) continue;
                facts.add(new AnalysisFactDTO(dimension.isBlank() ? "general" : dimension,
                        finding, evidence, confidence, sourceQuestion));
            }
            return facts;
        } catch (Exception e) {
            log.warn("Semantic extractor JSON parsing failed: {}", e.getMessage());
            return List.of();
        }
    }

}
