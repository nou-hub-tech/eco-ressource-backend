package com.marketplace.backend.service.finance;

import com.marketplace.backend.entity.finance.Invoice;
import com.marketplace.backend.repository.finance.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 🤖 Service de Détection des Risques — Factures & Clients
 *
 * Calcule pour chaque facture et chaque client :
 *  - Score de risque (0–100) : plus le score est élevé, plus le risque est élevé
 *  - Niveau de risque : CRITIQUE / ÉLEVÉ / MOYEN / FAIBLE
 *  - Recommandations d'action personnalisées
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceRiskService {

    private final InvoiceRepository invoiceRepository;
    private final EnterpriseContextHelper enterpriseContext;

    // ══════════════════════════════════════════════
    //  POINT D'ENTRÉE
    // ══════════════════════════════════════════════
    public RiskReport generateRiskReport() {
        // 🏢 Filtrer par entreprise connectee
        String companyName = enterpriseContext.getCurrentCompanyName();
        List<Invoice> all;
        if (companyName != null) {
            all = invoiceRepository.findByEnterpriseCompanyName(companyName);
            log.info("[RISK] Rapport pour '{}' — {} factures", companyName, all.size());
        } else {
            all = invoiceRepository.findAll();
        }

        List<InvoiceRisk>  invoiceRisks = analyzeInvoices(all);
        List<ClientRisk>   clientRisks  = analyzeClients(all);
        RiskSummary        summary      = computeSummary(all, invoiceRisks);
        List<String>       recommendations = generateRecommendations(summary, clientRisks);

        return new RiskReport(invoiceRisks, clientRisks, summary, recommendations);
    }

    // ══════════════════════════════════════════════
    //  1. RISQUE PAR FACTURE
    // ══════════════════════════════════════════════
    private List<InvoiceRisk> analyzeInvoices(List<Invoice> invoices) {
        return invoices.stream()
                .filter(i -> "UNPAID".equals(i.getStatus()))
                .map(i -> {
                    long daysOverdue = computeDaysOverdue(i);
                    double riskScore  = computeInvoiceRiskScore(i, daysOverdue);
                    String level      = riskLevel(riskScore);
                    String action     = recommendAction(i, daysOverdue);

                    return new InvoiceRisk(
                            i.getId(),
                            i.getInvoiceNumber(),
                            i.getClientName(),
                            i.getProject(),
                            i.getAmountTTC() != null ? i.getAmountTTC() : 0.0,
                            i.getIssueDate(),
                            daysOverdue,
                            Math.round(riskScore * 10.0) / 10.0,
                            level,
                            action
                    );
                })
                .sorted(Comparator.comparingDouble(InvoiceRisk::riskScore).reversed())
                .collect(Collectors.toList());
    }

    private long computeDaysOverdue(Invoice i) {
        if (i.getIssueDate() == null) return 0;
        try {
            LocalDate issued = LocalDate.parse(i.getIssueDate());
            long days = ChronoUnit.DAYS.between(issued, LocalDate.now());
            return Math.max(0, days);
        } catch (Exception e) { return 0; }
    }

    private double computeInvoiceRiskScore(Invoice i, long daysOverdue) {
        double score = 0;

        // Critère 1 — Ancienneté (50 pts max)
        if (daysOverdue > 90)       score += 50;
        else if (daysOverdue > 60)  score += 38;
        else if (daysOverdue > 30)  score += 22;
        else if (daysOverdue > 15)  score += 10;
        else                        score += 2;

        // Critère 2 — Montant (30 pts max)
        double amount = i.getAmountTTC() != null ? i.getAmountTTC() : 0;
        if (amount > 100_000)      score += 30;
        else if (amount > 50_000)  score += 22;
        else if (amount > 20_000)  score += 14;
        else if (amount > 5_000)   score += 7;
        else                       score += 2;

        // Critère 3 — Absence de lien livraison (20 pts max)
        if (i.getDeliveryOrderId() == null) score += 10;  // pas de suivi associé
        if (i.getLinkedEscrowId()  == null) score += 10;  // pas d'escrow de garantie

        return Math.min(100, score);
    }

    private String recommendAction(Invoice i, long daysOverdue) {
        if (daysOverdue > 90)
            return "🆘 Action urgente — Transmettre au service juridique / contentieux";
        if (daysOverdue > 60)
            return "📞 Appel téléphonique + mise en demeure formelle";
        if (daysOverdue > 30)
            return "📧 Relance email prioritaire avec échéancier de paiement";
        if (daysOverdue > 15)
            return "🔔 Relance préventive — Rappeler la date d'échéance";
        return "👁️ Surveiller — Facture récente, pas encore échue";
    }

    // ══════════════════════════════════════════════
    //  2. RISQUE PAR CLIENT
    // ══════════════════════════════════════════════
    private List<ClientRisk> analyzeClients(List<Invoice> invoices) {
        Map<String, List<Invoice>> byClient = invoices.stream()
                .filter(i -> i.getClientName() != null)
                .collect(Collectors.groupingBy(Invoice::getClientName));

        return byClient.entrySet().stream().map(entry -> {
            String client = entry.getKey();
            List<Invoice> clientInvoices = entry.getValue();

            long total   = clientInvoices.size();
            long unpaid  = clientInvoices.stream().filter(i -> "UNPAID".equals(i.getStatus())).count();
            long overdue = clientInvoices.stream()
                    .filter(i -> "UNPAID".equals(i.getStatus()) && computeDaysOverdue(i) > 30).count();

            double totalAmount  = clientInvoices.stream()
                    .mapToDouble(i -> i.getAmountTTC() != null ? i.getAmountTTC() : 0).sum();
            double unpaidAmount = clientInvoices.stream()
                    .filter(i -> "UNPAID".equals(i.getStatus()))
                    .mapToDouble(i -> i.getAmountTTC() != null ? i.getAmountTTC() : 0).sum();

            double unpaidRate  = total == 0 ? 0 : (double) unpaid / total * 100;
            double clientScore = computeClientRiskScore(total, unpaid, overdue, unpaidAmount);
            String level       = riskLevel(clientScore);

            return new ClientRisk(
                    client,
                    (int) total,
                    (int) unpaid,
                    (int) overdue,
                    Math.round(totalAmount  * 100.0) / 100.0,
                    Math.round(unpaidAmount * 100.0) / 100.0,
                    Math.round(unpaidRate   * 10.0)  / 10.0,
                    Math.round(clientScore  * 10.0)  / 10.0,
                    level
            );
        })
        .filter(c -> c.totalInvoices() > 0)
        .sorted(Comparator.comparingDouble(ClientRisk::riskScore).reversed())
        .collect(Collectors.toList());
    }

    private double computeClientRiskScore(long total, long unpaid, long overdue, double unpaidAmount) {
        double score = 0;

        // Taux d'impayé (40 pts)
        double unpaidRate = total == 0 ? 0 : (double) unpaid / total;
        score += Math.min(40, unpaidRate * 40);

        // Factures en retard > 30j (30 pts)
        double overdueRate = total == 0 ? 0 : (double) overdue / total;
        score += Math.min(30, overdueRate * 50);

        // Montant impayé absolu (30 pts)
        if (unpaidAmount > 100_000)      score += 30;
        else if (unpaidAmount > 50_000)  score += 22;
        else if (unpaidAmount > 20_000)  score += 14;
        else if (unpaidAmount > 5_000)   score += 7;
        else                             score += 2;

        return Math.min(100, score);
    }

    // ══════════════════════════════════════════════
    //  3. RÉSUMÉ GLOBAL
    // ══════════════════════════════════════════════
    private RiskSummary computeSummary(List<Invoice> all, List<InvoiceRisk> risks) {
        long total   = all.size();
        long paid    = all.stream().filter(i -> "PAID".equals(i.getStatus())).count();
        long unpaid  = all.stream().filter(i -> "UNPAID".equals(i.getStatus())).count();
        long overdue = all.stream()
                .filter(i -> "UNPAID".equals(i.getStatus()) && computeDaysOverdue(i) > 30).count();

        double unpaidAmount = all.stream()
                .filter(i -> "UNPAID".equals(i.getStatus()))
                .mapToDouble(i -> i.getAmountTTC() != null ? i.getAmountTTC() : 0).sum();

        long critique = risks.stream().filter(r -> "CRITIQUE".equals(r.riskLevel())).count();
        long eleve    = risks.stream().filter(r -> "ÉLEVÉ".equals(r.riskLevel())).count();
        long moyen    = risks.stream().filter(r -> "MOYEN".equals(r.riskLevel())).count();
        long faible   = risks.stream().filter(r -> "FAIBLE".equals(r.riskLevel())).count();

        double globalRiskScore = risks.isEmpty() ? 0 :
                risks.stream().mapToDouble(InvoiceRisk::riskScore).average().orElse(0);

        return new RiskSummary(
                (int) total, (int) paid, (int) unpaid, (int) overdue,
                Math.round(unpaidAmount * 100.0) / 100.0,
                (int) critique, (int) eleve, (int) moyen, (int) faible,
                Math.round(globalRiskScore * 10.0) / 10.0,
                riskLevel(globalRiskScore)
        );
    }

    // ══════════════════════════════════════════════
    //  4. RECOMMANDATIONS IA
    // ══════════════════════════════════════════════
    private List<String> generateRecommendations(RiskSummary s, List<ClientRisk> clients) {
        List<String> recs = new ArrayList<>();

        if (s.criticalCount() > 0)
            recs.add("🆘 " + s.criticalCount() + " facture(s) en risque CRITIQUE — Action juridique recommandée immédiatement.");

        if (s.overdueCount() > 0)
            recs.add("🔴 " + s.overdueCount() + " facture(s) impayée(s) depuis plus de 30 jours — Lancez une campagne de relance urgente.");

        if (s.unpaidAmount() > 50_000)
            recs.add("💸 " + String.format("%,.0f", s.unpaidAmount()).replace(",", " ") + " TND d'impayés en attente — Envisagez un affacturage pour améliorer votre trésorerie.");

        // Clients les plus risqués
        clients.stream()
               .filter(c -> "CRITIQUE".equals(c.riskLevel()) || "ÉLEVÉ".equals(c.riskLevel()))
               .limit(3)
               .forEach(c -> recs.add("⚠️ Client à risque " + c.riskLevel() + " : " + c.clientName() +
                       " — " + c.unpaidInvoices() + " facture(s) impayée(s), " +
                       String.format("%,.0f", c.unpaidAmount()).replace(",", " ") + " TND en attente."));

        if (s.globalRiskScore() < 20)
            recs.add("✅ Risque global faible — Votre portefeuille de factures est bien géré. Continuez la surveillance régulière.");
        else if (s.globalRiskScore() > 60)
            recs.add("📊 Mettez en place des conditions de paiement plus strictes (acomptes, délais réduits) pour les nouveaux clients.");

        if (recs.isEmpty())
            recs.add("✅ Aucun risque critique détecté — Portefeuille de factures en bonne santé.");

        return recs;
    }

    // ══════════════════════════════════════════════
    //  UTILITAIRES
    // ══════════════════════════════════════════════
    private String riskLevel(double score) {
        if (score >= 70) return "CRITIQUE";
        if (score >= 45) return "ÉLEVÉ";
        if (score >= 20) return "MOYEN";
        return "FAIBLE";
    }

    // ══════════════════════════════════════════════
    //  TYPES DE RETOUR
    // ══════════════════════════════════════════════
    public record InvoiceRisk(
            Long   invoiceId,
            String invoiceNumber,
            String clientName,
            String project,
            double amount,
            String issueDate,
            long   daysOverdue,
            double riskScore,
            String riskLevel,
            String recommendedAction
    ) {}

    public record ClientRisk(
            String clientName,
            int    totalInvoices,
            int    unpaidInvoices,
            int    overdueInvoices,
            double totalAmount,
            double unpaidAmount,
            double unpaidRate,
            double riskScore,
            String riskLevel
    ) {}

    public record RiskSummary(
            int    totalInvoices,
            int    paidInvoices,
            int    unpaidInvoices,
            int    overdueCount,
            double unpaidAmount,
            int    criticalCount,
            int    elevatedCount,
            int    mediumCount,
            int    lowCount,
            double globalRiskScore,
            String globalRiskLevel
    ) {}

    public record RiskReport(
            List<InvoiceRisk>  invoiceRisks,
            List<ClientRisk>   clientRisks,
            RiskSummary        summary,
            List<String>       recommendations
    ) {}
}
