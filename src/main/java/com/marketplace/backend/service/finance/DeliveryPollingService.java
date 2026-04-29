package com.marketplace.backend.service.finance;

import com.marketplace.backend.entity.finance.EscrowStatus;
import com.marketplace.backend.entity.finance.Invoice;
import com.marketplace.backend.entity.finance.escrow;
import com.marketplace.backend.entity.enums.StatutCommande;
import com.marketplace.backend.event.EscrowReleasedEvent;
import com.marketplace.backend.repository.DeliveryOrderRepo;
import com.marketplace.backend.repository.UserRepository;
import com.marketplace.backend.repository.finance.EscrowRepository;
import com.marketplace.backend.repository.finance.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 🚚 DeliveryPollingService — Polling automatique toutes les 30 secondes
 *
 * FLUX COMPLET :
 * ─────────────────────────────────────────────────────────────────
 * Toutes les 30s :
 *   1. Récupère les factures UNPAID avec deliveryOrderId non-null
 *   2. Pour chaque facture → vérifie si la livraison est LIVREE
 *   3. Si LIVREE :
 *      → Facture  : UNPAID → PAID + deliveredAt = maintenant
 *      → Escrow   : LOCKED → RELEASED (via linkedEscrowId ou matching projet)
 *      → Email    : EscrowReleasedEvent envoyé à l'enterprise
 * ─────────────────────────────────────────────────────────────────
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryPollingService {

    private final InvoiceRepository invoiceRepository;
    private final EscrowRepository escrowRepository;
    private final DeliveryOrderRepo deliveryOrderRepo;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** Timestamp du dernier check (accessible via API si besoin) */
    private LocalDateTime lastCheckTime = null;

    public LocalDateTime getLastCheckTime() { return lastCheckTime; }

    // ──────────────────────────────────────────────────────────────
    // SCHEDULER : toutes les 30 secondes
    // ──────────────────────────────────────────────────────────────

    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void checkDeliveriesAndRelease() {
        lastCheckTime = LocalDateTime.now();

        // 1. Factures UNPAID liées à une livraison
        List<Invoice> candidates = invoiceRepository.findUnpaidWithDelivery();

        if (candidates.isEmpty()) {
            log.debug("[POLLING] Aucune facture UNPAID avec livraison à vérifier");
            return;
        }

        log.debug("[POLLING] 🔍 {} facture(s) UNPAID liée(s) à une livraison à vérifier", candidates.size());

        for (Invoice invoice : candidates) {
            try {
                processInvoiceDelivery(invoice);
            } catch (Exception ex) {
                log.error("[POLLING] ❌ Erreur traitement facture #{} : {}", invoice.getId(), ex.getMessage());
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // TRAITEMENT D'UNE FACTURE
    // ──────────────────────────────────────────────────────────────

    private void processInvoiceDelivery(Invoice invoice) {
        deliveryOrderRepo.findById(invoice.getDeliveryOrderId()).ifPresent(delivery -> {

            if (!StatutCommande.LIVREE.equals(delivery.getStatut())) {
                return; // Pas encore livrée
            }

            log.info("[POLLING] ✅ Livraison #{} LIVREE — Cascade facture #{} / projet '{}'",
                    delivery.getIdDelivery(), invoice.getId(), invoice.getProject());

            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            // ── Étape A : Marquer la facture PAID ──────────────────
            invoice.setStatus("PAID");
            invoice.setDeliveredAt(now);
            invoiceRepository.save(invoice);
            log.info("[POLLING] 📄 Facture #{} → PAID", invoice.getId());

            // ── Étape B : Libérer l'escrow lié ────────────────────
            escrow releasedEscrow = findAndReleaseEscrow(invoice);

            // ── Étape C : Envoyer l'email de libération ────────────
            if (releasedEscrow != null) {
                sendReleaseNotification(releasedEscrow);
            }
        });
    }

    // ──────────────────────────────────────────────────────────────
    // LIBÉRATION DE L'ESCROW
    // Priorité 1 : linkedEscrowId (lien direct et précis)
    // Priorité 2 : matching par projet (fallback)
    // ──────────────────────────────────────────────────────────────

    private escrow findAndReleaseEscrow(Invoice invoice) {
        escrow target = null;

        // Priorité 1 : lien direct
        if (invoice.getLinkedEscrowId() != null) {
            target = escrowRepository.findById(invoice.getLinkedEscrowId()).orElse(null);
        }

        // Priorité 2 : matching par projet
        if (target == null) {
            target = escrowRepository.findAll().stream()
                    .filter(e -> EscrowStatus.LOCKED.equals(e.getStatus()))
                    .filter(e -> e.getProject() != null && e.getProject().equalsIgnoreCase(invoice.getProject()))
                    .findFirst()
                    .orElse(null);
        }

        if (target == null) {
            log.warn("[POLLING] ⚠️ Aucun escrow LOCKED trouvé pour facture #{} projet '{}'",
                    invoice.getId(), invoice.getProject());
            return null;
        }

        if (!EscrowStatus.LOCKED.equals(target.getStatus())) {
            log.debug("[POLLING] Escrow #{} déjà libéré", target.getIdescrow());
            return null;
        }

        target.setStatus(EscrowStatus.RELEASED);
        target.setReleaseDate(LocalDate.now().toString());
        escrowRepository.save(target);
        log.info("[POLLING] 💸 Escrow #{} '{}' → RELEASED", target.getIdescrow(), target.getProject());
        return target;
    }

    // ──────────────────────────────────────────────────────────────
    // EMAIL DE LIBÉRATION
    // ──────────────────────────────────────────────────────────────

    private void sendReleaseNotification(escrow esc) {
        // Cherche un utilisateur lié à l'entreprise de l'escrow
        userRepository.findAll().stream()
                .filter(u -> u.getEnterprise() != null
                        && esc.getEnterpriseId() != null
                        && esc.getEnterpriseId().equals(u.getEnterprise().getId()))
                .findFirst()
                .ifPresentOrElse(
                    user -> {
                        EscrowReleasedEvent event = new EscrowReleasedEvent(
                                esc.getIdescrow(),
                                esc.getProject(),
                                esc.getAmount(),
                                esc.getReleaseDate(),
                                user.getEmail(),
                                user.getFullName()
                        );
                        eventPublisher.publishEvent(event);
                        log.info("[POLLING] 📧 Email libération envoyé à : {}", user.getEmail());
                    },
                    () -> log.warn("[POLLING] ⚠️ Aucun utilisateur trouvé pour enterprise #{}", esc.getEnterpriseId())
                );
    }
}
