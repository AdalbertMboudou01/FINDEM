package com.memoire.assistant.service;

import com.memoire.assistant.dto.ChatAnswerAnalysisDTO;
import com.memoire.assistant.dto.AnalysisFactDTO;
import com.memoire.assistant.model.Application;
import com.memoire.assistant.model.Candidate;
import com.memoire.assistant.model.ChatAnswer;
import com.memoire.assistant.model.Job;
import com.memoire.assistant.repository.CandidateRepository;
import com.memoire.assistant.repository.ChatAnswerRepository;
import com.memoire.assistant.repository.ApplicationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Service
public class ChatAnswerService {

    private static final Logger log = LoggerFactory.getLogger(ChatAnswerService.class);
    
    @Autowired
    private ChatAnswerRepository chatAnswerRepository;
    
    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private SemanticExtractionService semanticExtractionService;

    @Lazy
    @Autowired
    private AIFindAssistService aiFindemAssistService;

    @Value("${app.semantic-extractor.require-llm:false}")
    private boolean requireLlmExtraction;
    
    private static final Pattern TOKEN_SPLIT_PATTERN = Pattern.compile("[,;/\\n]");
    private static final Pattern YEARS_EXPERIENCE_PATTERN = Pattern.compile("(\\d{1,2})\\s*(ans|an)");
    private static final List<String> MOTIVATION_MARKERS = List.of(
        "motivation", "motive", "envie", "interet", "passion", "apprendre", "progress", "evoluer", "mission", "entreprise", "poste"
    );
    private static final List<String> GENERIC_MOTIVATION_PATTERNS = List.of(
        "je suis motive", "je suis motivee", "je cherche un travail", "je veux un travail", "je suis interesse"
    );
    private static final List<String> TECH_MARKERS = List.of(
        "java", "spring", "python", "django", "flask", "node", "express", "react", "vue", "angular", "typescript",
        "javascript", "sql", "postgres", "mysql", "docker", "kubernetes", "aws", "gcp", "rest", "api", "ci/cd"
    );
    
    /**
     * Analyse complète des réponses du chatbot pour une candidature
     */
    public ChatAnswerAnalysisDTO analyzeChatAnswers(UUID applicationId) {
        Application application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new RuntimeException("Candidature non trouvée"));

        // Return from DB cache if fresh (< 24h)
        if (application.getSemanticCache() != null && application.getSemanticCacheAt() != null) {
            Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
            if (application.getSemanticCacheAt().isAfter(cutoff)) {
                try {
                    return OBJECT_MAPPER.readValue(application.getSemanticCache(), ChatAnswerAnalysisDTO.class);
                } catch (Exception e) {
                    log.warn("Semantic cache deserialization failed for {}, re-computing: {}", applicationId, e.getMessage());
                }
            }
        }

        List<ChatAnswer> answers = chatAnswerRepository.findByApplication_ApplicationId(applicationId);
        ChatAnswerAnalysisDTO result = analyzeApplicationWithAnswers(application, answers, true);

        // Persist cache
        try {
            application.setSemanticCache(OBJECT_MAPPER.writeValueAsString(result));
            application.setSemanticCacheAt(Instant.now());
            applicationRepository.save(application);
        } catch (Exception e) {
            log.warn("Could not save semantic cache for {}: {}", applicationId, e.getMessage());
        }

        // FindemAssist : poster l'analyse initiale dans le fil (une seule fois)
        aiFindemAssistService.postAnalysisIfNeeded(applicationId, result);

        return result;
    }

    /**
     * Analyse batch : une seule requête DB pour toutes les candidatures, puis traitement par candidature.
     * Retourne une map applicationId -> analyse.
     */
    public Map<UUID, ChatAnswerAnalysisDTO> analyzeChatAnswersBatch(List<UUID> applicationIds) {
        if (applicationIds == null || applicationIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 1 seule requête DB pour toutes les réponses
        List<ChatAnswer> allAnswers = chatAnswerRepository.findByApplication_ApplicationIdIn(applicationIds);

        // Regrouper par applicationId
        Map<UUID, List<ChatAnswer>> answersByApp = allAnswers.stream()
            .filter(ca -> ca.getApplication() != null && ca.getApplication().getApplicationId() != null)
            .collect(Collectors.groupingBy(ca -> ca.getApplication().getApplicationId()));

        // 1 seule requête DB pour toutes les applications
        List<Application> applications = applicationRepository.findAllById(applicationIds);
        Map<UUID, Application> appById = applications.stream()
            .collect(Collectors.toMap(Application::getApplicationId, a -> a));

        Map<UUID, ChatAnswerAnalysisDTO> result = new HashMap<>();
        for (UUID appId : applicationIds) {
            Application application = appById.get(appId);
            if (application == null) continue;
            List<ChatAnswer> answers = answersByApp.getOrDefault(appId, Collections.emptyList());
            if (answers.isEmpty()) continue;
            try {
                // Batch mode: rules-based only (no LLM), so the list loads instantly
                result.put(appId, analyzeApplicationWithAnswers(application, answers, false));
            } catch (Exception e) {
                log.warn("Analyse batch ignoree pour {}: {}", appId, e.getMessage());
            }
        }
        return result;
    }

    private ChatAnswerAnalysisDTO analyzeApplicationWithAnswers(Application application, List<ChatAnswer> answers, boolean includeLlm) {
        ChatAnswerAnalysisDTO analysis = new ChatAnswerAnalysisDTO(application.getApplicationId().toString());

        analyzeMotivation(answers, analysis);
        analyzeTechnicalProfile(answers, analysis);
        analyzeAvailability(answers, analysis);
        analyzeLocation(answers, analysis, application);
        if (includeLlm) {
            extractConstrainedSemanticFacts(answers, analysis);
        }
        calculateCompletenessScore(answers, analysis);
        detectInconsistencies(answers, analysis);
        buildQualitativeSummary(analysis);

        return analysis;
    }
    
    /**
     * Analyse de la motivation du candidat
     */
    private void analyzeMotivation(List<ChatAnswer> answers, ChatAnswerAnalysisDTO analysis) {
        List<ChatAnswer> motivationAnswers = answers.stream()
            .filter(this::isMotivationAnswer)
            .collect(Collectors.toList());

        if (motivationAnswers.isEmpty()) {
            analysis.setMotivationLevel("LOW");
            analysis.setHasSpecificMotivation(false);
            analysis.setMotivationSummary("Motivation non fournie.");
            analysis.setMotivationKeywords(Collections.emptyList());
            return;
        }

        String motivationText = motivationAnswers.stream()
            .map(ChatAnswer::getAnswerText)
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(text -> !text.isBlank())
            .collect(Collectors.joining(" "));

        String normalized = normalizeForSearch(motivationText);
        int score = 0;
        if (!motivationText.isBlank()) {
            score += 1;
        }
        if (motivationText.length() >= 80) {
            score += 1;
        }
        if (motivationText.length() >= 180) {
            score += 1;
        }

        List<String> detectedMarkers = MOTIVATION_MARKERS.stream()
            .filter(normalized::contains)
            .collect(Collectors.toList());
        if (detectedMarkers.size() >= 2) {
            score += 1;
        }
        if (detectedMarkers.size() >= 5) {
            score += 1;
        }

        boolean genericOnly = GENERIC_MOTIVATION_PATTERNS.stream().anyMatch(normalized::contains)
            && detectedMarkers.size() <= 2
            && motivationText.length() < 90;
        if (genericOnly) {
            score -= 1;
        }

        String level = score >= 4 ? "HIGH" : (score >= 2 ? "MEDIUM" : "LOW");
        analysis.setMotivationLevel(level);
        analysis.setHasSpecificMotivation(!genericOnly && !motivationText.isBlank());
        analysis.setMotivationSummary(generateMotivationSummary(motivationText));
        analysis.setMotivationKeywords(detectedMarkers.stream().distinct().collect(Collectors.toList()));
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Analyse du profil technique et des projets
     */
    private void analyzeTechnicalProfile(List<ChatAnswer> answers, ChatAnswerAnalysisDTO analysis) {
        List<String> technicalSkills = new ArrayList<>();
        List<String> mentionedProjects = new ArrayList<>();
        boolean hasProjectDetails = false;
        boolean hasGitHubOrPortfolio = false;
        int longProjectDescriptions = 0;
        
        for (ChatAnswer answer : answers) {
            String raw = answer.getAnswerText() == null ? "" : answer.getAnswerText().trim();
            String text = normalizeForSearch(raw);

            if (isTechnicalAnswer(answer)) {
                technicalSkills.addAll(extractListedItems(raw));
                technicalSkills.addAll(extractTechMarkers(text));
            }

            if (isProjectAnswer(answer)) {
                if (!raw.isBlank()) {
                    hasProjectDetails = true;
                    mentionedProjects.add(truncateEvidence(raw));
                    if (raw.length() >= 80) {
                        longProjectDescriptions++;
                    }
                }
            }

            if (text.contains("github.com") || text.contains("portfolio") || text.contains("gitlab") || text.contains("bitbucket")) {
                hasGitHubOrPortfolio = true;
            }
        }

        int distinctSkills = (int) technicalSkills.stream().filter(v -> !v.isBlank()).map(this::normalizeForSearch).distinct().count();
        int score = 0;
        score += Math.min(4, distinctSkills);
        if (hasProjectDetails) {
            score += 2;
        }
        if (longProjectDescriptions > 0) {
            score += 1;
        }
        if (hasGitHubOrPortfolio) {
            score += 1;
        }

        String technicalLevel = score >= 6 ? "STRONG" : (score >= 3 ? "MEDIUM" : "WEAK");
        analysis.setTechnicalLevel(technicalLevel);
        
        analysis.setTechnicalSkills(technicalSkills.stream().distinct().collect(Collectors.toList()));
        analysis.setMentionedProjects(mentionedProjects.stream().filter(v -> !v.isBlank()).distinct().collect(Collectors.toList()));
        analysis.setHasProjectDetails(hasProjectDetails);
        analysis.setHasGitHubOrPortfolio(hasGitHubOrPortfolio);
    }
    
    /**
     * Analyse de la disponibilité et du rythme d'alternance
     */
    private void analyzeAvailability(List<ChatAnswer> answers, ChatAnswerAnalysisDTO analysis) {
        List<String> availabilityTexts = answers.stream()
            .filter(this::isAvailabilityAnswer)
            .map(ChatAnswer::getAnswerText)
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(v -> !v.isBlank())
            .collect(Collectors.toList());

        List<String> rhythmTexts = answers.stream()
            .filter(this::isRhythmAnswer)
            .map(ChatAnswer::getAnswerText)
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(v -> !v.isBlank())
            .collect(Collectors.toList());

        String combined = normalizeForSearch(String.join(" ", availabilityTexts) + " " + String.join(" ", rhythmTexts));

        String availabilityStatus = "UNSPECIFIED";
        if (!combined.isBlank()) {
            if (containsAny(combined, List.of("immediat", "des maintenant", "tout de suite", "de suite", "asap"))) {
                availabilityStatus = "IMMEDIATE";
            } else if (containsAny(combined, List.of("dans", "a partir", "mois", "semaine", "septembre", "octobre", "janvier", "fevrier", "mars", "avril", "mai", "juin", "juillet", "aout", "novembre", "decembre"))) {
                availabilityStatus = "FUTURE";
            } else {
                availabilityStatus = "FUTURE";
            }
        }

        String alternanceRhythm = "FLEXIBLE";
        if (containsAny(combined, List.of("temps plein", "full time", "5 jours", "5j", "plein temps"))) {
            alternanceRhythm = "FULL_TIME";
        } else if (containsAny(combined, List.of("alternance", "2 jours", "3 jours", "4 jours", "1j1s", "2j2s", "3j2s", "4j1s", "rythme"))) {
            alternanceRhythm = "PART_TIME";
        }

        analysis.setAvailabilityStatus(availabilityStatus);
        analysis.setAlternanceRhythm(alternanceRhythm);
        analysis.setHasClearAvailability(!availabilityTexts.isEmpty() || !rhythmTexts.isEmpty());
    }
    
    /**
     * Analyse de la localisation
     */
    private void analyzeLocation(List<ChatAnswer> answers, ChatAnswerAnalysisDTO analysis, Application application) {
        List<String> locationAnswers = answers.stream()
            .filter(this::isLocationAnswer)
            .map(ChatAnswer::getAnswerText)
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(v -> !v.isBlank())
            .collect(Collectors.toList());

        String locationText = normalizeForSearch(String.join(" ", locationAnswers));
        String candidateLocation = application != null && application.getCandidate() != null
            ? normalizeForSearch(application.getCandidate().getLocation())
            : "";
        Job job = application == null ? null : application.getJob();
        String jobLocation = job == null ? "" : normalizeForSearch(job.getLocation());

        boolean hasLocationEvidence = !locationAnswers.isEmpty() || !candidateLocation.isBlank();
        boolean remoteCompatible = containsAny(locationText + " " + candidateLocation + " " + jobLocation,
            List.of("teletravail", "remote", "hybride", "a distance"));
        boolean mobility = containsAny(locationText, List.of("mobil", "deplac", "demenag", "relocalis"));
        boolean locationMatch = !candidateLocation.isBlank() && !jobLocation.isBlank() &&
            (candidateLocation.contains(jobLocation) || jobLocation.contains(candidateLocation));

        if (locationMatch) {
            analysis.setLocationMatch("PERFECT");
        } else if (remoteCompatible || mobility || hasLocationEvidence) {
            analysis.setLocationMatch("REMOTE_COMPATIBLE");
        } else {
            analysis.setLocationMatch("INCOMPATIBLE");
        }

        analysis.setHasMobility(mobility || remoteCompatible || locationMatch || hasLocationEvidence);
    }
    
    /**
     * Calcule le score de complétude (0.0 à 1.0)
     */
    private void calculateCompletenessScore(List<ChatAnswer> answers, ChatAnswerAnalysisDTO analysis) {
        double score = 0.0;
        List<String> missingInfo = new ArrayList<>();
        
        // Motivation (25%)
        if (analysis.getMotivationSummary() != null && !analysis.getMotivationSummary().isBlank() && !"Motivation non fournie.".equals(analysis.getMotivationSummary())) {
            score += 0.25;
        } else {
            missingInfo.add("Motivation insuffisante");
        }
        
        // Profil technique (25%)
        if (analysis.getTechnicalSkills() != null && !analysis.getTechnicalSkills().isEmpty()) {
            score += 0.25;
        } else {
            missingInfo.add("Profil technique faible");
        }
        
        // Projets (20%)
        if (analysis.isHasProjectDetails()) {
            score += 0.20;
        } else {
            missingInfo.add("Détails de projets manquants");
        }
        
        // Disponibilité (20%)
        if (analysis.isHasClearAvailability()) {
            score += 0.20;
        } else {
            missingInfo.add("Disponibilité non spécifiée");
        }
        
        // Localisation (10%)
        if (!analysis.getLocationMatch().equals("INCOMPATIBLE")) {
            score += 0.10;
        } else {
            missingInfo.add("Localisation incompatible");
        }
        
        analysis.setCompletenessScore(score);
        analysis.setMissingInformation(missingInfo);
    }
    
    /**
     * Détecte les incohérences dans les réponses
     */
    private void detectInconsistencies(List<ChatAnswer> answers, ChatAnswerAnalysisDTO analysis) {
        List<String> inconsistencies = new ArrayList<>();
        if ((analysis.getTechnicalSkills() == null || analysis.getTechnicalSkills().isEmpty()) && analysis.isHasProjectDetails()) {
            inconsistencies.add("Projets mentionnés sans technologies explicitement listées");
        }

        String availabilityText = normalizeForSearch(
            answers.stream()
                .filter(this::isAvailabilityAnswer)
                .map(ChatAnswer::getAnswerText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "))
        );
        boolean immediateSignal = containsAny(availabilityText, List.of("immediat", "des maintenant", "tout de suite", "de suite"));
        boolean delayedSignal = containsAny(availabilityText, List.of("dans ", "a partir", "d'ici", "mois", "semaine prochaine"))
            || containsAnyWord(availabilityText, List.of("septembre", "octobre", "janvier", "fevrier", "mars", "avril", "mai", "juin", "juillet", "aout", "novembre", "decembre"));
        if (immediateSignal && delayedSignal) {
            inconsistencies.add("Disponibilité contradictoire: immédiate et future mentionnées simultanément");
        }

        String rhythmText = normalizeForSearch(
            answers.stream()
                .filter(this::isRhythmAnswer)
                .map(ChatAnswer::getAnswerText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "))
        );
        boolean fullTimeSignal = containsAny(rhythmText, List.of("temps plein", "full time", "5 jours", "5j", "plein temps"));
        boolean alternanceSignal = containsAny(rhythmText, List.of("alternance", "2 jours", "3 jours", "4 jours", "1j1s", "2j2s", "3j2s", "4j1s", "rythme"));
        if (fullTimeSignal && alternanceSignal) {
            inconsistencies.add("Rythme contradictoire: temps plein et alternance partielle mentionnés simultanément");
        }

        String combinedAnswers = normalizeForSearch(
            answers.stream()
                .map(ChatAnswer::getAnswerText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "))
        );
        if (containsAny(combinedAnswers, List.of("debutant", "junior", "novice")) && mentionsHighExperience(combinedAnswers)) {
            inconsistencies.add("Niveau d'expérience potentiellement contradictoire (débutant avec plusieurs années revendiquées)");
        }
        
        analysis.setInconsistencies(inconsistencies);
    }
    
    private void buildQualitativeSummary(ChatAnswerAnalysisDTO analysis) {
        List<String> strengths = new ArrayList<>();
        List<String> pointsToConfirm = new ArrayList<>();

        Optional<AnalysisFactDTO> motivationFact = findFact(analysis, "motivation");
        Optional<AnalysisFactDTO> projectFact = findFact(analysis, "projects");

        if (motivationFact.isPresent()) {
            analysis.setMotivationAssessment(motivationFact.get().getFinding());
            strengths.add("La motivation est formulée de manière claire et engagée.");
        } else if ("HIGH".equals(analysis.getMotivationLevel())) {
            analysis.setMotivationAssessment("Motivation claire, exprimée avec engagement et reliée au poste.");
            strengths.add("La motivation est formulée de manière claire et engagée.");
        } else if ("MEDIUM".equals(analysis.getMotivationLevel())) {
            analysis.setMotivationAssessment("Motivation présente mais encore partiellement générique.");
            pointsToConfirm.add("Préciser ce qui motive spécifiquement le candidat pour ce poste ou cette entreprise.");
        } else {
            analysis.setMotivationAssessment("La motivation reste absente, trop courte ou insuffisamment exploitable.");
            pointsToConfirm.add("Revenir avec le candidat sur les raisons concrètes de sa candidature.");
        }

        if (projectFact.isPresent()) {
            analysis.setProjectAssessment(projectFact.get().getFinding());
            strengths.add("Des projets ou réalisations concrètes sont mentionnés.");
        } else if (analysis.isHasProjectDetails()) {
            analysis.setProjectAssessment("Au moins un projet ou une réalisation est identifiable dans les réponses ou les preuves externes.");
            strengths.add("Des projets ou réalisations concrètes sont mentionnés.");
        } else {
            analysis.setProjectAssessment("Aucun projet suffisamment détaillé n'a été identifié pour apprécier le niveau réel d'exécution.");
            pointsToConfirm.add("Demander un exemple de projet avec rôle, stack et résultat obtenu.");
        }

        if (analysis.getGithubSummary() != null && !analysis.getGithubSummary().isBlank()) {
            analysis.setGithubAssessment("Des éléments externes sont exploitables via GitHub ou le portfolio.");
            strengths.add("Le candidat fournit des preuves externes consultables (GitHub et/ou portfolio).");
        } else if (analysis.isHasGitHubOrPortfolio()) {
            analysis.setGithubAssessment("Un lien externe est fourni mais l'analyse reste partielle ou peu exploitable.");
            pointsToConfirm.add("Vérifier manuellement la qualité du GitHub ou du portfolio fourni.");
        } else {
            analysis.setGithubAssessment("Aucun GitHub ni portfolio exploitable n'est disponible dans cette candidature.");
            pointsToConfirm.add("Aucune preuve externe n'est disponible pour confirmer les projets mentionnés.");
        }

        if (analysis.isHasClearAvailability()) {
            analysis.setAvailabilityAssessment(describeAvailability(analysis));
            strengths.add("La disponibilité ou le rythme d'alternance est précisé.");
        } else {
            analysis.setAvailabilityAssessment("La disponibilité ou le rythme d'alternance n'est pas clairement précisé.");
            pointsToConfirm.add("Confirmer la date de disponibilité et le rythme d'alternance attendu.");
        }

        analysis.setLocationAssessment(describeLocation(analysis));
        if ("PERFECT".equals(analysis.getLocationMatch()) || "REMOTE_COMPATIBLE".equals(analysis.getLocationMatch())) {
            strengths.add("La localisation ne présente pas de blocage évident à ce stade.");
        } else {
            pointsToConfirm.add("Valider la mobilité ou la compatibilité géographique avec le poste.");
        }

        if (analysis.getTechnicalSkills() != null && !analysis.getTechnicalSkills().isEmpty()) {
            strengths.add("Des technologies ou environnements techniques sont explicitement mentionnés.");
        } else {
            pointsToConfirm.add("Le niveau technique reste à confirmer avec des exemples plus précis.");
        }

        if (analysis.isSemanticFallbackUsed()) {
            pointsToConfirm.add("Extraction sémantique partielle: certains constats reposent sur un fallback technique.");
        }

        if (analysis.getInconsistencies() != null && !analysis.getInconsistencies().isEmpty()) {
            pointsToConfirm.addAll(analysis.getInconsistencies());
        }

        analysis.setStrengths(strengths.stream().distinct().collect(Collectors.toList()));
        analysis.setPointsToConfirm(pointsToConfirm.stream().distinct().collect(Collectors.toList()));
        analysis.setFollowUpQuestions(generateFollowUpQuestions(analysis));
        analysis.setRecommendedAction(resolveQualitativeAction(analysis));
        analysis.setRecruiterGuidance(buildRecruiterGuidance(analysis));
    }

    private List<String> generateFollowUpQuestions(ChatAnswerAnalysisDTO analysis) {
        List<String> followUps = new ArrayList<>();
        List<String> inconsistencies = analysis.getInconsistencies() == null
            ? List.of()
            : analysis.getInconsistencies();

        for (String inconsistency : inconsistencies) {
            String normalized = normalizeForSearch(inconsistency);
            if (normalized.contains("disponibilite contradictoire")) {
                followUps.add("Pouvez-vous confirmer votre date exacte de debut et indiquer si vous etes disponible immediatement ou a une date precise ?");
            }
            if (normalized.contains("rythme contradictoire")) {
                followUps.add("Quel rythme d'alternance souhaitez-vous exactement (ex: 3j/2j, 4j/1j, temps plein) ?");
            }
            if (normalized.contains("experience") || normalized.contains("debutant")) {
                followUps.add("Pouvez-vous preciser vos annees d'experience reelles sur les technologies citees avec un exemple recent ?");
            }
            if (normalized.contains("projets mentionnes sans technologies")) {
                followUps.add("Pour votre projet principal, pouvez-vous detailler la stack utilisee, votre role et les resultats obtenus ?");
            }
        }

        List<String> missingInfo = analysis.getMissingInformation() == null
            ? List.of()
            : analysis.getMissingInformation();
        for (String missing : missingInfo) {
            String normalized = normalizeForSearch(missing);
            if (normalized.contains("motivation")) {
                followUps.add("Qu'est-ce qui vous motive specifiquement pour ce poste et cette entreprise ?");
            }
            if (normalized.contains("disponibilite")) {
                followUps.add("Quelle est votre disponibilite exacte pour debuter et quel rythme pouvez-vous tenir ?");
            }
        }

        return followUps.stream().distinct().limit(6).collect(Collectors.toList());
    }

    private String resolveQualitativeAction(ChatAnswerAnalysisDTO analysis) {
        double evidenceConfidence = computeEvidenceConfidence(analysis);
        boolean hasStrongConfidence = evidenceConfidence >= 0.70;
        boolean hasLowConfidence = evidenceConfidence > 0.0 && evidenceConfidence < 0.50;

        boolean strongReadiness = analysis.getCompletenessScore() >= 0.85
            && "HIGH".equals(analysis.getMotivationLevel())
            && "STRONG".equals(analysis.getTechnicalLevel())
            && !"INCOMPATIBLE".equals(analysis.getLocationMatch())
            && (analysis.getPointsToConfirm() == null || analysis.getPointsToConfirm().size() <= 2)
            && (analysis.getInconsistencies() == null || analysis.getInconsistencies().isEmpty())
            && !analysis.isSemanticFallbackUsed()
            && hasStrongConfidence;

        boolean weakProfile = analysis.getCompletenessScore() < 0.45
            || "LOW".equals(analysis.getMotivationLevel())
            || "WEAK".equals(analysis.getTechnicalLevel())
            || "INCOMPATIBLE".equals(analysis.getLocationMatch());

        if (strongReadiness) {
            return "PRIORITY";
        }
        if (weakProfile) {
            return "REJECT";
        }
        if (hasLowConfidence) {
            return "REVIEW";
        }
        return "REVIEW";
    }

    private String buildRecruiterGuidance(ChatAnswerAnalysisDTO analysis) {
        double evidenceConfidence = computeEvidenceConfidence(analysis);
        boolean incompleteProfile = (analysis.getMissingInformation() != null && analysis.getMissingInformation().size() >= 3)
            || (analysis.getPointsToConfirm() != null && analysis.getPointsToConfirm().size() >= 4)
            || analysis.isSemanticFallbackUsed()
            || evidenceConfidence < 0.55;

        if (incompleteProfile) {
            return "La candidature contient des éléments utiles, mais plusieurs informations clés restent à confirmer avant toute décision (fiabilité partielle des constats).";
        }
        return "La candidature est structurée en constats avec preuves textuelles et un niveau de confiance satisfaisant. Une validation humaine reste requise avant décision.";
    }

    private void extractConstrainedSemanticFacts(List<ChatAnswer> answers, ChatAnswerAnalysisDTO analysis) {
        List<AnalysisFactDTO> llmFacts = semanticExtractionService.extractFacts(answers);
        if (llmFacts != null && !llmFacts.isEmpty()) {
            List<AnalysisFactDTO> sanitizedFacts = sanitizeFacts(llmFacts);
            List<String> missing = analysis.getMissingInformation() == null
                ? new ArrayList<>()
                : new ArrayList<>(analysis.getMissingInformation());

            if (sanitizedFacts.stream().noneMatch(f -> "motivation".equals(f.getDimension()))) {
                missing.add("Aucune preuve textuelle explicite sur la motivation");
            }
            if (sanitizedFacts.stream().noneMatch(f -> "projects".equals(f.getDimension()))) {
                missing.add("Aucune preuve textuelle explicite sur les projets");
            }

            analysis.setAnalysisSchemaVersion("phase1.v2-llm");
            analysis.setSemanticFacts(sanitizedFacts);
            analysis.setSemanticFallbackUsed(false);
            analysis.setMissingInformation(missing.stream().distinct().collect(Collectors.toList()));
            log.info("LLM semantic extraction succeeded for applicationId={} with {} facts", analysis.getApplicationId(), sanitizedFacts.size());
            return;
        }

        log.warn("LLM semantic extraction returned no facts; switching to fallback for applicationId={} (answers={})", analysis.getApplicationId(), answers == null ? 0 : answers.size());
        if (requireLlmExtraction) {
            throw new IllegalStateException("LLM extraction required but no semantic facts were returned");
        }

        List<AnalysisFactDTO> facts = new ArrayList<>();
        boolean fallbackUsed = true;

        List<ChatAnswer> motivationAnswers = answers.stream()
            .filter(this::isMotivationAnswer)
            .collect(Collectors.toList());
        List<ChatAnswer> projectAnswers = answers.stream()
            .filter(this::isProjectAnswer)
            .collect(Collectors.toList());

        facts.addAll(extractFactsFromAnswers(motivationAnswers, "motivation"));
        facts.addAll(extractFactsFromAnswers(projectAnswers, "projects"));

        if (facts.stream().noneMatch(f -> "motivation".equals(f.getDimension()))) {
            fallbackUsed = true;
            String fallbackEvidence = truncateEvidence(analysis.getMotivationSummary());
            if (!fallbackEvidence.isBlank() && !"Motivation non fournie.".equalsIgnoreCase(fallbackEvidence)) {
                facts.add(new AnalysisFactDTO(
                    "motivation",
                    "Constat motivation produit via fallback heuristique (extraction sémantique incomplète).",
                    fallbackEvidence,
                    0.35,
                    "fallback"
                ));
            }
        }

        if (facts.stream().noneMatch(f -> "projects".equals(f.getDimension()))) {
            fallbackUsed = true;
            String fallbackProjects = analysis.getMentionedProjects() == null
                ? ""
                : String.join(" | ", analysis.getMentionedProjects());
            if (!fallbackProjects.isBlank()) {
                facts.add(new AnalysisFactDTO(
                    "projects",
                    "Constat projets produit via fallback technique (extraction sémantique incomplète).",
                    truncateEvidence(fallbackProjects),
                    0.35,
                    "fallback"
                ));
            }
        }

        List<String> missing = analysis.getMissingInformation() == null
            ? new ArrayList<>()
            : new ArrayList<>(analysis.getMissingInformation());
        if (facts.stream().noneMatch(f -> "motivation".equals(f.getDimension()))) {
            missing.add("Aucune preuve textuelle explicite sur la motivation");
        }
        if (facts.stream().noneMatch(f -> "projects".equals(f.getDimension()))) {
            missing.add("Aucune preuve textuelle explicite sur les projets");
        }

        List<AnalysisFactDTO> sanitizedFacts = sanitizeFacts(facts);

        analysis.setAnalysisSchemaVersion("phase1.v2-fallback");
        analysis.setSemanticFacts(sanitizedFacts);
        analysis.setSemanticFallbackUsed(fallbackUsed);
        analysis.setMissingInformation(missing.stream().distinct().collect(Collectors.toList()));
    }

    private List<AnalysisFactDTO> extractFactsFromAnswers(List<ChatAnswer> answers, String dimension) {
        List<AnalysisFactDTO> facts = new ArrayList<>();
        for (ChatAnswer answer : answers) {
            String raw = answer.getAnswerText() == null ? "" : answer.getAnswerText().trim();
            if (raw.isBlank()) {
                continue;
            }

            double confidence = answer.getQuestionText() == null || answer.getQuestionText().isBlank() ? 0.70 : 0.80;
            String finding = buildFinding(dimension);

            facts.add(new AnalysisFactDTO(
                dimension,
                finding,
                truncateEvidence(raw),
                confidence,
                answer.getQuestionText() == null ? "" : answer.getQuestionText()
            ));
        }
        return facts;
    }

    private String buildFinding(String dimension) {
        if ("motivation".equals(dimension)) {
            return "Le candidat fournit un élément explicite de motivation.";
        }

        if ("projects".equals(dimension)) {
            return "Le candidat décrit un projet ou une réalisation.";
        }

        return "Le candidat fournit une information exploitable.";
    }

    private String truncateEvidence(String text) {
        if (text == null) {
            return "";
        }
        String value = text.trim();
        if (value.length() <= 220) {
            return value;
        }
        return value.substring(0, 220) + "...";
    }

    private Optional<AnalysisFactDTO> findFact(ChatAnswerAnalysisDTO analysis, String dimension) {
        if (analysis.getSemanticFacts() == null) {
            return Optional.empty();
        }
        return analysis.getSemanticFacts().stream()
            .filter(fact -> dimension.equals(fact.getDimension()))
            .findFirst();
    }

    private List<AnalysisFactDTO> sanitizeFacts(List<AnalysisFactDTO> facts) {
        if (facts == null || facts.isEmpty()) {
            return List.of();
        }

        return facts.stream()
            .filter(Objects::nonNull)
            .map(fact -> {
                String dimension = fact.getDimension() == null || fact.getDimension().isBlank()
                    ? "general"
                    : fact.getDimension().trim();
                String finding = fact.getFinding() == null ? "" : fact.getFinding().trim();
                String evidence = fact.getEvidence() == null ? "" : fact.getEvidence().trim();
                String sourceQuestion = fact.getSourceQuestion() == null ? "" : fact.getSourceQuestion().trim();
                double confidence = Math.max(0.0, Math.min(1.0, fact.getConfidence()));
                return new AnalysisFactDTO(dimension, finding, evidence, confidence, sourceQuestion);
            })
            .filter(fact -> !fact.getFinding().isBlank() && !fact.getEvidence().isBlank())
            .collect(Collectors.toList());
    }

    private double computeEvidenceConfidence(ChatAnswerAnalysisDTO analysis) {
        if (analysis.getSemanticFacts() == null || analysis.getSemanticFacts().isEmpty()) {
            return 0.0;
        }
        return analysis.getSemanticFacts().stream()
            .mapToDouble(f -> Math.max(0.0, Math.min(1.0, f.getConfidence())))
            .average()
            .orElse(0.0);
    }

    private String describeAvailability(ChatAnswerAnalysisDTO analysis) {
        String availability = analysis.getAvailabilityStatus();
        String rhythm = analysis.getAlternanceRhythm();

        String availabilityLabel;
        if ("IMMEDIATE".equals(availability)) {
            availabilityLabel = "La disponibilité est annoncée comme immédiate";
        } else if ("FUTURE".equals(availability)) {
            availabilityLabel = "Une disponibilité future est mentionnée";
        } else {
            availabilityLabel = "Une disponibilité est mentionnée";
        }

        String rhythmLabel;
        if ("FULL_TIME".equals(rhythm)) {
            rhythmLabel = "avec un rythme d'alternance clairement précisé";
        } else if ("PART_TIME".equals(rhythm)) {
            rhythmLabel = "avec un rythme partiel à confirmer";
        } else {
            rhythmLabel = "avec un rythme encore flexible";
        }

        return availabilityLabel + " " + rhythmLabel + ".";
    }

    private String describeLocation(ChatAnswerAnalysisDTO analysis) {
        if ("PERFECT".equals(analysis.getLocationMatch())) {
            return "La localisation semble compatible avec le poste.";
        }
        if ("REMOTE_COMPATIBLE".equals(analysis.getLocationMatch())) {
            return "La localisation peut rester compatible sous réserve de mobilité ou de télétravail.";
        }
        return "La compatibilité géographique n'est pas démontrée à ce stade.";
    }
    
    /**
     * Génère un résumé de motivation
     */
    private String generateMotivationSummary(String fullText) {
        if (fullText == null || fullText.trim().isEmpty()) {
            return "Motivation non fournie.";
        }
        if (fullText.length() > 200) {
            return fullText.substring(0, 200) + "...";
        }
        return fullText;
    }

    private boolean isMotivationAnswer(ChatAnswer answer) {
        String key = normalizeForSearch(answer.getQuestionKey());
        String question = normalizeForSearch(answer.getQuestionText());

        return key.contains("motivation")
            || key.contains("motive")
            || question.contains("motivation")
            || question.contains("motive")
            || question.contains("pourquoi")
            || question.contains("interet")
            || question.contains("envie");
    }

    private boolean isProjectAnswer(ChatAnswer answer) {
        String key = normalizeForSearch(answer.getQuestionKey());
        String question = normalizeForSearch(answer.getQuestionText());

        return key.contains("projet")
            || key.contains("portfolio")
            || question.contains("projet")
            || question.contains("realisation")
            || question.contains("application")
            || question.contains("github")
            || question.contains("portfolio");
    }

    private boolean isTechnicalAnswer(ChatAnswer answer) {
        String key = normalizeForSearch(answer.getQuestionKey());
        String question = normalizeForSearch(answer.getQuestionText());

        return key.contains("tech")
            || key.contains("skill")
            || key.contains("competence")
            || question.contains("competence")
            || question.contains("technique")
            || question.contains("stack")
            || question.contains("technologie");
    }

    private boolean isAvailabilityAnswer(ChatAnswer answer) {
        String key = normalizeForSearch(answer.getQuestionKey());
        String question = normalizeForSearch(answer.getQuestionText());

        return key.contains("disponib")
            || key.contains("debut")
            || question.contains("disponibilite")
            || question.contains("disponible")
            || question.contains("date de debut");
    }

    private boolean isRhythmAnswer(ChatAnswer answer) {
        String key = normalizeForSearch(answer.getQuestionKey());
        String question = normalizeForSearch(answer.getQuestionText());

        return key.contains("rythme")
            || key.contains("alternance")
            || question.contains("rythme")
            || question.contains("alternance");
    }

    private boolean isLocationAnswer(ChatAnswer answer) {
        String key = normalizeForSearch(answer.getQuestionKey());
        String question = normalizeForSearch(answer.getQuestionText());

        return key.contains("local")
            || key.contains("mobilit")
            || question.contains("localisation")
            || question.contains("ville")
            || question.contains("mobilite");
    }

    private List<String> extractListedItems(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        return TOKEN_SPLIT_PATTERN.splitAsStream(raw)
            .map(String::trim)
            .map(v -> v.replaceAll("^[\\-•*]+", "").trim())
            .filter(v -> !v.isBlank())
            .filter(v -> v.length() <= 40)
            .collect(Collectors.toList());
    }

    private List<String> extractTechMarkers(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return List.of();
        }

        return TECH_MARKERS.stream()
            .filter(normalizedText::contains)
            .collect(Collectors.toList());
    }

    private boolean containsAny(String text, List<String> needles) {
        if (text == null || text.isBlank() || needles == null || needles.isEmpty()) {
            return false;
        }
        return needles.stream().anyMatch(text::contains);
    }

    private boolean containsAnyWord(String text, List<String> words) {
        if (text == null || text.isBlank() || words == null || words.isEmpty()) {
            return false;
        }
        return words.stream().anyMatch(word -> Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(text).find());
    }

    private boolean mentionsHighExperience(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        Matcher matcher = YEARS_EXPERIENCE_PATTERN.matcher(text);
        while (matcher.find()) {
            try {
                int years = Integer.parseInt(matcher.group(1));
                if (years >= 3) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // Ignore malformed captures and continue scanning.
            }
        }
        return false;
    }

    private String normalizeForSearch(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .toLowerCase();
    }
    
    /**
     * Valide la complétude des réponses pour une candidature
     */
    public boolean validateAnswerCompleteness(UUID applicationId) {
        ChatAnswerAnalysisDTO analysis = analyzeChatAnswers(applicationId);
        return analysis.getCompletenessScore() >= 0.5 && 
               analysis.getMissingInformation().size() <= 2;
    }
    
    /**
     * Extrait les réponses du chatbot pour une candidature
     */
    public Map<String, String> extractStructuredAnswers(UUID applicationId) {
        List<ChatAnswer> answers = chatAnswerRepository.findByApplication_ApplicationId(applicationId);
        Map<String, String> structuredAnswers = new HashMap<>();
        
        for (ChatAnswer answer : answers) {
            String question = answer.getQuestionText();
            String response = answer.getAnswerText();
            String qNorm = question.toLowerCase()
                .replace("é", "e")
                .replace("è", "e")
                .replace("ê", "e")
                .replace("à", "a")
                .replace("ç", "c")
                .replace("ô", "o")
                .replace("î", "i")
                .replace("ï", "i")
                .replace("û", "u")
                .replace("ù", "u");

            // Détection plus robuste de motivation
            if (qNorm.contains("motivation") || qNorm.contains("motive") || qNorm.contains("pourquoi postule") || qNorm.contains("envie") || qNorm.contains("interet")) {
                structuredAnswers.put("motivation", response);
            } else if (qNorm.contains("projet") || qNorm.contains("projets") || qNorm.contains("application") || qNorm.contains("realisation")) {
                structuredAnswers.put("projets", response);
            } else if (qNorm.contains("disponibilite") || qNorm.contains("disponible") || qNorm.contains("date de debut")) {
                structuredAnswers.put("disponibilite", response);
            } else if (qNorm.contains("rythme") || qNorm.contains("temps") || qNorm.contains("alternance")) {
                structuredAnswers.put("rythme", response);
            } else if (qNorm.contains("localisation") || qNorm.contains("ville") || qNorm.contains("region") || qNorm.contains("lieu")) {
                structuredAnswers.put("localisation", response);
            } else {
                structuredAnswers.put("general", response);
            }
        }
        return structuredAnswers;
    }
}
