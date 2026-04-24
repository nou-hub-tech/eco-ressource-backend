package com.marketplace.backend.service.finance;

import com.marketplace.backend.entity.finance.Invoice;
import com.marketplace.backend.repository.finance.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ══════════════════════════════════════════════════════════════
 *  💬 CHATBOT IA FINANCIER — Powered by Groq / Llama3
 * ══════════════════════════════════════════════════════════════
 *
 * Répond aux questions des utilisateurs sur leurs factures
 * en utilisant les vraies données de la base + Groq LLM.
 *
 * Exemple de questions :
 *  - "Qui est mon client le plus risqué ?"
 *  - "Combien j'ai de factures impayées ?"
 *  - "Quel est mon taux de recouvrement ?"
 *  - "Donne-moi un résumé de ma situation financière"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceChatService {

    private final InvoiceRepository invoiceRepository;
    private final RestTemplate restTemplate;

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url}")
    private String groqApiUrl;

    @Value("${groq.model}")
    private String groqModel;

    // ═══════════════════════════════════════════════════════════
    //  POINT D'ENTRÉE — Répondre à une question
    // ═══════════════════════════════════════════════════════════
    public ChatResponse chat(String userQuestion) {
        if (userQuestion == null || userQuestion.isBlank()) {
            return new ChatResponse("Veuillez poser une question.", false);
        }

        // 1. Construire le contexte financier depuis la DB
        String financialContext = buildFinancialContext();

        // 2. Appeler Groq
        try {
            String answer = callGroq(userQuestion, financialContext);
            return new ChatResponse(answer, true);
        } catch (Exception e) {
            log.error("[CHAT] Erreur appel Groq : {}", e.getMessage());
            // Fallback : réponse locale si Groq indisponible
            return new ChatResponse(localFallback(userQuestion), false);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  CONSTRUCTION DU CONTEXTE FINANCIER
    // ═══════════════════════════════════════════════════════════
    private String buildFinancialContext() {
        List<Invoice> all = invoiceRepository.findAll();

        if (all.isEmpty()) return "Aucune facture dans le système.";

        long total   = all.size();
        long paid    = all.stream().filter(i -> "PAID".equals(i.getStatus())).count();
        long unpaid  = total - paid;
        double totalAmount  = all.stream().mapToDouble(i -> safe(i.getAmountTTC())).sum();
        double paidAmount   = all.stream().filter(i -> "PAID".equals(i.getStatus())).mapToDouble(i -> safe(i.getAmountTTC())).sum();
        double unpaidAmount = totalAmount - paidAmount;
        double recoveryRate = totalAmount > 0 ? paidAmount / totalAmount * 100 : 0;

        // Factures en retard
        long overdue = all.stream()
                .filter(i -> "UNPAID".equals(i.getStatus()) && daysOverdue(i) > 30)
                .count();

        // Résumé par client
        Map<String, List<Invoice>> byClient = all.stream()
                .filter(i -> i.getClientName() != null)
                .collect(Collectors.groupingBy(Invoice::getClientName));

        StringBuilder clientSummary = new StringBuilder();
        byClient.forEach((client, invoices) -> {
            long clientUnpaid  = invoices.stream().filter(i -> "UNPAID".equals(i.getStatus())).count();
            double clientUnpaidAmt = invoices.stream().filter(i -> "UNPAID".equals(i.getStatus())).mapToDouble(i -> safe(i.getAmountTTC())).sum();
            long maxDays = invoices.stream().filter(i -> "UNPAID".equals(i.getStatus())).mapToLong(this::daysOverdue).max().orElse(0);
            clientSummary.append(String.format(
                    "- %s : %d factures (%d impayees, %.0f TND impayес, max %d jours de retard)\n",
                    client, invoices.size(), clientUnpaid, clientUnpaidAmt, maxDays));
        });

        // Liste des 5 factures impayées les plus anciennes
        String oldestUnpaid = all.stream()
                .filter(i -> "UNPAID".equals(i.getStatus()))
                .sorted(Comparator.comparingLong(this::daysOverdue).reversed())
                .limit(5)
                .map(i -> String.format("  * Facture %s — Client: %s — %.0f TND — %d jours",
                        i.getInvoiceNumber(), i.getClientName(), safe(i.getAmountTTC()), daysOverdue(i)))
                .collect(Collectors.joining("\n"));

        return String.format("""
                === DONNÉES FINANCIÈRES RÉELLES (EcoRessource B2B) ===
                Date d'analyse : %s
                
                RÉSUMÉ GLOBAL :
                - Total factures : %d
                - Factures payées : %d (%.1f%%)
                - Factures impayées : %d
                - Montant total facturé : %.0f TND
                - Montant encaissé : %.0f TND  
                - Montant impayé : %.0f TND
                - Taux de recouvrement : %.1f%%
                - Factures en retard >30j : %d
                
                RÉSUMÉ PAR CLIENT :
                %s
                
                FACTURES IMPAYÉES LES PLUS ANCIENNES :
                %s
                """,
                LocalDate.now(),
                total, paid, (double) paid / total * 100, unpaid,
                totalAmount, paidAmount, unpaidAmount, recoveryRate, overdue,
                clientSummary,
                oldestUnpaid.isEmpty() ? "Aucune facture impayée" : oldestUnpaid
        );
    }

    // ═══════════════════════════════════════════════════════════
    //  APPEL GROQ API
    // ═══════════════════════════════════════════════════════════
    @SuppressWarnings("unchecked")
    private String callGroq(String question, String context) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> systemMsg = new LinkedHashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content",
                "Tu es un assistant financier expert pour une plateforme B2B de ressources recyclées. " +
                "Tu analyses les données financières réelles fournies et réponds en français de manière " +
                "professionnelle, concise et actionnable. " +
                "Tu utilises UNIQUEMENT les données fournies dans le contexte. " +
                "Si une information n'est pas dans les données, dis-le clairement. " +
                "Tes réponses sont structurées avec des puces ou paragraphes courts.\n\n" +
                "DONNÉES DISPONIBLES :\n" + context);

        Map<String, Object> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", question);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", groqModel);
        body.put("messages", List.of(systemMsg, userMsg));
        body.put("max_tokens", 512);
        body.put("temperature", 0.3);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                groqApiUrl, HttpMethod.POST, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
        }
        throw new RuntimeException("Réponse Groq invalide");
    }

    // ═══════════════════════════════════════════════════════════
    //  FALLBACK LOCAL (si Groq indisponible)
    // ═══════════════════════════════════════════════════════════
    private String localFallback(String question) {
        List<Invoice> all = invoiceRepository.findAll();
        String q = question.toLowerCase();

        if (q.contains("impay") || q.contains("unpaid")) {
            long count = all.stream().filter(i -> "UNPAID".equals(i.getStatus())).count();
            double amount = all.stream().filter(i -> "UNPAID".equals(i.getStatus())).mapToDouble(i -> safe(i.getAmountTTC())).sum();
            return String.format("Vous avez **%d factures impayées** pour un total de **%.0f TND**.", count, amount);
        }
        if (q.contains("taux") || q.contains("recouvrement")) {
            long paid   = all.stream().filter(i -> "PAID".equals(i.getStatus())).count();
            long total  = all.size();
            return String.format("Votre taux de recouvrement est de **%.1f%%** (%d/%d factures payées).",
                    total > 0 ? (double) paid / total * 100 : 0, paid, total);
        }
        if (q.contains("client") || q.contains("risque")) {
            return "Consultez l'onglet **Profils de Solvabilité** pour voir les ratings clients (AAA→CCC).";
        }
        return "⚠️ L'assistant IA est temporairement indisponible. Vérifiez votre clé Groq dans application.properties.";
    }

    // ─────────────────────────────────────────────────────────
    private long daysOverdue(Invoice i) {
        if (i.getIssueDate() == null) return 0;
        try { return Math.max(0, ChronoUnit.DAYS.between(LocalDate.parse(i.getIssueDate()), LocalDate.now())); }
        catch (Exception e) { return 0; }
    }
    private double safe(Double v) { return v != null ? v : 0; }

    // ─────────────────────────────────────────────────────────
    public record ChatResponse(String message, boolean fromAi) {}
    public record ChatRequest(String question) {}
}
