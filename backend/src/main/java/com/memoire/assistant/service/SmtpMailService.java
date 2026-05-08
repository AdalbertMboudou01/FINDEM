package com.memoire.assistant.service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmtpMailService {
    @Value("${app.email.host:}")
    private String host;

    @Value("${app.email.port:587}")
    private int port;

    @Value("${app.email.username:}")
    private String username;

    @Value("${app.email.password:}")
    private String password;

    @Value("${app.email.from:}")
    private String from;

    public boolean isConfigured() {
        return hasText(host) && hasText(username) && hasText(password) && hasText(resolveFrom());
    }

    public void sendTextEmail(String to, String subject, String body) {
        if (!isConfigured()) {
            throw new IllegalStateException("Configuration SMTP incomplete");
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(resolveFrom()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
            message.setSubject(subject, "UTF-8");
            message.setText(body, "UTF-8");
            Transport.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Impossible d'envoyer l'email SMTP", e);
        }
    }

    private String resolveFrom() {
        return hasText(from) ? from.trim() : username;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
