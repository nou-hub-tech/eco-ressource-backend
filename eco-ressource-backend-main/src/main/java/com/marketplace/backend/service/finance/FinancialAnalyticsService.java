package com.marketplace.backend.service.finance;

import com.marketplace.backend.entity.finance.Invoice;
import com.marketplace.backend.entity.finance.transaction;
import com.marketplace.backend.repository.finance.InvoiceRepository;
import com.marketplace.backend.repository.finance.TransactionRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 🤖 Moteur d'Analyse Financière Intelligente
 *
 * Calcule :
 *  - Score de santé financière (0–100) pondéré sur 5 KPIs
 *  - Prédiction de trésorerie sur 3 mois (moyenne mobile pondérée)
 *  - Détection d'anomalies (Z-Score sur montants)
 *  - Recommandations métier automatiques
 */
@Service
@RequiredArgsConstructor
public class FinancialAnalyticsService {

    private final InvoiceRepository invoiceRepo;
    private final TransactionRepository txRepo;


    // ══════════════════════════════════════════════════════════════════════
    //  POINT D'ENTRÉE PRINCIPAL
    // ══════════════════════════════════════════════════════════════════════
    public FinancialHealthReport generateReport() {
        List<Invoice> invoices = invoiceRepo.findAll();
        List<transaction> transactions = txRepo.findAll();


        FinancialHealthReport report = new FinancialHealthReport();


        report.setHealthScore(computeHealthScore(invoices, transactions));
        report.setHealthLevel(scoreToLevel(report.getHealthScore()));


        report.setCashFlowPredictions(predictCashFlow(transactions));


        report.setAnomalies(detectAnomalies(transactions));

        // 4⃣ KPIs calculés
        report.setKpis(computeKpis(invoices, transactions));

        // 5 Recommandations
        report.setRecommendations(generateRecommendations(report));

        return report;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  1. SCORE SANTÉ FINANCIÈRE (0–100)
    //     Pondération : Recouvrement 35% | Liquidité 25% | Vitesse 20%
    //                   Diversification 10% | Régularité 10%
    // ══════════════════════════════════════════════════════════════════════
    private double computeHealthScore(List<Invoice> invoices, List<transaction> txs) {
        if (invoices.isEmpty()) return 0;

        // KPI 1 — Taux de recouvrement (35%)
        long paid    = invoices.stream().filter(i -> "PAID".equals(i.getStatus())).count();
        long overdue = invoices.stream().filter(this::isOverdue).count();
        double recoveryRate = invoices.isEmpty() ? 0 : (double) paid / invoices.size() * 100;
        double overdueRatio = invoices.isEmpty() ? 0 : (double) overdue / invoices.size() * 100;
        double kpi1 = Math.max(0, recoveryRate - overdueRatio * 0.5);

        // KPI 2 — Ratio liquidité : revenus vs dépenses (25%)
        double totalIn  = txs.stream().filter(t -> t.getAmount() > 0).mapToDouble(t -> t.getAmount()).sum();
        double totalOut = txs.stream().filter(t -> t.getAmount() < 0).mapToDouble(t -> Math.abs(t.getAmount())).sum();
        double liquidityRatio = totalOut == 0 ? 100 : Math.min(100, (totalIn / (totalIn + totalOut)) * 100);
        double kpi2 = liquidityRatio;

        // KPI 3 — Vitesse d'encaissement : % factures payées dans les 30j (20%)
        double kpi3 = invoices.stream()
                .filter(i -> "PAID".equals(i.getStatus()) && i.getIssueDate() != null && i.getDeliveredAt() != null)
                .mapToDouble(i -> {
                    try {
                        LocalDate issued    = LocalDate.parse(i.getIssueDate());
                        LocalDate delivered = LocalDate.parse(i.getDeliveredAt());
                        long days = java.time.temporal.ChronoUnit.DAYS.between(issued, delivered);
                        return days <= 30 ? 100 : Math.max(0, 100 - (days - 30) * 2L);
                    } catch (Exception e) { return 50; }
                })
                .average().orElse(50);

        // KPI 4 — Diversification clients : Herfindahl index inversé (10%)
        Map<String, Double> clientRevenue = invoices.stream()
                .filter(i -> "PAID".equals(i.getStatus()) && i.getClientName() != null)
                .collect(Collectors.groupingBy(Invoice::getClientName,
                        Collectors.summingDouble(inv -> inv.getAmountTTC() != null ? inv.getAmountTTC() : 0)));
        double totalRevenue = clientRevenue.values().stream().mapToDouble(Double::doubleValue).sum();
        double hhi = totalRevenue == 0 ? 1 :
                clientRevenue.values().stream()
                        .mapToDouble(v -> Math.pow(v / totalRevenue, 2))
                        .sum();
        double kpi4 = Math.max(0, (1 - hhi) * 100); // 0 = concentré, 100 = diversifié

        // KPI 5 — Régularité des paiements entrants (10%)
        double kpi5 = txs.size() < 3 ? 50 : computeRegularityScore(txs);

        // Score final pondéré
        double score = kpi1 * 0.35 + kpi2 * 0.25 + kpi3 * 0.20 + kpi4 * 0.10 + kpi5 * 0.10;
        return Math.min(100, Math.max(0, Math.round(score * 10.0) / 10.0));
    }

    private double computeRegularityScore(List<transaction> txs) {
        // Écart-type des montants entrants → plus c'est stable, meilleur le score
        List<Double> incoming = txs.stream()
                .filter(t -> t.getAmount() > 0)
                .map(transaction::getAmount)
                .collect(Collectors.toList());
        if (incoming.size() < 2) return 50;
        double mean = incoming.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = incoming.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0);
        double stddev = Math.sqrt(variance);
        double cv = mean == 0 ? 1 : stddev / mean; // Coefficient de variation
        return Math.max(0, Math.min(100, (1 - cv) * 100));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  2. PRÉDICTION TRÉSORERIE — Moyenne Mobile Pondérée (3 mois)
    // ══════════════════════════════════════════════════════════════════════
    private List<CashFlowPrediction> predictCashFlow(List<transaction> txs) {
        // Regrouper les transactions par mois
        Map<String, Double> monthlyFlow = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");

        // Calculer le flux net mensuel réel
        txs.stream()
                .filter(t -> t.getDate() != null)
                .forEach(t -> {
                    try {
                        String month = LocalDate.parse(t.getDate()).format(fmt);
                        monthlyFlow.merge(month, t.getAmount(), Double::sum);
                    } catch (Exception ignored) {}
                });

        List<Double> historicalValues = new ArrayList<>(monthlyFlow.values());
        int n = historicalValues.size();

        // Moyenne mobile pondérée (poids croissants vers le plus récent)
        List<CashFlowPrediction> predictions = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (int i = 1; i <= 3; i++) {
            LocalDate future = now.plusMonths(i);
            String monthLabel = future.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH));

            double predicted;
            if (n == 0) {
                predicted = 0;
            } else if (n == 1) {
                predicted = historicalValues.get(0);
            } else {
                // Moyenne mobile pondérée : dernier mois × 3, avant-dernier × 2, etc.
                int window = Math.min(n, 3);
                double weights = 0, weightedSum = 0;
                for (int j = 0; j < window; j++) {
                    double weight = window - j;
                    weightedSum += historicalValues.get(n - 1 - j) * weight;
                    weights += weight;
                }
                predicted = weightedSum / weights;
                // Tendance légère (drift)
                if (n >= 2) {
                    double trend = (historicalValues.get(n - 1) - historicalValues.get(0)) / n;
                    predicted += trend * i * 0.5;
                }
            }

            predictions.add(new CashFlowPrediction(monthLabel, Math.round(predicted * 100.0) / 100.0, i));
        }

        return predictions;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  3. DÉTECTION D'ANOMALIES — Z-Score
    //     Une transaction est "anormale" si |Z| > 2.0
    // ══════════════════════════════════════════════════════════════════════
    private List<AnomalyAlert> detectAnomalies(List<transaction> txs) {
        if (txs.size() < 3) return Collections.emptyList();

        double[] amounts = txs.stream().mapToDouble(transaction::getAmount).toArray();
        double mean   = Arrays.stream(amounts).average().orElse(0);
        double stddev = Math.sqrt(Arrays.stream(amounts).map(a -> Math.pow(a - mean, 2)).average().orElse(0));

        if (stddev == 0) return Collections.emptyList();

        List<AnomalyAlert> alerts = new ArrayList<>();
        for (int i = 0; i < txs.size(); i++) {
            transaction tx = txs.get(i);
            double z = (tx.getAmount() - mean) / stddev;
            if (Math.abs(z) > 2.0) {
                String type = z > 0 ? "MONTANT_ÉLEVÉ" : "MONTANT_FAIBLE";
                String severity = Math.abs(z) > 3.0 ? "CRITIQUE" : "ATTENTION";
                alerts.add(new AnomalyAlert(
                        tx.getIdtransaction(),
                        tx.getProject() != null ? tx.getProject() : "Inconnu",
                        tx.getAmount(),
                        Math.round(z * 100.0) / 100.0,
                        type,
                        severity,
                        "Transaction " + (z > 0 ? "+" : "") + String.format("%.0f", Math.abs(z)) + "σ de la moyenne"
                ));
            }
        }
        return alerts;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  4. KPIs RÉSUMÉS
    // ══════════════════════════════════════════════════════════════════════
    private Map<String, Object> computeKpis(List<Invoice> invoices, List<transaction> txs) {
        Map<String, Object> kpis = new LinkedHashMap<>();

        long paid   = invoices.stream().filter(i -> "PAID".equals(i.getStatus())).count();
        long unpaid = invoices.stream().filter(i -> "UNPAID".equals(i.getStatus())).count();
        long overdue = invoices.stream().filter(this::isOverdue).count();

        double totalTTC  = invoices.stream().mapToDouble(i -> i.getAmountTTC() != null ? i.getAmountTTC() : 0).sum();
        double paidTTC   = invoices.stream().filter(i -> "PAID".equals(i.getStatus()))
                .mapToDouble(i -> i.getAmountTTC() != null ? i.getAmountTTC() : 0).sum();
        double unpaidTTC = totalTTC - paidTTC;

        kpis.put("totalInvoices",  invoices.size());
        kpis.put("paidInvoices",   paid);
        kpis.put("unpaidInvoices", unpaid);
        kpis.put("overdueInvoices", overdue);
        kpis.put("totalRevenueTTC", Math.round(totalTTC * 1000.0) / 1000.0);
        kpis.put("collectedTTC",    Math.round(paidTTC * 1000.0) / 1000.0);
        kpis.put("pendingTTC",      Math.round(unpaidTTC * 1000.0) / 1000.0);
        kpis.put("recoveryRate",    invoices.isEmpty() ? 0 : Math.round((double) paid / invoices.size() * 10000.0) / 100.0);
        kpis.put("overdueRate",     invoices.isEmpty() ? 0 : Math.round((double) overdue / invoices.size() * 10000.0) / 100.0);

        double totalIn  = txs.stream().filter(t -> t.getAmount() > 0).mapToDouble(t -> t.getAmount()).sum();
        double totalOut = Math.abs(txs.stream().filter(t -> t.getAmount() < 0).mapToDouble(t -> t.getAmount()).sum());
        kpis.put("cashInflow",  Math.round(totalIn  * 100.0) / 100.0);
        kpis.put("cashOutflow", Math.round(totalOut * 100.0) / 100.0);
        kpis.put("netCashFlow", Math.round((totalIn - totalOut) * 100.0) / 100.0);

        return kpis;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  5. RECOMMANDATIONS AUTOMATIQUES
    // ══════════════════════════════════════════════════════════════════════
    private List<String> generateRecommendations(FinancialHealthReport report) {
        List<String> recs = new ArrayList<>();
        double score = report.getHealthScore();
        Map<String, Object> kpis = report.getKpis();

        double recoveryRate = (double) kpis.getOrDefault("recoveryRate", 0);
        double overdueRate  = (double) kpis.getOrDefault("overdueRate", 0);
        double netCash      = (double) kpis.getOrDefault("netCashFlow", 0);
        int anomalies       = report.getAnomalies().size();

        if (recoveryRate < 60)
            recs.add("⚠️ Taux de recouvrement critique (" + recoveryRate + "%) — Activez les relances automatiques pour les factures UNPAID.");

        if (recoveryRate >= 80)
            recs.add("✅ Excellent taux de recouvrement (" + recoveryRate + "%) — Continuez sur cette lancée !");

        if (overdueRate > 20)
            recs.add("🔴 " + overdueRate + "% des factures sont en retard de plus de 30 jours — Lancez une campagne de relance urgente.");

        if (netCash < 0)
            recs.add("💸 Flux de trésorerie négatif (" + netCash + " TND) — Vos dépenses dépassent vos revenus. Révisez les postes de coût.");

        if (netCash > 0)
            recs.add("💰 Flux net positif (" + netCash + " TND) — Considérez d'investir l'excédent ou de réduire les escrows bloqués.");

        if (anomalies > 0)
            recs.add("🔍 " + anomalies + " transaction(s) anormale(s) détectée(s) — Vérifiez ces montants inhabituels dans l'onglet Anomalies.");

        if (score < 40)
            recs.add("🆘 Score de santé critique — Contactez votre comptable et révisez votre plan de trésorerie immédiatement.");
        else if (score < 60)
            recs.add("📊 Score moyen — Concentrez-vous sur l'encaissement des factures UNPAID pour améliorer votre score.");
        else if (score >= 80)
            recs.add("🌟 Excellente santé financière — Votre plateforme est prête pour une phase d'expansion.");

        if (recs.isEmpty())
            recs.add("✅ Aucun problème majeur détecté — Continuez à surveiller régulièrement vos indicateurs.");

        return recs;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ══════════════════════════════════════════════════════════════════════
    private boolean isOverdue(Invoice invoice) {
        if (!"UNPAID".equals(invoice.getStatus()) || invoice.getIssueDate() == null) return false;
        try {
            LocalDate issued = LocalDate.parse(invoice.getIssueDate());
            return issued.isBefore(LocalDate.now().minusDays(30));
        } catch (Exception e) { return false; }
    }

    private String scoreToLevel(double score) {
        if (score >= 80) return "EXCELLENT";
        if (score >= 60) return "BON";
        if (score >= 40) return "MOYEN";
        return "CRITIQUE";
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CLASSES RÉSULTAT (Records)
    // ══════════════════════════════════════════════════════════════════════
    public static class FinancialHealthReport {
        private double healthScore;
        private String healthLevel;
        private Map<String, Object> kpis;
        private List<CashFlowPrediction> cashFlowPredictions;
        private List<AnomalyAlert> anomalies;
        private List<String> recommendations;

        public double getHealthScore() { return healthScore; }
        public void setHealthScore(double healthScore) { this.healthScore = healthScore; }
        public String getHealthLevel() { return healthLevel; }
        public void setHealthLevel(String healthLevel) { this.healthLevel = healthLevel; }
        public Map<String, Object> getKpis() { return kpis; }
        public void setKpis(Map<String, Object> kpis) { this.kpis = kpis; }
        public List<CashFlowPrediction> getCashFlowPredictions() { return cashFlowPredictions; }
        public void setCashFlowPredictions(List<CashFlowPrediction> cashFlowPredictions) { this.cashFlowPredictions = cashFlowPredictions; }
        public List<AnomalyAlert> getAnomalies() { return anomalies; }
        public void setAnomalies(List<AnomalyAlert> anomalies) { this.anomalies = anomalies; }
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    }

    public record CashFlowPrediction(String month, double predictedAmount, int monthOffset) {}

    public record AnomalyAlert(
            Long transactionId,
            String project,
            double amount,
            double zScore,
            String type,
            String severity,
            String description
    ) {}
}
