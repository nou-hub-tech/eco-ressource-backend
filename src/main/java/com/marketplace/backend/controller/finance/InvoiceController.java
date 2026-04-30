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
@RequiredArgsConstructor
public class InvoiceController {

    private final IInvoiceService invoiceService;
    private final InvoiceRiskService invoiceRiskService;
    private final ClientSolvabilityService solvabilityService;
    private final InvoiceChatService chatService;

    // Repos pour le filtrage par entreprise
    private final UserRepository userRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final InvoiceRepository invoiceRepository;

    // ═══════════════════════════════════════════════════════════
    //  CRUD standard
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/add")
    public Invoice addInvoice(@RequestBody Invoice invoice) {
        String companyName = getCurrentCompanyName();
        
        // Auto-detection du type si non fourni
        if (invoice.getInvoiceType() == null && companyName != null) {
            if (companyName.equalsIgnoreCase(invoice.getSellerName())) {
                invoice.setInvoiceType(InvoiceType.VENTE);
            } else if (companyName.equalsIgnoreCase(invoice.getClientName())) {
                invoice.setInvoiceType(InvoiceType.ACHAT);
            }
        }

        // Forcer l'association à l'entreprise connectée pour garantir la visibilité
        if (companyName != null) {
            if (invoice.getInvoiceType() == InvoiceType.VENTE) {
                invoice.setSellerName(companyName);
            } else if (invoice.getInvoiceType() == InvoiceType.ACHAT) {
                invoice.setClientName(companyName);
            }
        }

        // Auto-génération du numéro si vide
        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isBlank()) {
            invoice.setInvoiceNumber(generateNextNumber(invoice.getInvoiceType()));
        }

        return invoiceService.addInvoice(invoice);
    }

    /**
     * 🔢 Prochain numéro de facture disponible : VTE-2026-001 ou ACH-2026-001
     * GET /api/invoices/next-number/{type}
     */
    @GetMapping("/next-number/{type}")
    public ResponseEntity<Map<String, String>> getNextNumber(@PathVariable String type) {
        InvoiceType invoiceType;
        try { invoiceType = InvoiceType.valueOf(type.toUpperCase()); }
        catch (Exception e) { return ResponseEntity.badRequest().build(); }
        return ResponseEntity.ok(Map.of("number", generateNextNumber(invoiceType)));
    }

    private String generateNextNumber(InvoiceType type) {
        if (type == null) return "";
        String year  = String.valueOf(java.time.LocalDate.now().getYear());
        String prefix = type == InvoiceType.VENTE ? "VTE" : "ACH";
        long count = invoiceRepository.countByTypeAndYear(type, year);
        return String.format("%s-%s-%03d", prefix, year, count + 1);
    }

    /** Toutes les factures (admin uniquement) */
    @GetMapping("/all")
    public List<Invoice> getAll() {
        return invoiceService.retrieveAllInvoices();
    }

    @GetMapping("/{id}")
    public Invoice getById(@PathVariable Long id) {
        return invoiceService.retrieveInvoice(id);
    }

    @PutMapping("/update")
    public Invoice update(@RequestBody Invoice invoice) {
        return invoiceService.updateInvoice(invoice);
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
    //  🏢 FILTRES PAR ENTREPRISE CONNECTEE
    // ═══════════════════════════════════════════════════════════

    /** Toutes les factures de l'entreprise (acheteur OU vendeur) */
    @GetMapping("/my")
    public ResponseEntity<List<Invoice>> getMyInvoices() {
        String companyName = getCurrentCompanyName();
        if (companyName == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(invoiceRepository.findByEnterpriseCompanyName(companyName));
    }

    /** 📤 Factures de VENTE : l'entreprise est vendeur → va encaisser */
    @GetMapping("/my/sales")
    public ResponseEntity<List<Invoice>> getMySalesInvoices() {
        String companyName = getCurrentCompanyName();
        if (companyName == null) return ResponseEntity.ok(List.of());
        List<Invoice> sales = invoiceRepository.findSalesInvoices(companyName);
        log.info("[INVOICES] {} factures de vente pour '{}'", sales.size(), companyName);
        return ResponseEntity.ok(sales);
    }

    /** 📥 Factures d'ACHAT : l'entreprise est acheteur → doit payer */
    @GetMapping("/my/purchases")
    public ResponseEntity<List<Invoice>> getMyPurchaseInvoices() {
        String companyName = getCurrentCompanyName();
        if (companyName == null) return ResponseEntity.ok(List.of());
        List<Invoice> purchases = invoiceRepository.findPurchaseInvoices(companyName);
        log.info("[INVOICES] {} factures d'achat pour '{}'", purchases.size(), companyName);
        return ResponseEntity.ok(purchases);
    }

    // ═══════════════════════════════════════════════════════════
    //  IA
    // ═══════════════════════════════════════════════════════════

    /** 🤖 Analyse IA risque simple — GET /api/invoices/ai-risk */
    @GetMapping("/ai-risk")
    public InvoiceRiskService.RiskReport getRiskReport() {
        return invoiceRiskService.generateRiskReport();
    }

    /** 🏦 Solvabilité avancée — GET /api/invoices/ai-solvability */
    @GetMapping("/ai-solvability")
    public ClientSolvabilityService.SolvabilityReport getSolvabilityReport() {
        return solvabilityService.generateSolvabilityReport();
    }

    /** 💬 Chatbot IA Financier — POST /api/invoices/chat */
    @PostMapping("/chat")
    public InvoiceChatService.ChatResponse chat(@RequestBody InvoiceChatService.ChatRequest request) {
        return chatService.chat(request.question());
    }

    // ═══════════════════════════════════════════════════════════
    //  Méthode utilitaire : companyName de l'utilisateur connecté
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