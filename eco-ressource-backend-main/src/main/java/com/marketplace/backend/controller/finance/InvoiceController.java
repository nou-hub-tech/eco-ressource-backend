package com.marketplace.backend.controller.finance;

import com.marketplace.backend.entity.finance.Invoice;
import com.marketplace.backend.service.finance.ClientSolvabilityService;
import com.marketplace.backend.service.finance.IInvoiceService;
import com.marketplace.backend.service.finance.InvoiceChatService;
import com.marketplace.backend.service.finance.InvoiceRiskService;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class InvoiceController {

    private final IInvoiceService invoiceService;
    private final InvoiceRiskService invoiceRiskService;
    private final ClientSolvabilityService solvabilityService;
    private final InvoiceChatService chatService;

    @PostMapping("/add")
    public Invoice addInvoice(@RequestBody Invoice invoice) {
        return invoiceService.addInvoice(invoice);
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

    /**
     * 💬 Chatbot IA Financier — Powered by Groq/Llama3
     * POST /api/invoices/chat
     * Body: { "question": "Qui est mon client le plus risqué ?" }
     */
    @PostMapping("/chat")
    public InvoiceChatService.ChatResponse chat(@RequestBody InvoiceChatService.ChatRequest request) {
        return chatService.chat(request.question());
    }
}