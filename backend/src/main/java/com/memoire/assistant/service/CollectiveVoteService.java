package com.memoire.assistant.service;

import com.memoire.assistant.model.*;
import com.memoire.assistant.model.ApplicationActivity.EventType;
import com.memoire.assistant.repository.*;
import com.memoire.assistant.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CollectiveVoteService {

    @Autowired private CollectiveVoteRepository voteRepository;
    @Autowired private CollectiveVoteBallotRepository ballotRepository;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private ApplicationStatusRepository statusRepository;
    @Autowired private RecruiterRepository recruiterRepository;
    @Autowired private InternalNotificationService notificationService;
    @Autowired private ApplicationActivityService activityService;
    @Autowired private JdbcTemplate jdbcTemplate;

    // Durées des rounds
    private static final int ROUND_1_HOURS = 72;
    private static final int ROUND_2_HOURS = 48;
    private static final int REMINDER_BEFORE_CLOSE_HOURS = 24; // rappel à 48h pour round 1, 24h pour round 2

    /**
     * Ouvre un vote collectif round 1 (membres assignés à l'offre).
     * Appelé par DecisionService quand MANAGER et ADMIN ne sont pas d'accord.
     */
    public CollectiveVote openRound1(UUID applicationId, UUID companyId) {
        Application app = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new IllegalArgumentException("Candidature introuvable"));

        // Passer le statut à en_deliberation
        setApplicationStatus(app, "en_deliberation");

        CollectiveVote vote = new CollectiveVote();
        vote.setApplicationId(applicationId);
        vote.setCompanyId(companyId);
        vote.setRound(1);
        vote.setClosesAt(LocalDateTime.now().plusHours(ROUND_1_HOURS));
        CollectiveVote saved = voteRepository.save(vote);

        // Notifier tous les assignés de l'offre
        List<UUID> eligibleVoters = getEligibleVoters(app.getJob().getJobId(), companyId, 1);
        String candidateName = buildCandidateName(app);
        for (UUID voterId : eligibleVoters) {
            notificationService.notify(voterId, companyId,
                InternalNotification.Type.DECISION_NEEDED,
                "Vote collectif ouvert",
                "Un désaccord a été détecté sur " + candidateName + ". Votre vote est attendu sous 72h.",
                "APPLICATION", applicationId);
        }

        activityService.logEvent(applicationId, EventType.STATUS_CHANGED,
            Map.of("from", app.getStatus() != null ? app.getStatus().getCode() : "",
                   "to", "en_deliberation", "reason", "desaccord_managers"));

        // Email candidat : décision en cours
        notifyCandidatePending(applicationId, companyId);

        return saved;
    }

    /**
     * Ouvre un vote collectif round 2 (tous les recruteurs de l'entreprise).
     * Appelé quand le round 1 aboutit à une égalité.
     */
    public CollectiveVote openRound2(UUID applicationId, UUID companyId) {
        Application app = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new IllegalArgumentException("Candidature introuvable"));

        setApplicationStatus(app, "second_vote");

        CollectiveVote vote = new CollectiveVote();
        vote.setApplicationId(applicationId);
        vote.setCompanyId(companyId);
        vote.setRound(2);
        vote.setClosesAt(LocalDateTime.now().plusHours(ROUND_2_HOURS));
        CollectiveVote saved = voteRepository.save(vote);

        // Notifier TOUS les recruteurs de l'entreprise
        List<UUID> allRecruiters = recruiterRepository.findByCompany_CompanyId(companyId)
            .stream().map(Recruiter::getRecruiterId).collect(Collectors.toList());
        for (UUID voterId : allRecruiters) {
            notificationService.notify(voterId, companyId,
                InternalNotification.Type.DECISION_NEEDED,
                "Vote élargi — votre avis compte",
                "Le vote interne est à égalité. Toute l'équipe est invitée à voter sous 48h.",
                "APPLICATION", applicationId);
        }

        activityService.logEvent(applicationId, EventType.STATUS_CHANGED,
            Map.of("from", "en_deliberation", "to", "second_vote", "reason", "egalite_round1"));

        return saved;
    }

    /**
     * Enregistre un bulletin de vote anonyme.
     */
    public Map<String, Object> castBallot(UUID voteId, String choice) {
        UUID voterId = TenantContext.getActorId();
        UUID companyId = TenantContext.getCompanyId();

        CollectiveVote vote = voteRepository.findById(voteId)
            .orElseThrow(() -> new IllegalArgumentException("Vote introuvable"));

        if (!"OPEN".equals(vote.getStatus())) {
            throw new IllegalStateException("Ce vote est clôturé.");
        }
        if (LocalDateTime.now().isAfter(vote.getClosesAt())) {
            throw new IllegalStateException("Le délai de vote est dépassé.");
        }
        if (!List.of("APPROVE", "REJECT").contains(choice)) {
            throw new IllegalArgumentException("Choix invalide.");
        }
        if (ballotRepository.existsByVoteIdAndVoterId(voteId, voterId)) {
            throw new IllegalStateException("Vous avez déjà voté.");
        }

        // Vérifier que le votant est éligible
        Application app = applicationRepository.findById(vote.getApplicationId())
            .orElseThrow(() -> new IllegalArgumentException("Candidature introuvable"));
        List<UUID> eligible = getEligibleVoters(app.getJob().getJobId(), companyId, vote.getRound());
        if (!eligible.contains(voterId)) {
            throw new IllegalStateException("Vous n'êtes pas éligible à ce vote.");
        }

        CollectiveVoteBallot ballot = new CollectiveVoteBallot();
        ballot.setVoteId(voteId);
        ballot.setVoterId(voterId);
        ballot.setChoice(choice);
        ballotRepository.save(ballot);

        return buildVoteStatus(vote, companyId);
    }

    /**
     * Ferme un vote expiré, calcule le résultat, déclenche la suite.
     */
    public void closeExpiredVote(CollectiveVote vote) {
        UUID companyId = vote.getCompanyId();
        Application app = applicationRepository.findById(vote.getApplicationId()).orElse(null);
        if (app == null) return;

        // Marquer les non-votants comme ABSTAIN
        List<UUID> eligible = getEligibleVoters(app.getJob().getJobId(), companyId, vote.getRound());
        Set<UUID> alreadyVoted = ballotRepository.findByVoteId(vote.getId())
            .stream().map(CollectiveVoteBallot::getVoterId).collect(Collectors.toSet());

        for (UUID abstainerId : eligible) {
            if (!alreadyVoted.contains(abstainerId)) {
                CollectiveVoteBallot abstain = new CollectiveVoteBallot();
                abstain.setVoteId(vote.getId());
                abstain.setVoterId(abstainerId);
                abstain.setChoice("ABSTAIN");
                ballotRepository.save(abstain);
            }
        }

        // Compter les votes (abstentions exclues du décompte)
        List<CollectiveVoteBallot> ballots = ballotRepository.findByVoteId(vote.getId());
        long approves = ballots.stream().filter(b -> "APPROVE".equals(b.getChoice())).count();
        long rejects  = ballots.stream().filter(b -> "REJECT".equals(b.getChoice())).count();

        String result;
        if (approves > rejects)       result = "APPROVED";
        else if (rejects > approves)  result = "REJECTED";
        else                          result = "TIE";

        vote.setResult(result);
        vote.setStatus("CLOSED");
        vote.setClosedAt(LocalDateTime.now());
        voteRepository.save(vote);

        activityService.logEvent(vote.getApplicationId(), EventType.DECISION_RECORDED,
            Map.of("type", "COLLECTIVE_VOTE", "round", vote.getRound(),
                   "approves", approves, "rejects", rejects, "result", result));

        if ("TIE".equals(result)) {
            if (vote.getRound() == 1) {
                openRound2(vote.getApplicationId(), companyId);
            } else {
                // Round 2 encore égalité → ADMIN tranche (notification)
                escalateToAdmin(vote.getApplicationId(), companyId);
            }
        } else {
            applyCollectiveResult(app, result, companyId);
        }
    }

    /**
     * Envoie les rappels aux non-votants (48h avant clôture round 1, 24h avant round 2).
     */
    public void sendReminders(CollectiveVote vote) {
        Application app = applicationRepository.findById(vote.getApplicationId()).orElse(null);
        if (app == null) return;

        UUID companyId = vote.getCompanyId();
        List<UUID> eligible = getEligibleVoters(app.getJob().getJobId(), companyId, vote.getRound());
        Set<UUID> alreadyVoted = ballotRepository.findByVoteId(vote.getId())
            .stream().map(CollectiveVoteBallot::getVoterId).collect(Collectors.toSet());

        String hoursLeft = vote.getRound() == 1 ? "48h" : "24h";
        for (UUID recruiterId : eligible) {
            if (!alreadyVoted.contains(recruiterId)) {
                notificationService.notify(recruiterId, companyId,
                    InternalNotification.Type.DECISION_NEEDED,
                    "Rappel : vote en attente",
                    "Il vous reste " + hoursLeft + " pour voter sur la candidature de "
                        + buildCandidateName(app) + ".",
                    "APPLICATION", vote.getApplicationId());
            }
        }

        vote.setReminderSent(true);
        voteRepository.save(vote);
    }

    /**
     * Retourne l'état du vote courant pour une candidature (pour le frontend).
     */
    public Map<String, Object> getVoteStatus(UUID applicationId) {
        UUID companyId = TenantContext.getCompanyId();
        UUID voterId = TenantContext.getActorId();

        Optional<CollectiveVote> openVote = voteRepository.findByApplicationIdAndStatus(applicationId, "OPEN");
        if (openVote.isEmpty()) {
            // Retourner le dernier vote clôturé s'il existe
            return voteRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId)
                .stream().findFirst()
                .map(v -> buildVoteStatus(v, companyId))
                .orElse(Map.of("hasVote", false));
        }

        CollectiveVote vote = openVote.get();
        Map<String, Object> status = buildVoteStatus(vote, companyId);

        // Indiquer si l'utilisateur courant a déjà voté (sans révéler son vote)
        boolean hasVoted = ballotRepository.existsByVoteIdAndVoterId(vote.getId(), voterId);
        status.put("currentUserHasVoted", hasVoted);

        // Indiquer si l'utilisateur est éligible
        Application app = applicationRepository.findById(applicationId).orElse(null);
        if (app != null) {
            List<UUID> eligible = getEligibleVoters(app.getJob().getJobId(), companyId, vote.getRound());
            status.put("currentUserIsEligible", eligible.contains(voterId));
        }

        return status;
    }

    // ─── Helpers ────────────────────────────────────────────

    private Map<String, Object> buildVoteStatus(CollectiveVote vote, UUID companyId) {
        List<CollectiveVoteBallot> ballots = ballotRepository.findByVoteId(vote.getId());
        long approves = ballots.stream().filter(b -> "APPROVE".equals(b.getChoice())).count();
        long rejects  = ballots.stream().filter(b -> "REJECT".equals(b.getChoice())).count();
        long total    = ballots.stream().filter(b -> !"ABSTAIN".equals(b.getChoice())).count();

        Application app = applicationRepository.findById(vote.getApplicationId()).orElse(null);
        long eligibleCount = app != null
            ? getEligibleVoters(app.getJob().getJobId(), companyId, vote.getRound()).size()
            : 0;

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("hasVote",       true);
        map.put("voteId",        vote.getId().toString());
        map.put("round",         vote.getRound());
        map.put("status",        vote.getStatus());
        map.put("closesAt",      vote.getClosesAt().toString());
        map.put("result",        vote.getResult());
        map.put("approves",      approves);
        map.put("rejects",       rejects);
        map.put("totalVoted",    total);
        map.put("eligibleCount", eligibleCount);
        return map;
    }

    private List<UUID> getEligibleVoters(UUID jobId, UUID companyId, int round) {
        if (round == 2) {
            return recruiterRepository.findByCompany_CompanyId(companyId)
                .stream().map(Recruiter::getRecruiterId).collect(Collectors.toList());
        }
        // Round 1 : membres assignés à l'offre via job_assignees
        List<UUID> assignees = jdbcTemplate.queryForList(
            "SELECT recruiter_id FROM job_assignees WHERE job_id = ?",
            UUID.class, jobId);
        // Toujours inclure le ownerRecruiter
        jdbcTemplate.queryForList(
            "SELECT owner_recruiter_id FROM jobs WHERE job_id = ? AND owner_recruiter_id IS NOT NULL",
            UUID.class, jobId)
            .forEach(id -> { if (!assignees.contains(id)) assignees.add(id); });
        return assignees;
    }

    private void setApplicationStatus(Application app, String code) {
        statusRepository.findByCode(code).ifPresent(s -> {
            app.setStatus(s);
            applicationRepository.save(app);
        });
    }

    private void applyCollectiveResult(Application app, String result, UUID companyId) {
        String statusCode = "APPROVED".equals(result) ? "retenu" : "non_retenu";
        setApplicationStatus(app, statusCode);

        // Notifier tous les votants du résultat
        String label = "APPROVED".equals(result) ? "Retenu" : "Non retenu";
        String candidateName = buildCandidateName(app);
        recruiterRepository.findByCompany_CompanyId(companyId).forEach(r ->
            notificationService.notify(r.getRecruiterId(), companyId,
                InternalNotification.Type.FINAL_DECISION_RECORDED,
                "Résultat du vote collectif",
                candidateName + " : " + label + " (vote collectif)",
                "APPLICATION", app.getApplicationId())
        );
    }

    private void escalateToAdmin(UUID applicationId, UUID companyId) {
        // Égalité après round 2 → notifier les ADMIN pour décision arbitrale
        recruiterRepository.findByCompany_CompanyId(companyId).stream()
            .filter(r -> "ADMIN".equalsIgnoreCase(r.getRole()))
            .forEach(admin -> notificationService.notify(admin.getRecruiterId(), companyId,
                InternalNotification.Type.DECISION_NEEDED,
                "Égalité — décision arbitrale requise",
                "Le vote élargi n'a pas permis de trancher. Votre décision finale est attendue.",
                "APPLICATION", applicationId));
    }

    private void notifyCandidatePending(UUID applicationId, UUID companyId) {
        // Placeholder — email candidat géré par le service email existant si disponible
    }

    private String buildCandidateName(Application app) {
        if (app.getCandidate() == null) return "ce candidat";
        String first = app.getCandidate().getFirstName() != null ? app.getCandidate().getFirstName() : "";
        String last  = app.getCandidate().getLastName()  != null ? app.getCandidate().getLastName()  : "";
        return (first + " " + last).trim();
    }
}
