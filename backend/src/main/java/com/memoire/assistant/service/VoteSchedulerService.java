package com.memoire.assistant.service;

import com.memoire.assistant.model.CollectiveVote;
import com.memoire.assistant.repository.CollectiveVoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VoteSchedulerService {

    @Autowired private CollectiveVoteRepository voteRepository;
    @Autowired private CollectiveVoteService collectiveVoteService;

    /** Toutes les heures : ferme les votes expirés */
    @Scheduled(fixedDelay = 3_600_000)
    public void closeExpiredVotes() {
        List<CollectiveVote> expired = voteRepository
            .findByStatusAndClosesAtBefore("OPEN", LocalDateTime.now());
        for (CollectiveVote vote : expired) {
            try {
                collectiveVoteService.closeExpiredVote(vote);
            } catch (Exception e) {
                // log silencieux — ne pas bloquer les autres votes
            }
        }
    }

    /**
     * Toutes les heures : envoie les rappels aux non-votants.
     * Round 1 (72h) → rappel à 48h restantes = quand openedAt + 24h est passé
     * Round 2 (48h) → rappel à 24h restantes = quand openedAt + 24h est passé
     * Dans les deux cas : rappel quand il reste exactement la moitié du temps.
     */
    @Scheduled(fixedDelay = 3_600_000)
    public void sendVoteReminders() {
        // Rappel quand il reste moins de 24h (dans les deux rounds)
        LocalDateTime reminderThreshold = LocalDateTime.now().plusHours(24);
        List<CollectiveVote> needReminder = voteRepository
            .findByStatusAndReminderSentFalseAndClosesAtBefore("OPEN", reminderThreshold);
        for (CollectiveVote vote : needReminder) {
            try {
                collectiveVoteService.sendReminders(vote);
            } catch (Exception e) {
                // log silencieux
            }
        }
    }
}
