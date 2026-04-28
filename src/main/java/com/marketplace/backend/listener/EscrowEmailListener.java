package com.marketplace.backend.listener;

import com.marketplace.backend.event.EscrowReleasedEvent;
import com.marketplace.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener asynchrone qui réagit à l'événement EscrowReleasedEvent.
 *
 * - @Async          → l'envoi de l'email ne bloque pas la réponse HTTP
 * - AFTER_COMMIT    → l'email n'est envoyé qu'après validation en base de données
 *                     (évite les faux positifs en cas de rollback)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EscrowEmailListener {

    private final EmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEscrowReleased(EscrowReleasedEvent event) {
        log.info("[LISTENER] 📨 Envoi email libération escrow #{} vers {}",
                event.escrowId(), event.recipientEmail());
        emailService.sendEscrowReleasedEmail(event);
    }
}
