package com.memoire.assistant.service;

import com.memoire.assistant.dto.EmailNotificationDTO;
import com.memoire.assistant.model.Application;
import com.memoire.assistant.model.Candidate;
import com.memoire.assistant.model.Job;
import com.memoire.assistant.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class EmailNotificationService {
    
    @Autowired
    private ApplicationRepository applicationRepository;
    
    @Value("${app.frontend.url:http://localhost:3100}")
    private String frontendUrl;
    
    /**
     * Simule l'envoi d'une notification email
     * En production, cette méthode intégrera un vrai service d'envoi d'emails
     */
    public boolean sendEmail(EmailNotificationDTO notification) {
        try {
            // Simuler l'envoi d'email
            System.out.println("=== ENVOI EMAIL ===");
            System.out.println("To: " + notification.getTo());
            System.out.println("Subject: " + notification.getSubject());
            System.out.println("Type: " + notification.getType());
            System.out.println("Content: " + notification.getContent());
            
            // Si template, générer le contenu
            if (notification.getTemplateName() != null) {
                String generatedContent = generateEmailContent(notification);
                System.out.println("Generated Content:\n" + generatedContent);
                notification.setContent(generatedContent);
            }
            
            // Simuler un délai d'envoi
            Thread.sleep(100);
            
            notification.setSent(true);
            notification.setSentAt(LocalDateTime.now());
            
            System.out.println("Email envoyé avec succès à: " + notification.getTo());
            System.out.println("==================");
            
            return true;
        } catch (Exception e) {
            notification.setErrorMessage("Erreur d'envoi email: " + e.getMessage());
            System.err.println("Erreur lors de l'envoi d'email: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Génère le contenu de l'email basé sur le template
     */
    private String generateEmailContent(EmailNotificationDTO notification) {
        StringBuilder content = new StringBuilder();
        
        switch (notification.getType()) {
            case CANDIDATE_RETENTION:
                content.append(generateRetentionEmailContent(notification));
                break;
            case CANDIDATE_REJECTION:
                content.append(generateRejectionEmailContent(notification));
                break;
            case CANDIDATE_POOL:
                content.append(generatePoolEmailContent(notification));
                break;
            case CANDIDATE_ACKNOWLEDGMENT:
                content.append(generateAcknowledgmentEmailContent(notification));
                break;
            case INTERVIEW_REMINDER:
                content.append(generateInterviewReminderContent(notification));
                break;
            case INTERVIEW_CONFIRMATION:
                content.append(generateInterviewConfirmationContent(notification));
                break;
            case RECRUITER_SUMMARY:
                content.append(generateRecruiterSummaryContent(notification));
                break;
            default:
                content.append(notification.getContent());
        }
        
        return content.toString();
    }
    
    private String generateRetentionEmailContent(EmailNotificationDTO notification) {
        Map<String, Object> vars = notification.getTemplateVariables();
        return String.format("""
            Bonjour %s,
            
            Nous avons le plaisir de vous informer que votre candidature pour le poste de %s chez %s a été retenue !
            
            Votre profil a particulièrement retenu notre attention et nous souhaitons vous rencontrer pour un entretien.
            
            Prochaines étapes :
            1. Nous vous contacterons rapidement pour convenir d'un créneau
            2. Entretien avec l'équipe technique
            3. Retour sous 48h
            
            Nous vous remercions pour votre intérêt et sommes impatients de vous rencontrer.
            
            Cordialement,
            L'équipe de %s
            
            ---
            ID Candidature: %s
            """, 
            vars != null ? vars.get("candidateName") : "Candidat",
            notification.getJobTitle(),
            notification.getCompanyName(),
            notification.getCompanyName(),
            notification.getApplicationId()
        );
    }
    
    private String generateRejectionEmailContent(EmailNotificationDTO notification) {
        Map<String, Object> vars = notification.getTemplateVariables();
        return String.format("""
            Bonjour %s,
            
            Nous vous remercions pour l'intérêt que vous avez porté au poste de %s chez %s.
            
            Après étude attentive de votre candidature, nous regrettons de vous informer que celle-ci n'a pas été retenue à ce stade.
            
            Nous conservons votre profil au sein de notre vivier et ne manquerons pas de vous recontacter si une opportunité correspondant à vos compétences se présente.
            
            Nous vous souhaitons beaucoup de succès dans votre recherche d'alternance.
            
            Cordialement,
            L'équipe de %s
            
            ---
            ID Candidature: %s
            """, 
            vars != null ? vars.get("candidateName") : "Candidat",
            notification.getJobTitle(),
            notification.getCompanyName(),
            notification.getCompanyName(),
            notification.getApplicationId()
        );
    }
    
    private String generatePoolEmailContent(EmailNotificationDTO notification) {
        Map<String, Object> vars = notification.getTemplateVariables();
        return String.format("""
            Bonjour %s,
            
            Nous vous remercions pour votre candidature au poste de %s chez %s.
            
            Bien que votre profil ne corresponde pas parfaitement aux besoins actuels, il a retenu notre attention.
            
            Nous avons le plaisir de vous informer que nous conservons votre candidature au sein de notre vivier pour de futures opportunités.
            
            Nous ne manquerons pas de vous recontacter dès qu'un poste en adéquation avec vos compétences et vos aspirations sera disponible.
            
            N'hésitez pas à consulter régulièrement nos offres sur notre site carrière.
            
            Cordialement,
            L'équipe de %s
            
            ---
            ID Candidature: %s
            """, 
            vars != null ? vars.get("candidateName") : "Candidat",
            notification.getJobTitle(),
            notification.getCompanyName(),
            notification.getCompanyName(),
            notification.getApplicationId()
        );
    }
    
    private String generateAcknowledgmentEmailContent(EmailNotificationDTO notification) {
        Map<String, Object> vars = notification.getTemplateVariables();
        return String.format("""
            Bonjour %s,
            
            Nous vous confirmons la bonne réception de votre candidature pour le poste de %s chez %s.
            
            Votre dossier est désormais en cours de traitement par notre équipe.
            
            Les prochaines étapes :
            ✅ Analyse de votre candidature : en cours
            ⏳ Étude de vos réponses : sous 48h
            ⏳ Retour : sous 5 jours ouvrés
            
            Nous vous remercions de l'intérêt que vous portez à notre entreprise et vous tiendrons informé de l'avancement de votre candidature.
            
            Cordialement,
            L'équipe recrutement de %s
            
            ---
            ID Candidature: %s
            Suivez l'avancement sur: %s/candidates/%s
            """, 
            vars != null ? vars.get("candidateName") : "Candidat",
            notification.getJobTitle(),
            notification.getCompanyName(),
            notification.getCompanyName(),
            notification.getApplicationId(),
            frontendUrl,
            notification.getApplicationId()
        );
    }
    
    private String generateInterviewReminderContent(EmailNotificationDTO notification) {
        Map<String, Object> vars = notification.getTemplateVariables();
        return String.format("""
            Bonjour %s,
            
            Rappelez-vous de votre entretien pour le poste de %s chez %s !
            
            📅 Date: %s
            ⏰ Heure: %s
            📍 Lieu: Visioconférence (lien envoyé séparément)
            
            Quelques conseils pour votre entretien :
            • Préparez la présentation de vos projets
            • Pensez à vos questions sur l'entreprise
            • Vérifiez votre connexion internet
            
            Nous sommes impatients de vous rencontrer !
            
            Cordialement,
            L'équipe de %s
            
            ---
            En cas d'empêchement, merci de nous prévenir au plus vite.
            """, 
            vars != null ? vars.get("candidateName") : "Candidat",
            notification.getJobTitle(),
            notification.getCompanyName(),
            vars != null ? vars.get("interviewDate") : "Date à définir",
            vars != null ? vars.get("interviewTime") : "Heure à définir",
            notification.getCompanyName()
        );
    }
    
    private String generateInterviewConfirmationContent(EmailNotificationDTO notification) {
        Map<String, Object> vars = notification.getTemplateVariables();
        return String.format("""
            Bonjour %s,
            
            Nous vous confirmons votre entretien pour le poste de %s chez %s.
            
            📅 Date: %s
            ⏰ Heure: %s
            📍 Modalités: Visioconférence
            
            Le lien de connexion sera envoyé 24h avant l'entretien.
            
            Programme prévisionnel :
            • 30 min : Présentation de l'entreprise et du poste
            • 45 min : Entretien technique avec l'équipe
            • 15 min : Vos questions
            
            Nous vous remercions de votre disponibilité et sommes impatients d'échanger avec vous.
            
            Cordialement,
            L'équipe de %s
            
            ---
            ID Candidature: %s
            """, 
            vars != null ? vars.get("candidateName") : "Candidat",
            notification.getJobTitle(),
            notification.getCompanyName(),
            vars != null ? vars.get("interviewDate") : "Date à définir",
            vars != null ? vars.get("interviewTime") : "Heure à définir",
            notification.getCompanyName(),
            notification.getApplicationId()
        );
    }
    
    private String generateRecruiterSummaryContent(EmailNotificationDTO notification) {
        Map<String, Object> vars = notification.getTemplateVariables();
        @SuppressWarnings("unchecked")
        Map<String, Object> summaryData = (Map<String, Object>) vars.get("summaryData");
        
        return String.format("""
            Bonjour,
            
            Voici votre résumé quotidien des candidatures :
            
            📊 Statistiques du jour :
            • Total candidatures : %s
            • Candidatures prioritaires : %s
            • À examiner : %s
            • Nouvelles candidatures : %s
            
            🔥 Actions recommandées :
            • Traiter les %s candidatures prioritaires en premier
            • Examiner les %s candidatures en attente
            • Planifier les entretiens pour les profils retenus
            
            Accédez au dashboard pour plus de détails : %s/dashboard
            
            Bonne journée !
            
            ---
            Assistant de préqualification automatique
            """, 
            summaryData != null ? summaryData.get("totalApplications") : "0",
            summaryData != null ? summaryData.get("priorityApplications") : "0",
            summaryData != null ? summaryData.get("reviewApplications") : "0",
            summaryData != null ? summaryData.get("newApplications") : "0",
            summaryData != null ? summaryData.get("priorityApplications") : "0",
            summaryData != null ? summaryData.get("reviewApplications") : "0",
            frontendUrl
        );
    }
    
    /**
     * Notifie un candidat retenu pour entretien
     */
    public EmailNotificationDTO sendCandidateRetentionEmail(UUID applicationId) {
        Application application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new RuntimeException("Candidature non trouvée"));
        
        Candidate candidate = application.getCandidate();
        Job job = application.getJob();
        
        EmailNotificationDTO notification = new EmailNotificationDTO();
        notification.setTo(candidate.getEmail());
        notification.setType(EmailNotificationDTO.NotificationType.CANDIDATE_RETENTION);
        notification.setApplicationId(applicationId.toString());
        notification.setCandidateName(candidate.getFirstName() + " " + candidate.getLastName());
        notification.setJobTitle(job.getTitle());
        notification.setCompanyName(job.getCompany().getName());
        
        notification.setSubject("Félicitations ! Votre candidature pour " + job.getTitle() + " a été retenue");
        notification.setTemplateName("candidate-retention");
        
        // Variables pour le template
        Map<String, Object> variables = new HashMap<>();
        variables.put("candidateName", candidate.getFirstName());
        variables.put("jobTitle", job.getTitle());
        variables.put("companyName", job.getCompany().getName());
        variables.put("applicationId", applicationId.toString());
        
        notification.setTemplateVariables(variables);
        
        boolean sent = sendEmail(notification);
        return notification;
    }
    
    /**
     * Notifie un candidat non retenu
     */
    public EmailNotificationDTO sendCandidateRejectionEmail(UUID applicationId) {
        Application application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new RuntimeException("Candidature non trouvée"));
        
        Candidate candidate = application.getCandidate();
        Job job = application.getJob();
        
        EmailNotificationDTO notification = new EmailNotificationDTO();
        notification.setTo(candidate.getEmail());
        notification.setType(EmailNotificationDTO.NotificationType.CANDIDATE_REJECTION);
        notification.setApplicationId(applicationId.toString());
        notification.setCandidateName(candidate.getFirstName() + " " + candidate.getLastName());
        notification.setJobTitle(job.getTitle());
        notification.setCompanyName(job.getCompany().getName());
        
        notification.setSubject("Suite à votre candidature pour " + job.getTitle());
        notification.setTemplateName("candidate-rejection");
        
        // Variables pour le template
        Map<String, Object> variables = new HashMap<>();
        variables.put("candidateName", candidate.getFirstName());
        variables.put("jobTitle", job.getTitle());
        variables.put("companyName", job.getCompany().getName());
        
        notification.setTemplateVariables(variables);
        
        boolean sent = sendEmail(notification);
        return notification;
    }
    
    /**
     * Test la configuration email
     */
    public boolean testEmailConfiguration(String testEmail) {
        EmailNotificationDTO testNotification = new EmailNotificationDTO();
        testNotification.setTo(testEmail);
        testNotification.setSubject("Test de configuration email");
        testNotification.setContent("Ceci est un test de configuration du système d'emails.");
        testNotification.setType(EmailNotificationDTO.NotificationType.CANDIDATE_ACKNOWLEDGMENT);
        
        return sendEmail(testNotification);
    }
}
