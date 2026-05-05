package com.marketplace.backend.controller.finance;

import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.finance.Invoice;
import com.marketplace.backend.entity.finance.InvoiceType;
import com.marketplace.backend.repository.EnterpriseRepository;
import com.marketplace.backend.repository.UserRepository;
import com.marketplace.backend.repository.finance.InvoiceRepository;
import com.marketplace.backend.service.finance.ClientSolvabilityService;
import com.marketplace.backend.service.finance.IInvoiceService;
import com.marketplace.backend.service.finance.InvoiceChatService;
import com.marketplace.backend.service.finance.InvoiceRiskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/invoices")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class InvoiceController {

    private final IInvoiceService invoiceService;
    private final InvoiceRiskService invoiceRiskService;
    private final ClientSolvabilityService solvabilityService;
    private final InvoiceChatService chatService;

    private final UserRepository userRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final InvoiceRepository invoiceRepository;

    // ═══════════════════════════════════════════════════════════
    //  CRUD standard
    // ═══════════════════════════════════════════════════════════

    /**
     * POST /api/invoices/add
     * Sauvegarde directement via invoiceRepository (bypass InvoiceServiceImpl)
     * pour éviter les problèmes @Transactional liés à l'escrow automatique.
     */
    @PostMapping("/add")
    public ResponseEntity<?> addInvoice(@RequestBody Invoice invoice) {
        try {
            String companyName = getCurrentCompanyName();

            // 1. Auto-détection du type si non fourni
            if (invoice.getInvoiceType() == null && companyName != null) {
                if (companyName.equalsIgnoreCase(invoice.getSellerName())) {
                    invoice.setInvoiceType(InvoiceType.VENTE);
                } else if (companyName.equalsIgnoreCase(invoice.getClientName())) {
                    invoice.setInvoiceType(InvoiceType.ACHAT);
                }
            }
            if (invoice.getInvoiceType() == null) {
                invoice.setInvoiceType(InvoiceType.VENTE);
            }

            // 2. Forcer l'association à l'entreprise connectée
            if (companyName != null) {
                if (invoice.getInvoiceType() == InvoiceType.VENTE) {
                    invoice.setSellerName(companyName);
                } else {
                    invoice.setClientName(companyName);
                }
            }

            // 3. Toujours régénérer le numéro côté backend (jamais de doublon)
            invoice.setInvoiceNumber(generateNextNumber(invoice.getInvoiceType()));

            // 4. Calculer le montant TTC
            if (invoice.getAmountHT() != null && invoice.getTva() != null) {
                double ttc = invoice.getAmountHT() * (1.0 + invoice.getTva() / 100.0);
                invoice.setAmountTTC(Math.round(ttc * 1000.0) / 1000.0);
            }

            // 5. Sauvegarder directement (pas de logique escrow ici)
            Invoice saved = invoiceRepository.save(invoice);
            log.info("[INVOICE] ✅ Créée : #{} id={}", saved.getInvoiceNumber(), saved.getId());
            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            log.error("[INVOICE] ❌ Erreur création : {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "error", "Erreur création facture",
                            "detail", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()
                    ));
        }
    }

    /**
     * GET /api/invoices/next-number/{type}
     * Prochain numéro de facture disponible : VTE-2026-001 ou ACH-2026-001
     */
    @GetMapping("/next-number/{type}")
    public ResponseEntity<Map<String, String>> getNextNumber(@PathVariable String type) {
        try {
            InvoiceType invoiceType = InvoiceType.valueOf(type.toUpperCase());
            return ResponseEntity.ok(Map.of("number", generateNextNumber(invoiceType)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private String generateNextNumber(InvoiceType type) {
        if (type == null) type = InvoiceType.VENTE;
        String year   = String.valueOf(java.time.LocalDate.now().getYear());
        String prefix = type == InvoiceType.VENTE ? "VTE" : "ACH";
        try {
            long count = invoiceRepository.countByTypeAndYear(type, year);
            String candidate = String.format("%s-%s-%03d", prefix, year, count + 1);
            // Garantir l'unicité
            int attempt = 0;
            while (invoiceRepository.existsByInvoiceNumber(candidate) && attempt < 100) {
                count++;
                attempt++;
                candidate = String.format("%s-%s-%03d", prefix, year, count + 1);
            }
            return candidate;
        } catch (Exception e) {
            log.warn("[INVOICE] generateNextNumber fallback timestamp : {}", e.getMessage());
            return String.format("%s-%s-%d", prefix, year, System.currentTimeMillis() % 100000);
        }
    }

    @GetMapping("/all")
    public List<Invoice> getAll() {
        return invoiceService.retrieveAllInvoices();
    }

    @GetMapping("/{id}")
    public Invoice getById(@PathVariable Long id) {
        return invoiceService.retrieveInvoice(id);
    }

    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestBody Invoice invoice) {
        try {
            // Recalcul TTC
            if (invoice.getAmountHT() != null && invoice.getTva() != null) {
                double ttc = invoice.getAmountHT() * (1.0 + invoice.getTva() / 100.0);
                invoice.setAmountTTC(Math.round(ttc * 1000.0) / 1000.0);
            }
            Invoice saved = invoiceRepository.save(invoice);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("[INVOICE] ❌ Erreur update #{} : {}", invoice.getId(), e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Erreur mise à jour", "detail", e.getMessage()));
        }
    }

    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
    }

    @PostMapping("/mark-paid/{id}")
    public Invoice markPaid(@PathVariable Long id) {
        return invoiceService.markAsPaid(id);
    }

    // ═══════════════════════════════════════════════════════════
    //  FILTRES PAR ENTREPRISE CONNECTEE
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/my")
    public ResponseEntity<List<Invoice>> getMyInvoices() {
        String companyName = getCurrentCompanyName();
        if (companyName == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(invoiceRepository.findByEnterpriseCompanyName(companyName));
    }

    @GetMapping("/my/sales")
    public ResponseEntity<List<Invoice>> getMySalesInvoices() {
        String companyName = getCurrentCompanyName();
        if (companyName == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(invoiceRepository.findSalesInvoices(companyName));
    }

    @GetMapping("/my/purchases")
    public ResponseEntity<List<Invoice>> getMyPurchaseInvoices() {
        String companyName = getCurrentCompanyName();
        if (companyName == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(invoiceRepository.findPurchaseInvoices(companyName));
    }

    // ═══════════════════════════════════════════════════════════
    //  IA
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/ai-risk")
    public InvoiceRiskService.RiskReport getRiskReport() {
        return invoiceRiskService.generateRiskReport();
    }

    @GetMapping("/ai-solvability")
    public ClientSolvabilityService.SolvabilityReport getSolvabilityReport() {
        return solvabilityService.generateSolvabilityReport();
    }

    @PostMapping("/chat")
    public InvoiceChatService.ChatResponse chat(@RequestBody InvoiceChatService.ChatRequest request) {
        return chatService.chat(request.question());
    }

    // ═══════════════════════════════════════════════════════════
    //  UTILITAIRE
    // ═══════════════════════════════════════════════════════════

    private String getCurrentCompanyName() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return null;
            String email = auth.getName();
            User user = userRepository.findByEmailWithProfiles(email).orElse(null);
            if (user == null) return null;
            Enterprise enterprise = enterpriseRepository.findByUserId(user.getId()).orElse(null);
            if (enterprise == null) return null;
            return enterprise.getCompanyName();
        } catch (Exception e) {
            log.error("[INVOICES] Erreur récupération entreprise : {}", e.getMessage());
            return null;
        }
    }
}