package com.memoire.assistant.service;

import com.memoire.assistant.model.Application;
import com.memoire.assistant.model.Candidate;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.repository.CandidateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.scheduling.annotation.Async;
import java.util.Properties;
import java.util.UUID;

@Service
public class NotificationService {
    
    @Autowired
    private ApplicationRepository applicationRepository;
    
    @Autowired
    private CandidateRepository candidateRepository;
    
    @Value("${app.email.host:smtp.gmail.com}")
    private String emailHost;
    
    @Value("${app.email.port:587}")
    private int emailPort;
    
    @Value("${app.email.username}")
    private String emailUsername;
    
    @Value("${app.email.password}")
    private String emailPassword;
    
    @Value("${app.email.from:noreply@assistant-prequalification.com}")
    private String emailFrom;
    
    @Value("${app.frontend.url:http://localhost:3100}")
    private String frontendUrl;
    
    public void sendApplicationStatusUpdate(UUID applicationId, String newStatus) {
        Application application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new RuntimeException("Application non trouvée"));
        
        Candidate candidate = application.getCandidate();
        String subject = "Mise à jour de votre candidature - " + application.getJob().getTitle();
        String content = generateStatusUpdateEmail(application, candidate, newStatus);
        
        sendEmail(candidate.getEmail(), subject, content);
    }
    
    public void sendInterviewInvitation(UUID applicationId, String interviewDetails) {
        Application application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new RuntimeException("Application non trouvée"));
        
        Candidate candidate = application.getCandidate();
        String subject = "Invitation à un entretien - " + application.getJob().getTitle();
        String content = generateInterviewInvitationEmail(application, candidate, interviewDetails);
        
        sendEmail(candidate.getEmail(), subject, content);
    }
    
    public void sendRejectionNotification(UUID applicationId) {
        Application application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new RuntimeException("Application non trouvée"));
        
        Candidate candidate = application.getCandidate();
        String subject = "Suite à votre candidature - " + application.getJob().getTitle();
        String content = generateRejectionEmail(application, candidate);
        
        sendEmail(candidate.getEmail(), subject, content);
    }
    
    public void sendPoolNotification(UUID candidateId, String jobTitle) {
        Candidate candidate = candidateRepository.findById(candidateId)
            .orElseThrow(() -> new RuntimeException("Candidat non trouvé"));
        
        String subject = "Nouvelle opportunité qui pourrait vous intéresser";
        String content = generatePoolNotificationEmail(candidate, jobTitle);
        
        sendEmail(candidate.getEmail(), subject, content);
    }
    
    private String generateStatusUpdateEmail(Application application, Candidate candidate, String newStatus) {
        StringBuilder content = new StringBuilder();
        
        content.append("<html><body>");
        content.append("<h2>Mise à jour de votre candidature</h2>");
        content.append("<p>Bonjour ").append(candidate.getFirstName()).append(" ").append(candidate.getLastName()).append(",</p>");
        
        content.append("<p>Nous vous informons que votre candidature pour le poste de ");
        content.append("<strong>").append(application.getJob().getTitle()).append("</strong>");
        content.append(" chez ").append(application.getJob().getCompany().getName());
        content.append(" a été mise à jour.</p>");
        
        content.append("<p><strong>Nouveau statut : </strong>").append(newStatus).append("</p>");
        
        content.append("<p>Vous pouvez suivre l'évolution de votre candidature en vous connectant à notre plateforme.</p>");
        
        content.append("<p>Cordialement,<br>");
        content.append("L'équipe de recrutement<br>");
        content.append(application.getJob().getCompany().getName()).append("</p>");
        
        content.append("</body></html>");
        
        return content.toString();
    }
    
    private String generateInterviewInvitationEmail(Application application, Candidate candidate, String interviewDetails) {
        StringBuilder content = new StringBuilder();
        
        content.append("<html><body>");
        content.append("<h2>Invitation à un entretien</h2>");
        content.append("<p>Bonjour ").append(candidate.getFirstName()).append(" ").append(candidate.getLastName()).append(",</p>");
        
        content.append("<p>Nous avons le plaisir de vous convier à un entretien pour le poste de ");
        content.append("<strong>").append(application.getJob().getTitle()).append("</strong>");
        content.append(" chez ").append(application.getJob().getCompany().getName()).append(".</p>");
        
        content.append("<h3>Détails de l'entretien :</h3>");
        content.append("<p>").append(interviewDetails.replace("\n", "<br>")).append("</p>");
        
        content.append("<p>Veuillez confirmer votre présence en répondant à cet email.</p>");
        
        content.append("<p>Nous vous remercions de votre intérêt et attendons votre retour avec impatience.</p>");
        
        content.append("<p>Cordialement,<br>");
        content.append("L'équipe de recrutement<br>");
        content.append(application.getJob().getCompany().getName()).append("</p>");
        
        content.append("</body></html>");
        
        return content.toString();
    }
    
    private String generateRejectionEmail(Application application, Candidate candidate) {
        StringBuilder content = new StringBuilder();
        
        content.append("<html><body>");
        content.append("<h2>Suite à votre candidature</h2>");
        content.append("<p>Bonjour ").append(candidate.getFirstName()).append(" ").append(candidate.getLastName()).append(",</p>");
        
        content.append("<p>Nous vous remercions de l'intérêt que vous avez porté au poste de ");
        content.append("<strong>").append(application.getJob().getTitle()).append("</strong>");
        content.append(" chez ").append(application.getJob().getCompany().getName()).append(".</p>");
        
        content.append("<p>Après étude attentive de votre profil, nous regrettons de vous informer que nous ne poursuivons pas votre candidature pour cette fois.</p>");
        
        content.append("<p>Nous conservons votre CV au sein de notre vivier et ne manquerons pas de vous recontacter si une autre opportunité correspond à votre profil.</p>");
        
        content.append("<p>Nous vous souhaitons beaucoup de succès dans votre recherche d'alternance.</p>");
        
        content.append("<p>Cordialement,<br>");
        content.append("L'équipe de recrutement<br>");
        content.append(application.getJob().getCompany().getName()).append("</p>");
        
        content.append("</body></html>");
        
        return content.toString();
    }
    
    private String generatePoolNotificationEmail(Candidate candidate, String jobTitle) {
        StringBuilder content = new StringBuilder();
        
        content.append("<html><body>");
        content.append("<h2>Nouvelle opportunité d'alternance</h2>");
        content.append("<p>Bonjour ").append(candidate.getFirstName()).append(" ").append(candidate.getLastName()).append(",</p>");
        
        content.append("<p>Nous avons identifié une nouvelle opportunité qui pourrait correspondre à votre profil :</p>");
        
        content.append("<p><strong>Poste : </strong>").append(jobTitle).append("</p>");
        
        content.append("<p>Si vous êtes intéressé(e), n'hésitez pas à postuler via notre plateforme.</p>");
        
        content.append("<p>Pour consulter cette offre et d'autres opportunités : ");
        content.append("<a href=\"").append(frontendUrl).append("\">Cliquez ici</a></p>");
        
        content.append("<p>Cordialement,<br>");
        content.append("L'équipe Assistant de Préqualification</p>");
        
        content.append("</body></html>");
        
        return content.toString();
    }
    
    @Async
    protected void sendEmail(String to, String subject, String content) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", emailHost);
            props.put("mail.smtp.port", emailPort);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            
            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(emailUsername, emailPassword);
                }
            });
            
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(emailFrom));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(content, "text/html");
            
            Transport.send(message);
            
        } catch (MessagingException e) {
            System.err.println("Erreur lors de l'envoi de l'email : " + e.getMessage());
            // Ne pas lancer d'exception en mode async pour éviter de bloquer le thread principal
        }
    }
    
    public void sendApplicationConfirmation(UUID applicationId) {
        Application application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new RuntimeException("Application non trouvée"));
        
        Candidate candidate = application.getCandidate();
        String subject = "Confirmation de réception de votre candidature - " + application.getJob().getTitle();
        String content = generateConfirmationEmail(application, candidate);
        
        sendEmail(candidate.getEmail(), subject, content);
    }
    
    private String generateConfirmationEmail(Application application, Candidate candidate) {
        StringBuilder content = new StringBuilder();
        
        content.append("<html><body>");
        content.append("<h2>Confirmation de réception de votre candidature</h2>");
        content.append("<p>Bonjour ").append(candidate.getFirstName()).append(" ").append(candidate.getLastName()).append(",</p>");
        
        content.append("<p>Nous vous confirmons la bonne réception de votre candidature pour le poste de ");
        content.append("<strong>").append(application.getJob().getTitle()).append("</strong>");
        content.append(" chez ").append(application.getJob().getCompany().getName()).append(".</p>");
        
        content.append("<p>Votre candidature est actuellement en cours d'étude par notre équipe de recrutement.</p>");
        
        content.append("<p>Nous vous tiendrons informé(e) de l'évolution de votre dossier dans les plus brefs délais.</p>");
        
        content.append("<p>Nous vous remercions de l'intérêt que vous portez à notre entreprise.</p>");
        
        content.append("<p>Cordialement,<br>");
        content.append("L'équipe de recrutement<br>");
        content.append(application.getJob().getCompany().getName()).append("</p>");
        
        content.append("</body></html>");
        
        return content.toString();
    }
}
