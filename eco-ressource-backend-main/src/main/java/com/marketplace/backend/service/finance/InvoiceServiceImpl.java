package com.marketplace.backend.service.finance;

import com.marketplace.backend.entity.finance.Invoice;
import com.marketplace.backend.entity.finance.escrow;
import com.marketplace.backend.entity.finance.EscrowStatus;
import com.marketplace.backend.repository.finance.InvoiceRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements IInvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final IEscrowService escrowService; // ✅ Utilise le service (pas le repo) → email automatique

    // ══════════════════════════════════════════════════
    //  CRÉER FACTURE → ESCROW AUTOMATIQUE
    // ══════════════════════════════════════════════════
    @Override
    @Transactional
    public Invoice addInvoice(Invoice invoice) {

        // 1️⃣ Calculer le montant TTC
        if (invoice.getAmountHT() != null && invoice.getTva() != null) {
            double ttc = invoice.getAmountHT() * (1 + invoice.getTva() / 100);
            invoice.setAmountTTC(Math.round(ttc * 1000.0) / 1000.0);
        }

        // 2️⃣ Sauvegarder la facture
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // 3️⃣ Créer automatiquement l'escrow lié
        escrow autoEscrow = new escrow();
        autoEscrow.setProject(savedInvoice.getProject());
        autoEscrow.setAmount(savedInvoice.getAmountTTC());
        autoEscrow.setStatus(EscrowStatus.LOCKED);
        autoEscrow.setCreatedAt(LocalDate.now().toString());
        autoEscrow.setLinkedInvoiceId(savedInvoice.getId());

        escrow savedEscrow = escrowService.addEscrow(autoEscrow);

        // 4️⃣ Lier l'escrow à la facture
        savedInvoice.setLinkedEscrowId(savedEscrow.getIdescrow());
        return invoiceRepository.save(savedInvoice);
    }

    // ══════════════════════════════════════════════════
    //  CONFIRMER LIVRAISON → PAID + ESCROW RELEASED + 📧 EMAIL
    // ══════════════════════════════════════════════════
    @Override
    @Transactional
    public Invoice markAsPaid(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facture introuvable : " + id));

        // 1️⃣ Marquer la facture comme PAID
        invoice.setStatus("PAID");
        invoice.setDeliveredAt(LocalDate.now().toString());
        invoiceRepository.save(invoice);

        // 2️⃣ Libérer l'escrow via IEscrowService
        //    → déclenche EscrowReleasedEvent → EscrowEmailListener → 📧 email
        if (invoice.getLinkedEscrowId() != null) {
            try {
                escrowService.releaseEscrow(invoice.getLinkedEscrowId());
            } catch (Exception ex) {
                org.slf4j.LoggerFactory.getLogger(InvoiceServiceImpl.class)
                        .warn("[INVOICE] Impossible de liberer l'escrow #{} : {}",
                                invoice.getLinkedEscrowId(), ex.getMessage());
            }
        }

        return invoice;
    }

    // ══════════════════════════════════════════════════
    //  MODIFIER FACTURE + MÀJ ESCROW
    // ══════════════════════════════════════════════════
    @Override
    @Transactional
    public Invoice updateInvoice(Invoice invoice) {
        if (invoice.getAmountHT() != null && invoice.getTva() != null) {
            double ttc = invoice.getAmountHT() * (1 + invoice.getTva() / 100);
            invoice.setAmountTTC(Math.round(ttc * 1000.0) / 1000.0);
        }

        Invoice saved = invoiceRepository.save(invoice);

        // Mettre à jour le montant de l'escrow lié si encore LOCKED
        if (saved.getLinkedEscrowId() != null) {
            try {
                escrow linkedEscrow = escrowService.retrieveEscrow(saved.getLinkedEscrowId());
                if (linkedEscrow != null && linkedEscrow.getStatus() == EscrowStatus.LOCKED) {
                    linkedEscrow.setAmount(saved.getAmountTTC());
                    escrowService.modifyEscrow(linkedEscrow);
                }
            } catch (Exception ex) {
                org.slf4j.LoggerFactory.getLogger(InvoiceServiceImpl.class)
                        .warn("[INVOICE] Impossible de mettre a jour l'escrow #{} : {}",
                                saved.getLinkedEscrowId(), ex.getMessage());
            }
        }

        return saved;
    }

    // ══════════════════════════════════════════════════
    //  SUPPRIMER FACTURE + ESCROW LIÉ
    // ══════════════════════════════════════════════════
    @Override
    @Transactional
    public void deleteInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facture introuvable : " + id));

        // Supprimer l'escrow lié si encore LOCKED
        if (invoice.getLinkedEscrowId() != null) {
            try {
                escrow linkedEscrow = escrowService.retrieveEscrow(invoice.getLinkedEscrowId());
                if (linkedEscrow != null && linkedEscrow.getStatus() == EscrowStatus.LOCKED) {
                    escrowService.removeEscrow(linkedEscrow.getIdescrow());
                }
            } catch (Exception ex) {
                org.slf4j.LoggerFactory.getLogger(InvoiceServiceImpl.class)
                        .warn("[INVOICE] Impossible de supprimer l'escrow #{} : {}",
                                invoice.getLinkedEscrowId(), ex.getMessage());
            }
        }

        invoiceRepository.deleteById(id);
    }

    // ══════════════════════════════════════════════════
    //  LECTURE
    // ══════════════════════════════════════════════════
    @Override
    public List<Invoice> retrieveAllInvoices() {
        return invoiceRepository.findAll();
    }

    @Override
    public Invoice retrieveInvoice(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facture introuvable : " + id));
    }
}
