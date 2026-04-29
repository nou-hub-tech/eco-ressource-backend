package com.marketplace.backend.service;

import com.marketplace.backend.entity.finance.EscrowStatus;
import com.marketplace.backend.entity.finance.Invoice;
import com.marketplace.backend.entity.finance.escrow;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.event.EscrowReleasedEvent;
import com.marketplace.backend.repository.UserRepository;
import com.marketplace.backend.repository.finance.EscrowRepository;
import com.marketplace.backend.repository.finance.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 🎯 DeliveryCompletionService
 *
 * Appelé immédiatement quand un transporteur termine une livraison (statut → LIVREE).
 *
 * CASCADE DÉCLENCHÉE :
 *  1. Facture  UNPAID  → Encaissée  (deliveryOrderId = id livraison)
 *  2. Escrow   LOCKED  → RELEASED   (linkedEscrowId ou matching projet)
 *  3. Email             envoyé au vendeur (EscrowReleasedEvent)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryCompletionService {

    private final InvoiceRepository invoiceRepository;
    private final EscrowRepository  escrowRepository;
    private final UserRepository    userRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ── Statut "payée" attendu par la base de données et le frontend ──
    // Le frontend affiche "✅ Encaissée" quand status === 'PAID'
    private static final String INVOICE_PAID_STATUS = "PAID";

    // ─────────────────────────────────────────────────────────────
    // Point d'entrée : appelé depuis DeliveryOrderServiceImpl
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void onDeliveryCompleted(Long deliveryOrderId, String clientName) {
        log.info("[DELIVERY-CASCADE] 🚚 Livraison #{} terminée — client='{}'", deliveryOrderId, clientName);

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        // ════════════════════════════════════════════════════
        // STRATÉGIE 1 : escrow.deliveryOrderId = id livraison
        // (lien direct si renseigné à la création de l'escrow)
        // ════════════════════════════════════════════════════
        List<escrow> directEscrows = escrowRepository
                .findByDeliveryOrderIdAndStatus(deliveryOrderId, EscrowStatus.LOCKED);

        if (!directEscrows.isEmpty()) {
            log.info("[DELIVERY-CASCADE] ✅ Stratégie 1: {} escrow(s) trouvé(s) par deliveryOrderId", directEscrows.size());
            directEscrows.forEach(esc -> releaseAndNotify(esc, now, deliveryOrderId));
            return;
        }

        // ════════════════════════════════════════════════════
        // STRATÉGIE 2 : escrow.project = delivery.nomClient
        // EX : escrow projet "samra" + delivery nomClient "samra"
        // C'est le cas le plus courant quand l'escrow est nommé
        // d'après le client de la livraison.
        // ════════════════════════════════════════════════════
        if (clientName != null && !clientName.isBlank()) {
            List<escrow> byClientName = escrowRepository.findAll().stream()
                    .filter(e -> EscrowStatus.LOCKED.equals(e.getStatus()))
                    .filter(e -> clientName.equalsIgnoreCase(e.getProject()))
                    .collect(java.util.stream.Collectors.toList());

            if (!byClientName.isEmpty()) {
                log.info("[DELIVERY-CASCADE] ✅ Stratégie 2: {} escrow(s) trouvé(s) par project=clientName '{}'",
                        byClientName.size(), clientName);
                byClientName.forEach(esc -> releaseAndNotify(esc, now, deliveryOrderId));
                return;
            }
        }

        // ════════════════════════════════════════════════════
        // STRATÉGIE 3 : via facture (invoice.project = escrow.project)
        // ════════════════════════════════════════════════════
        List<Invoice> invoices = invoiceRepository.findByDeliveryOrderId(deliveryOrderId);
        if (invoices.isEmpty()) {
            invoices = invoiceRepository.findByClientNameAndStatus(clientName, "UNPAID");
        }
        if (invoices.isEmpty()) {
            invoices = invoiceRepository.findByClientNameAndStatus(clientName, "PAID");
        }

        if (!invoices.isEmpty()) {
            log.info("[DELIVERY-CASCADE] ✅ Stratégie 3: {} facture(s) trouvée(s) pour client '{}'",
                    invoices.size(), clientName);
            for (Invoice invoice : invoices) {
                if (!"PAID".equals(invoice.getStatus())) {
                    invoice.setStatus(INVOICE_PAID_STATUS);
                    invoice.setDeliveredAt(now);
                    invoiceRepository.save(invoice);
                    log.info("[DELIVERY-CASCADE] 📄 Facture #{} → PAID", invoice.getId());
                }
                escrow released = findAndReleaseEscrow(invoice);
                if (released != null) {
                    sendEmail(released);
                }
            }
            return;
        }

        log.warn("[DELIVERY-CASCADE] ⚠️ Aucun escrow LOCKED trouvé pour delivery #{} / client '{}'",
                deliveryOrderId, clientName);
    }

    /** Libère un escrow et envoie l'email vendeur */
    private void releaseAndNotify(escrow esc, String now, Long deliveryOrderId) {
        esc.setStatus(EscrowStatus.RELEASED);
        esc.setReleaseDate(LocalDate.now().toString());
        escrowRepository.save(esc);
        log.info("[DELIVERY-CASCADE] 💸 Escrow #{} '{}' → RELEASED (via deliveryOrderId #{})",
                esc.getIdescrow(), esc.getProject(), deliveryOrderId);

        // Mettre aussi la facture liée à PAID
        if (esc.getLinkedInvoiceId() != null) {
            invoiceRepository.findById(esc.getLinkedInvoiceId()).ifPresent(inv -> {
                if (!"PAID".equals(inv.getStatus())) {
                    inv.setStatus(INVOICE_PAID_STATUS);
                    inv.setDeliveredAt(LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                    invoiceRepository.save(inv);
                    log.info("[DELIVERY-CASCADE] 📄 Facture #{} → PAID", inv.getId());
                }
            });
        }

        // Email en dehors de la transaction principale (ne bloque pas le release)
        try {
            sendEmail(esc);
        } catch (Exception e) {
            log.error("[DELIVERY-CASCADE] ⚠️ Email non envoyé (escrow déjà libéré) : {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Libération de l'escrow
    // Priorité 1 : linkedEscrowId (lien direct)
    // Priorité 2 : matching par projet (fallback)
    // ─────────────────────────────────────────────────────────────

    private escrow findAndReleaseEscrow(Invoice invoice) {
        escrow target = null;

        if (invoice.getLinkedEscrowId() != null) {
            target = escrowRepository.findById(invoice.getLinkedEscrowId()).orElse(null);
        }

        if (target == null && invoice.getProject() != null) {
            target = escrowRepository.findAll().stream()
                    .filter(e -> EscrowStatus.LOCKED.equals(e.getStatus()))
                    .filter(e -> invoice.getProject().equalsIgnoreCase(e.getProject()))
                    .findFirst().orElse(null);
        }

        if (target == null || !EscrowStatus.LOCKED.equals(target.getStatus())) {
            log.debug("[DELIVERY-CASCADE] Pas d'escrow LOCKED pour facture #{}", invoice.getId());
            return null;
        }

        target.setStatus(EscrowStatus.RELEASED);
        target.setReleaseDate(LocalDate.now().toString());
        escrowRepository.save(target);
        log.info("[DELIVERY-CASCADE] 💸 Escrow #{} '{}' → RELEASED", target.getIdescrow(), target.getProject());
        return target;
    }

    // ─────────────────────────────────────────────────────────────
    // Email responsable financier via EscrowReleasedEvent (async)
    //
    // Priorité 1 : user.enterprise.id = escrow.enterpriseId
    // Priorité 2 : user.fullName = escrow.project (ou invoice.sellerName)
    // Priorité 3 : premier user ROLE_ENTERPRISE (fallback garanti)
    // ─────────────────────────────────────────────────────────────

    private void sendEmail(escrow esc) {
        List<com.marketplace.backend.entity.User> allUsers = userRepository.findAll();

        // Priorité 1 : matching par enterpriseId
        com.marketplace.backend.entity.User recipient = allUsers.stream()
                .filter(u -> u.getEnterprise() != null
                        && esc.getEnterpriseId() != null
                        && esc.getEnterpriseId().equals(u.getEnterprise().getId()))
                .findFirst().orElse(null);

        // Priorité 2 : fallback → premier utilisateur ROLE_ENTERPRISE
        if (recipient == null) {
            recipient = allUsers.stream()
                    .filter(u -> Role.ROLE_ENTERPRISE.equals(u.getRole()))
                    .findFirst().orElse(null);
            if (recipient != null) {
                log.info("[DELIVERY-CASCADE] 📧 Fallback: email envoyé au responsable entreprise : {}",
                        recipient.getEmail());
            }
        }

        if (recipient == null) {
            log.warn("[DELIVERY-CASCADE] ⚠️ Aucun destinataire trouvé — email non envoyé pour escrow #{}",
                    esc.getIdescrow());
            return;
        }

        eventPublisher.publishEvent(new EscrowReleasedEvent(
                esc.getIdescrow(),
                esc.getProject(),
                esc.getAmount(),
                esc.getReleaseDate(),
                recipient.getEmail(),
                recipient.getFullName()
        ));
        log.info("[DELIVERY-CASCADE] 📧 Email libération escrow envoyé à : {}", recipient.getEmail());
    }
}
