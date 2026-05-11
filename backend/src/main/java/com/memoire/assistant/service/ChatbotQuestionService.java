package com.memoire.assistant.service;

import com.memoire.assistant.model.ChatbotQuestion;
import com.memoire.assistant.model.Job;
import com.memoire.assistant.repository.ChatbotQuestionRepository;
import com.memoire.assistant.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChatbotQuestionService {
    @Autowired
    private ChatbotQuestionRepository chatbotQuestionRepository;
    @Autowired
    private JobRepository jobRepository;

    @Transactional
    public List<ChatbotQuestion> getQuestionsForJob(UUID jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found"));
        List<ChatbotQuestion> existing = chatbotQuestionRepository.findByJobOrderByOrderIndexAsc(job);
        if (!existing.isEmpty()) return existing;
        return generateAndSaveQuestions(job);
    }

    /** Régénère les questions d'un poste depuis ses exigences (écrase les existantes). */
    @Transactional
    public List<ChatbotQuestion> regenerateQuestionsForJob(UUID jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found"));
        chatbotQuestionRepository.deleteByJob(job);
        return generateAndSaveQuestions(job);
    }

    private List<ChatbotQuestion> generateAndSaveQuestions(Job job) {
        List<ChatbotQuestion> questions = new ArrayList<>();
        int idx = 0;

        // 1. Motivation — toujours présente, personnalisée avec le titre du poste
        questions.add(build(job, idx++,
            "motivation",
            "Pourquoi souhaitez-vous rejoindre notre équipe en tant que " + job.getTitle() + " ? "
            + "Qu'est-ce qui vous a motivé à postuler à ce poste ?",
            true));

        // 2. Technologies — adaptée à la liste des technologies requises du poste
        List<String> techs = job.getTechnologies();
        if (techs != null && !techs.isEmpty()) {
            String techList = String.join(", ", techs);
            questions.add(build(job, idx++,
                "tech_requises",
                "Ce poste requiert les technologies suivantes : " + techList + ". "
                + "Pour chacune, décrivez votre niveau de maîtrise et vos expériences concrètes.",
                true));
        } else {
            questions.add(build(job, idx++,
                "tech_generales",
                "Quelles technologies, langages et frameworks maîtrisez-vous ? "
                + "Décrivez votre niveau pour chacun.",
                true));
        }

        // 3. Projets — toujours présente
        questions.add(build(job, idx++,
            "projet",
            "Décrivez un ou plusieurs projets sur lesquels vous avez travaillé récemment. "
            + "Quel était votre rôle, les technologies utilisées, et les résultats obtenus ?",
            true));

        // 4. Disponibilité — adaptée au rythme si c'est une alternance
        String rhythm = job.getAlternanceRhythm();
        if (rhythm != null && !rhythm.isBlank()) {
            questions.add(build(job, idx++,
                "disponibilite",
                "Ce poste est proposé en " + rhythm + ". "
                + "Êtes-vous disponible selon ce rythme ? Quelle est votre date de début souhaitée ?",
                true));
        } else {
            questions.add(build(job, idx++,
                "disponibilite",
                "Quelle est votre disponibilité pour commencer ce poste ? Avez-vous un préavis en cours ?",
                true));
        }

        // 5. Localisation — uniquement si le poste n'est pas full remote
        String location = job.getLocation();
        if (location != null && !location.isBlank()
                && !location.toLowerCase().contains("remote")
                && !location.toLowerCase().contains("télétravail")) {
            questions.add(build(job, idx++,
                "localisation",
                "Ce poste est basé à " + location + ". Êtes-vous disponible pour travailler sur ce lieu, "
                + "ou avez-vous besoin d'arrangements particuliers (télétravail partiel, déménagement) ?",
                false));
        }

        // 6. Question libre sur le poste (contexte si disponible)
        String contexte = job.getContextePoste();
        if (contexte != null && !contexte.isBlank()) {
            questions.add(build(job, idx++,
                "contexte_poste",
                "Ce poste s'inscrit dans le contexte suivant : " + contexte + ". "
                + "Avez-vous des questions ou des éléments à partager en lien avec ce contexte ?",
                false));
        }

        chatbotQuestionRepository.saveAll(questions);
        return questions;
    }

    private ChatbotQuestion build(Job job, int idx, String key, String text, boolean required) {
        ChatbotQuestion q = new ChatbotQuestion();
        q.setJob(job);
        q.setOrderIndex(idx);
        q.setQuestionKey(key);
        q.setQuestionText(text);
        q.setAnswerType("open");
        q.setRequired(required);
        return q;
    }

    public ChatbotQuestion addQuestion(UUID jobId, String questionText, int orderIndex, String answerType, boolean required) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found"));
        ChatbotQuestion question = new ChatbotQuestion();
        question.setJob(job);
        question.setQuestionText(questionText);
        question.setOrderIndex(orderIndex);
        question.setAnswerType(answerType);
        question.setRequired(required);
        return chatbotQuestionRepository.save(question);
    }

    public void deleteQuestion(UUID questionId) {
        chatbotQuestionRepository.deleteById(questionId);
    }

    public Optional<ChatbotQuestion> updateQuestion(UUID questionId, String questionText, int orderIndex, String answerType, boolean required) {
        Optional<ChatbotQuestion> opt = chatbotQuestionRepository.findById(questionId);
        opt.ifPresent(q -> {
            q.setQuestionText(questionText);
            q.setOrderIndex(orderIndex);
            q.setAnswerType(answerType);
            q.setRequired(required);
            chatbotQuestionRepository.save(q);
        });
        return opt;
    }
}
