package com.marketplace.backend.service;

import com.marketplace.backend.event.EscrowReleasedEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

/**
 * Service responsable de la construction et de l'envoi des emails HTML
 * à l'aide de Thymeleaf comme moteur de templates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from:noreply@eco-ressource.com}")
    private String fromAddress;

    public boolean sendHtmlEmail(String recipientEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);

            log.info("[EMAIL] Notification HTML envoyee a {}", recipientEmail);
            return true;
        } catch (Exception e) {
            log.error("[EMAIL] Erreur envoi email HTML vers {} : {}",
                    recipientEmail, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Envoie un email HTML au responsable financier de l'entreprise
     * informant que l'escrow a été libéré.
     *
     * @param event les données de l'escrow libéré
     */
    public void sendEscrowReleasedEmail(EscrowReleasedEvent event) {
        try {
            // Préparer le contexte Thymeleaf
            Context ctx = new Context(Locale.FRENCH);
            ctx.setVariable("recipientName",  event.recipientName());
            ctx.setVariable("projectName",    event.projectName());
            ctx.setVariable("escrowId",       event.escrowId());
            ctx.setVariable("amount",         String.format("%,.2f", event.amount()).replace(",", " "));
            ctx.setVariable("releasedAt",     event.releasedAt());

            // Générer le corps HTML
            String htmlBody = templateEngine.process("escrow-released", ctx);

            // Construire le MimeMessage
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(event.recipientEmail());
            helper.setSubject("✅ Fonds Libérés — Commande \"" + event.projectName() + "\" confirmée");
            helper.setText(htmlBody, true);

            mailSender.send(message);

            log.info("[EMAIL] ✅ Notification escrow #{} envoyée à {}", event.escrowId(), event.recipientEmail());

        } catch (MessagingException e) {
            log.error("[EMAIL] ❌ Erreur envoi email escrow #{} → {} : {}",
                    event.escrowId(), event.recipientEmail(), e.getMessage(), e);
        }
    }
}
