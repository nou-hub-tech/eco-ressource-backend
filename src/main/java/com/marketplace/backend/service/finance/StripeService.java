package com.marketplace.backend.service.finance;

import com.marketplace.backend.entity.finance.escrow;
import com.marketplace.backend.repository.finance.EscrowRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 💳 StripeService — Gestion des paiements escrow via Stripe
 *
 * FLUX ESCROW STRIPE :
 * ─────────────────────────────────────────────────────
 * ÉTAPE 1 (Création Escrow) :
 *   initPayment() → Stripe crée un PaymentIntent
 *                → Retourne client_secret au frontend
 *                → Frontend affiche formulaire carte Stripe
 *                → L'acheteur paie → argent BLOQUÉ chez Stripe
 *
 * ÉTAPE 2 (Livraison confirmée - déjà implémenté) :
 *   releaseEscrow() → Stripe libère l'argent vers le vendeur (capture)
 *                  → Escrow status → RELEASED
 *                  → Email envoyé (logique existante)
 * ─────────────────────────────────────────────────────
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService {

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.currency:tnd}")
    private String currency;

    private final EscrowRepository escrowRepository;

    /** Initialise la clé Stripe au démarrage du serveur */
    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
        log.info("[STRIPE] ✅ SDK Stripe initialisé — Mode : {}",
                secretKey.startsWith("sk_test_") ? "TEST" : "PRODUCTION");
    }

    /**
     * ÉTAPE 1 — Créer un PaymentIntent Stripe pour un escrow.
     * Stripe bloque l'argent mais ne le prélève pas encore.
     *
     * @param escrowId ID de l'escrow à payer
     * @return Map avec client_secret (pour le frontend Stripe.js) et paymentIntentId
     */
    public Map<String, String> initPayment(Long escrowId) throws StripeException {

        escrow esc = escrowRepository.findById(escrowId)
                .orElseThrow(() -> new RuntimeException("Escrow introuvable : " + escrowId));

        // Stripe travaille en centimes (1 TND = 100 millimes pour Stripe)
        long amountInCents = Math.round(esc.getAmount() * 100);

        log.info("[STRIPE] Création PaymentIntent — Escrow #{} | {} TND ({} centimes)",
                escrowId, esc.getAmount(), amountInCents);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(currency)
                .setDescription("EcoRessource B2B — Escrow : " + esc.getProject())
                .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.AUTOMATIC)
                .putMetadata("escrowId", String.valueOf(escrowId))
                .putMetadata("project", esc.getProject())
                .build();

        PaymentIntent intent = PaymentIntent.create(params);

        log.info("[STRIPE] ✅ PaymentIntent créé : {} | Status : {}",
                intent.getId(), intent.getStatus());

        // Sauvegarder la référence Stripe dans l'escrow
        esc.setKonnectPaymentRef(intent.getId()); // réutilise le champ existant pour stocker stripePaymentId
        escrowRepository.save(esc);

        Map<String, String> result = new HashMap<>();
        result.put("clientSecret", intent.getClientSecret());
        result.put("paymentIntentId", intent.getId());
        result.put("amount", String.valueOf(esc.getAmount()));
        result.put("project", esc.getProject());
        return result;
    }

    /**
     * ÉTAPE 2 — Récupérer le statut d'un PaymentIntent Stripe
     * Appelé après confirmation côté frontend pour vérifier le paiement
     */
    public String getPaymentStatus(String paymentIntentId) throws StripeException {
        PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
        log.info("[STRIPE] Statut PaymentIntent {} : {}", paymentIntentId, intent.getStatus());
        return intent.getStatus(); // "succeeded" | "processing" | "requires_payment_method"
    }
}
