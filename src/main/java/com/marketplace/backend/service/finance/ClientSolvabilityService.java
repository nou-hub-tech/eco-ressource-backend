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
 * ══════════════════════════════════════════════════════════════
 *  🏦 SERVICE DE SOLVABILITÉ CLIENT — Modèle Crédit Bancaire
 * ══════════════════════════════════════════════════════════════
 *
 * Génère pour chaque client un profil complet inspiré des méthodes
 * de scoring utilisées par les banques et les fintechs :
 *
 *  ① Taux de recouvrement historique   (35%)
 *  ② DSO — Days Sales Outstanding      (25%)
 *  ③ Ratio d'endettement               (20%)
 *  ④ Régularité des paiements          (15%)
 *  ⑤ Tendance comportementale          (5%)
 *
 *  → Credit Rating : AAA / AA / A / BBB / BB / B / CCC
 *  → Payment Prediction : probabilité 0–100% par facture
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientSolvabilityService {

    private final InvoiceRepository invoiceRepository;
    private final EnterpriseContextHelper enterpriseContext;

    // ═══════════════════════════════════════════════════════════
    //  POINT D'ENTRÉE
    // ═══════════════════════════════════════════════════════════
    public SolvabilityReport generateSolvabilityReport() {
        // 🏢 Filtrer par entreprise connectee
        String companyName = enterpriseContext.getCurrentCompanyName();
        List<Invoice> all;
        if (companyName != null) {
            all = invoiceRepository.findByEnterpriseCompanyName(companyName);
            log.info("[SOLVABILITY] Rapport pour '{}' — {} factures", companyName, all.size());
        } else {
            all = invoiceRepository.findAll();
        }

        // 1. Profils de solvabilite par client
        List<ClientSolvabilityProfile> profiles = buildClientProfiles(all);

        // 2. Prédictions de paiement par facture UNPAID
        List<InvoicePaymentPrediction> predictions = predictPayments(all, profiles);

        // 3. Résumé global du portefeuille
        PortfolioHealth health = computePortfolioHealth(all, profiles, predictions);

        // 4. Conseils stratégiques enrichis
        List<StrategicAdvice> advices = generateStrategicAdvice(profiles, predictions, health);

        return new SolvabilityReport(profiles, predictions, health, advices);
    }

    // ═══════════════════════════════════════════════════════════
    //  1. PROFILS DE SOLVABILITÉ PAR CLIENT
    // ═══════════════════════════════════════════════════════════
    private List<ClientSolvabilityProfile> buildClientProfiles(List<Invoice> all) {
        Map<String, List<Invoice>> byClient = all.stream()
                .filter(i -> i.getClientName() != null)
                .collect(Collectors.groupingBy(Invoice::getClientName));

        return byClient.entrySet().stream()
                .map(e -> buildProfile(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingDouble(ClientSolvabilityProfile::solvabilityScore))
                .collect(Collectors.toList());
    }

    private ClientSolvabilityProfile buildProfile(String clientName, List<Invoice> invoices) {
        List<Invoice> paid   = invoices.stream().filter(i -> "PAID".equals(i.getStatus())).collect(Collectors.toList());
        List<Invoice> unpaid = invoices.stream().filter(i -> "UNPAID".equals(i.getStatus())).collect(Collectors.toList());

        int total = invoices.size();
        int paidCount   = paid.size();
        int unpaidCount = unpaid.size();

        // ── KPI 1 : Taux de recouvrement (35%) ──────────────────
        double recoveryRate = total == 0 ? 0 : (double) paidCount / total * 100;
        double kpi1 = recoveryRate * 0.35; // max 35 pts

        // ── KPI 2 : DSO — Days Sales Outstanding (25%) ──────────
        double avgDso = computeAverageDSO(paid);
        double dsoScore = dsoToScore(avgDso); // 0–100
        double kpi2 = dsoScore * 0.25; // max 25 pts

        // ── KPI 3 : Ratio d'endettement (20%) ───────────────────
        double totalAmount  = invoices.stream().mapToDouble(i -> safe(i.getAmountTTC())).sum();
        double unpaidAmount = unpaid.stream().mapToDouble(i -> safe(i.getAmountTTC())).sum();
        double debtRatio    = totalAmount == 0 ? 0 : unpaidAmount / totalAmount;
        double debtScore    = Math.max(0, (1 - debtRatio) * 100);
        double kpi3 = debtScore * 0.20; // max 20 pts

        // ── KPI 4 : Régularité des paiements (15%) ──────────────
        double regularityScore = computeRegularity(paid); // 0–100
        double kpi4 = regularityScore * 0.15; // max 15 pts

        // ── KPI 5 : Tendance comportementale (5%) ───────────────
        double trendScore = computeTrend(invoices); // 0–100
        double kpi5 = trendScore * 0.05; // max 5 pts

        // ── Score final & Rating ─────────────────────────────────
        double solvabilityScore = Math.round((kpi1 + kpi2 + kpi3 + kpi4 + kpi5) * 10.0) / 10.0;
        String creditRating     = scoreToRating(solvabilityScore);
        String ratingDescription = ratingDescription(creditRating);

        // ── Métriques additionnelles ─────────────────────────────
        int overdueCount = (int) unpaid.stream()
                .filter(i -> daysOverdue(i) > 30).count();
        double maxDaysOverdue = unpaid.stream()
                .mapToDouble(this::daysOverdue).max().orElse(0);

        // ── Probabilité de paiement prochain (score client) ──────
        double paymentProbability = computeClientPaymentProbability(
                recoveryRate, avgDso, debtRatio, regularityScore);

        return new ClientSolvabilityProfile(
                clientName,
                total, paidCount, unpaidCount, overdueCount,
                Math.round(totalAmount  * 100.0) / 100.0,
                Math.round(unpaidAmount * 100.0) / 100.0,
                Math.round(recoveryRate * 10.0)  / 10.0,
                Math.round(avgDso * 10.0)        / 10.0,
                Math.round(debtRatio * 1000.0)   / 10.0, // en %
                Math.round(regularityScore * 10.0) / 10.0,
                Math.round(trendScore * 10.0)    / 10.0,
                solvabilityScore,
                creditRating,
                ratingDescription,
                Math.round(paymentProbability * 10.0) / 10.0,
                (long) maxDaysOverdue
        );
    }

    // ── DSO ─────────────────────────────────────────────────────
    private double computeAverageDSO(List<Invoice> paid) {
        if (paid.isEmpty()) return 999; // aucune facture payée → DSO très mauvais
        OptionalDouble avg = paid.stream()
                .mapToDouble(i -> {
                    if (i.getIssueDate() == null || i.getDeliveredAt() == null) return 30;
                    try {
                        LocalDate issued    = LocalDate.parse(i.getIssueDate());
                        LocalDate delivered = LocalDate.parse(i.getDeliveredAt());
                        return Math.max(0, ChronoUnit.DAYS.between(issued, delivered));
                    } catch (Exception e) { return 30; }
                })
                .average();
        return avg.orElse(999);
    }

    private double dsoToScore(double dso) {
        if (dso <= 0)   return 100;
        if (dso <= 15)  return 95;
        if (dso <= 30)  return 80;
        if (dso <= 45)  return 62;
        if (dso <= 60)  return 45;
        if (dso <= 90)  return 25;
        if (dso <= 120) return 10;
        return 2; // >120 jours ou aucun paiement
    }

    // ── Régularité ───────────────────────────────────────────────
    private double computeRegularity(List<Invoice> paid) {
        if (paid.size() < 2) return paid.isEmpty() ? 0 : 50;
        List<Double> delays = paid.stream()
                .map(i -> {
                    if (i.getIssueDate() == null || i.getDeliveredAt() == null) return 30.0;
                    try {
                        LocalDate issued    = LocalDate.parse(i.getIssueDate());
                        LocalDate delivered = LocalDate.parse(i.getDeliveredAt());
                        return (double) Math.max(0, ChronoUnit.DAYS.between(issued, delivered));
                    } catch (Exception e) { return 30.0; }
                })
                .collect(Collectors.toList());

        double mean = delays.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        if (mean == 0) return 80;
        double variance = delays.stream().mapToDouble(d -> Math.pow(d - mean, 2)).average().orElse(0);
        double cv = Math.sqrt(variance) / mean; // coefficient de variation
        return Math.max(0, Math.min(100, (1 - cv) * 100));
    }

    // ── Tendance ─────────────────────────────────────────────────
    private double computeTrend(List<Invoice> invoices) {
        // Compare le taux de paiement dans les 90 derniers jours vs l'historique total
        LocalDate cutoff = LocalDate.now().minusDays(90);

        List<Invoice> recent = invoices.stream()
                .filter(i -> {
                    if (i.getIssueDate() == null) return false;
                    try { return LocalDate.parse(i.getIssueDate()).isAfter(cutoff); }
                    catch (Exception e) { return false; }
                })
                .collect(Collectors.toList());

        if (recent.isEmpty()) return 50; // pas de données récentes

        double recentRate  = (double) recent.stream().filter(i -> "PAID".equals(i.getStatus())).count() / recent.size();
        double overallRate = (double) invoices.stream().filter(i -> "PAID".equals(i.getStatus())).count() / invoices.size();

        if (recentRate > overallRate + 0.1) return 90;   // amélioration nette
        if (recentRate > overallRate - 0.05) return 60;  // stable
        return 20; // dégradation
    }

    // ── Probabilité client ────────────────────────────────────────
    private double computeClientPaymentProbability(double recoveryRate, double avgDso,
                                                    double debtRatio, double regularityScore) {
        double p = 0;
        p += recoveryRate   * 0.50;  // historique prime
        p += dsoToScore(avgDso) * 0.25;
        p += (1 - debtRatio) * 100 * 0.15;
        p += regularityScore * 0.10;
        return Math.min(98, Math.max(2, p));
    }

    // ═══════════════════════════════════════════════════════════
    //  2. PRÉDICTIONS DE PAIEMENT PAR FACTURE
    // ═══════════════════════════════════════════════════════════
    private List<InvoicePaymentPrediction> predictPayments(List<Invoice> all,
                                                            List<ClientSolvabilityProfile> profiles) {
        Map<String, ClientSolvabilityProfile> profileMap = profiles.stream()
                .collect(Collectors.toMap(ClientSolvabilityProfile::clientName, p -> p, (a, b) -> a));

        return all.stream()
                .filter(i -> "UNPAID".equals(i.getStatus()))
                .map(i -> {
                    ClientSolvabilityProfile profile = profileMap.get(i.getClientName());
                    double probability = computeInvoiceProbability(i, profile);
                    String prediction  = probabilityToPrediction(probability);
                    String urgency     = probabilityToUrgency(probability, daysOverdue(i));
                    String estimatedPaymentDate = estimatePaymentDate(i, profile, probability);

                    return new InvoicePaymentPrediction(
                            i.getId(),
                            i.getInvoiceNumber(),
                            i.getClientName(),
                            i.getProject(),
                            safe(i.getAmountTTC()),
                            i.getIssueDate(),
                            (long) daysOverdue(i),
                            Math.round(probability * 10.0) / 10.0,
                            prediction,
                            urgency,
                            estimatedPaymentDate,
                            profile != null ? profile.creditRating() : "N/A",
                            profile != null ? profile.solvabilityScore() : 0
                    );
                })
                .sorted(Comparator.comparingDouble(InvoicePaymentPrediction::paymentProbability))
                .collect(Collectors.toList());
    }

    private double computeInvoiceProbability(Invoice i, ClientSolvabilityProfile profile) {
        double p = 60; // base neutre

        // Influence de la solvabilité client (±40pts)
        if (profile != null) {
            p = profile.paymentProbability() * 0.6; // base = profil client
        }

        // Pénalité ancienneté (−40pts max)
        long days     = (long) daysOverdue(i);
        double agePenalty = Math.min(40, days / 90.0 * 40);
        p -= agePenalty;

        // Pénalité montant élevé (−15pts max)
        double amount = safe(i.getAmountTTC());
        if (amount > 100_000) p -= 15;
        else if (amount > 50_000) p -= 10;
        else if (amount > 20_000) p -= 5;

        // Bonus si escrow présent (garantie)
        if (i.getLinkedEscrowId() != null) p += 10;

        return Math.min(98, Math.max(2, p));
    }

    private String probabilityToPrediction(double p) {
        if (p >= 80) return "Paiement très probable ce mois";
        if (p >= 60) return "Paiement probable — surveiller";
        if (p >= 40) return "Incertain — relance recommandée";
        if (p >= 20) return "Risque élevé de non-paiement";
        return "Non-recouvrement probable — action juridique";
    }

    private String probabilityToUrgency(double p, double days) {
        if (p < 20 || days > 90) return "CRITIQUE";
        if (p < 40 || days > 60) return "HAUTE";
        if (p < 60 || days > 30) return "MOYENNE";
        return "FAIBLE";
    }

    private String estimatePaymentDate(Invoice i, ClientSolvabilityProfile profile, double probability) {
        if (probability < 20) return "Non prévu";
        double dso = profile != null ? profile.avgDso() : 45;
        if (i.getIssueDate() == null) return "Inconnu";
        try {
            LocalDate issued    = LocalDate.parse(i.getIssueDate());
            LocalDate estimated = issued.plusDays((long) dso);
            if (estimated.isBefore(LocalDate.now())) estimated = LocalDate.now().plusDays(15);
            return estimated.toString();
        } catch (Exception e) { return "Inconnu"; }
    }

    // ═══════════════════════════════════════════════════════════
    //  3. SANTÉ DU PORTEFEUILLE
    // ═══════════════════════════════════════════════════════════
    private PortfolioHealth computePortfolioHealth(List<Invoice> all,
                                                    List<ClientSolvabilityProfile> profiles,
                                                    List<InvoicePaymentPrediction> predictions) {
        int totalInvoices  = all.size();
        int paidInvoices   = (int) all.stream().filter(i -> "PAID".equals(i.getStatus())).count();
        int unpaidInvoices = totalInvoices - paidInvoices;

        double totalRevenue   = all.stream().mapToDouble(i -> safe(i.getAmountTTC())).sum();
        double collectedRev   = all.stream().filter(i -> "PAID".equals(i.getStatus())).mapToDouble(i -> safe(i.getAmountTTC())).sum();
        double atRiskAmount   = predictions.stream().filter(p -> p.paymentProbability() < 40).mapToDouble(InvoicePaymentPrediction::amount).sum();
        double expectedInflow = predictions.stream().mapToDouble(p -> p.amount() * p.paymentProbability() / 100).sum();

        // Compter les ratings
        Map<String, Long> ratingCounts = profiles.stream()
                .collect(Collectors.groupingBy(ClientSolvabilityProfile::creditRating, Collectors.counting()));

        double avgSolvability = profiles.stream()
                .mapToDouble(ClientSolvabilityProfile::solvabilityScore).average().orElse(0);
        String portfolioRating = scoreToRating(avgSolvability);

        int criticalClients  = (int) profiles.stream().filter(p -> "CCC".equals(p.creditRating()) || "B".equals(p.creditRating())).count();
        int excellentClients = (int) profiles.stream().filter(p -> "AAA".equals(p.creditRating()) || "AA".equals(p.creditRating())).count();

        return new PortfolioHealth(
                totalInvoices, paidInvoices, unpaidInvoices,
                Math.round(totalRevenue   * 100.0) / 100.0,
                Math.round(collectedRev   * 100.0) / 100.0,
                Math.round(atRiskAmount   * 100.0) / 100.0,
                Math.round(expectedInflow * 100.0) / 100.0,
                Math.round(avgSolvability * 10.0)  / 10.0,
                portfolioRating,
                criticalClients,
                excellentClients,
                profiles.size(),
                ratingCounts
        );
    }

    // ═══════════════════════════════════════════════════════════
    //  4. CONSEILS STRATÉGIQUES
    // ═══════════════════════════════════════════════════════════
    private List<StrategicAdvice> generateStrategicAdvice(List<ClientSolvabilityProfile> profiles,
                                                           List<InvoicePaymentPrediction> predictions,
                                                           PortfolioHealth health) {
        List<StrategicAdvice> advices = new ArrayList<>();

        // Clients CCC
        profiles.stream().filter(p -> "CCC".equals(p.creditRating())).forEach(p ->
                advices.add(new StrategicAdvice("CRITIQUE",
                        "Client " + p.clientName() + " — Rating CCC",
                        "Suspendre le crédit. Exiger paiement comptant ou acompte 100%. " +
                        "Montant à risque : " + String.format("%.0f", p.unpaidAmount()) + " TND.",
                        "Stop crédit immédiat"))
        );

        // Clients B
        profiles.stream().filter(p -> "B".equals(p.creditRating())).forEach(p ->
                advices.add(new StrategicAdvice("HAUTE",
                        "Client " + p.clientName() + " — Rating B",
                        "Exiger un acompte de 50% sur les nouvelles commandes. " +
                        "DSO moyen : " + String.format("%.0f", p.avgDso()) + " jours.",
                        "Acompte 50%"))
        );

        // Factures avec probabilité < 20%
        predictions.stream().filter(p -> p.paymentProbability() < 20)
                .forEach(p -> advices.add(new StrategicAdvice("CRITIQUE",
                        "Facture " + p.invoiceNumber() + " — " + p.clientName(),
                        "Probabilité de recouvrement : " + String.format("%.0f", p.paymentProbability()) + "%. " +
                        "Montant : " + String.format("%.0f", p.amount()) + " TND. " +
                        "Transmettre au service juridique ou contentieux.",
                        "Procédure juridique"))
        );

        // Montant global à risque
        if (health.atRiskAmount() > 10_000)
            advices.add(new StrategicAdvice("MOYENNE",
                    "Montant à risque de non-recouvrement",
                    String.format("%.0f", health.atRiskAmount()) + " TND présentent un risque fort " +
                    "(probabilité paiement < 40%). Envisagez l'affacturage pour sécuriser votre trésorerie.",
                    "Affacturage"));

        // Bonne santé
        if (health.excellentClients() > 0)
            advices.add(new StrategicAdvice("INFO",
                    health.excellentClients() + " client(s) AAA/AA",
                    "Ces clients sont fiables. Proposez-leur des conditions préférentielles " +
                    "(paiement différé, volume discount) pour renforcer la relation.",
                    "Fidélisation"));

        // Portfolio sain
        if (advices.isEmpty())
            advices.add(new StrategicAdvice("INFO",
                    "Portefeuille en bonne santé",
                    "Aucun risque critique détecté. Continuez à surveiller les indicateurs DSO et " +
                    "taux de recouvrement. Rating moyen : " + health.portfolioRating() + ".",
                    "Surveillance continue"));

        return advices;
    }

    // ═══════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ═══════════════════════════════════════════════════════════
    private double daysOverdue(Invoice i) {
        if (i.getIssueDate() == null) return 0;
        try {
            LocalDate issued = LocalDate.parse(i.getIssueDate());
            return Math.max(0, ChronoUnit.DAYS.between(issued, LocalDate.now()));
        } catch (Exception e) { return 0; }
    }

    private double safe(Double v) { return v != null ? v : 0; }

    private String scoreToRating(double score) {
        if (score >= 85) return "AAA";
        if (score >= 70) return "AA";
        if (score >= 55) return "A";
        if (score >= 40) return "BBB";
        if (score >= 25) return "BB";
        if (score >= 10) return "B";
        return "CCC";
    }

    private String ratingDescription(String rating) {
        return switch (rating) {
            case "AAA" -> "Solvabilite excellente — Risque quasiment nul";
            case "AA"  -> "Tres bonne solvabilite — Tres fiable";
            case "A"   -> "Bonne solvabilite — Risque faible";
            case "BBB" -> "Solvabilite moderee — Surveillance recommandee";
            case "BB"  -> "Solvabilite degradee — Risque notable";
            case "B"   -> "Risque eleve — Acompte recommande";
            case "CCC" -> "Solvabilite critique — Ne pas accorder de credit";
            default    -> "Donnees insuffisantes";
        };
    }

    // ═══════════════════════════════════════════════════════════
    //  TYPES DE RETOUR
    // ═══════════════════════════════════════════════════════════

    public record ClientSolvabilityProfile(
            String clientName,
            int    totalInvoices,
            int    paidInvoices,
            int    unpaidInvoices,
            int    overdueInvoices,
            double totalAmount,
            double unpaidAmount,
            double recoveryRate,   // %
            double avgDso,         // jours
            double debtRatio,      // %
            double regularityScore,// 0-100
            double trendScore,     // 0-100
            double solvabilityScore,
            String creditRating,   // AAA→CCC
            String ratingDescription,
            double paymentProbability, // %
            long   maxDaysOverdue
    ) {}

    public record InvoicePaymentPrediction(
            Long   invoiceId,
            String invoiceNumber,
            String clientName,
            String project,
            double amount,
            String issueDate,
            long   daysOverdue,
            double paymentProbability, // %
            String prediction,
            String urgency,            // CRITIQUE / HAUTE / MOYENNE / FAIBLE
            String estimatedPaymentDate,
            String clientRating,
            double clientSolvabilityScore
    ) {}

    public record PortfolioHealth(
            int    totalInvoices,
            int    paidInvoices,
            int    unpaidInvoices,
            double totalRevenue,
            double collectedRevenue,
            double atRiskAmount,
            double expectedInflow,
            double avgSolvabilityScore,
            String portfolioRating,
            int    criticalClients,
            int    excellentClients,
            int    totalClients,
            Map<String, Long> ratingDistribution
    ) {}

    public record StrategicAdvice(
            String severity,   // CRITIQUE / HAUTE / MOYENNE / INFO
            String title,
            String description,
            String action
    ) {}

    public record SolvabilityReport(
            List<ClientSolvabilityProfile> clientProfiles,
            List<InvoicePaymentPrediction> paymentPredictions,
            PortfolioHealth                portfolioHealth,
            List<StrategicAdvice>          strategicAdvices
    ) {}
}
