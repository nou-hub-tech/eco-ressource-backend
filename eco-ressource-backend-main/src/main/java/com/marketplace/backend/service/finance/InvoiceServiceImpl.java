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
    private final IEscrowService escrowService;

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
        savedInvoice.setLinkedEscrowId(savedEscrow.getIdescrow()); // ✅ FIX: getIdescrow()
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
                // L'escrow peut être déjà libéré ou introuvable — on log sans bloquer
                org.slf4j.LoggerFactory.getLogger(InvoiceServiceImpl.class)
                        .warn("[INVOICE] ⚠️ Impossible de libérer l'escrow #{} : {}",
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

        if (saved.getLinkedEscrowId() != null) {
            escrowRepository.findById(saved.getLinkedEscrowId()).ifPresent(esc -> {
                if (esc.getStatus() == EscrowStatus.LOCKED) {
                    esc.setAmount(saved.getAmountTTC());
                    escrowRepository.save(esc);
                }
            });
        }

        return saved;
    }

    // ══════════════════════════════════════════════════
    //  SUPPRIMER FACTURE + ESCROW LIÉ   ✅ FIX: deleteInvoice (pas removeInvoice)
    // ══════════════════════════════════════════════════
    @Override
    @Transactional
    public void deleteInvoice(Long id) {  // ✅ FIX: nom correct selon l'interface
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facture introuvable : " + id));

        if (invoice.getLinkedEscrowId() != null) {
            escrowRepository.findById(invoice.getLinkedEscrowId()).ifPresent(esc -> {
                if (esc.getStatus() == EscrowStatus.LOCKED) {
                    escrowRepository.deleteById(esc.getIdescrow()); // ✅ FIX: getIdescrow()
                }
            });
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
