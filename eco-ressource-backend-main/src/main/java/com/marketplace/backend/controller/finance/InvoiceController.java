package com.marketplace.backend.controller.finance;

import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.finance.Invoice;
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

    // Repos pour le filtrage par entreprise
    private final UserRepository userRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final InvoiceRepository invoiceRepository;

    // ═══════════════════════════════════════════════════════════
    //  CRUD standard
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/add")
    public Invoice addInvoice(@RequestBody Invoice invoice) {
        return invoiceService.addInvoice(invoice);
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
    //  🏢 FILTRÉ PAR ENTREPRISE CONNECTÉE
    //  GET /api/invoices/my  →  acheteur OU vendeur
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/my")
    public ResponseEntity<List<Invoice>> getMyInvoices() {
        String companyName = getCurrentCompanyName();
        if (companyName == null) {
            log.warn("[INVOICES] Entreprise introuvable pour l'utilisateur connecté");
            return ResponseEntity.ok(List.of());
        }
        List<Invoice> invoices = invoiceRepository.findByEnterpriseCompanyName(companyName);
        log.info("[INVOICES] {} factures trouvées pour l'entreprise '{}'", invoices.size(), companyName);
        return ResponseEntity.ok(invoices);
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