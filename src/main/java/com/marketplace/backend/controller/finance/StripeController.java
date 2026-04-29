package com.marketplace.backend.controller.finance;

import com.marketplace.backend.entity.finance.EscrowStatus;
import com.marketplace.backend.repository.finance.EscrowRepository;
import com.marketplace.backend.service.finance.IEscrowService;
import com.marketplace.backend.service.finance.StripeService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.marketplace.backend.service.finance.DeliveryPollingService;
import com.marketplace.backend.repository.finance.InvoiceRepository;
import java.util.Map;
import java.util.Optional;

/**
 * 💳 StripeController — Endpoints pour l'intégration Stripe
 *
 * POST /api/stripe/init-payment   → Créer PaymentIntent, retourner client_secret
 * POST /api/stripe/webhook        → Recevoir confirmation Stripe (sans JWT)
 * POST /api/stripe/confirm        → Frontend confirme paiement réussi
 * GET  /api/stripe/public-key     → Fournir la clé publique au frontend
 */
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
public class StripeController {

    private final StripeService stripeService;
    private final IEscrowService escrowService;
    private final EscrowRepository escrowRepository;
    private final InvoiceRepository invoiceRepository;
    private final DeliveryPollingService deliveryPollingService;

    @Value("${stripe.public-key}")
    private String publicKey;

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    // ──────────────────────────────────────────────
    // 1. CLÉ PUBLIQUE (pour Stripe.js côté frontend)
    // ──────────────────────────────────────────────

    /**
     * GET /api/stripe/public-key
     * Le frontend Angular récupère la clé publique pour initialiser Stripe.js
     */
    @GetMapping("/public-key")
    public ResponseEntity<Map<String, String>> getPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", publicKey));
    }

    /**
     * GET /api/stripe/polling-status
     * Retourne le timestamp du dernier check de livraison (pour badge UI)
     */
    @GetMapping("/polling-status")
    public ResponseEntity<Map<String, String>> getPollingStatus() {
        var lastCheck = deliveryPollingService.getLastCheckTime();
        String formatted = lastCheck != null
                ? lastCheck.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
                : "En attente...";
        return ResponseEntity.ok(Map.of("lastCheck", formatted, "active", "true"));
    }

    // ──────────────────────────────────────────────
    // 2. INITIER UN PAIEMENT
    // ──────────────────────────────────────────────

    /**
     * POST /api/stripe/init-payment
     * Body: { "escrowId": 7 }
     * Retourne: { clientSecret, paymentIntentId, amount, project }
     *
     * Le clientSecret est transmis à Stripe.js pour afficher le formulaire de carte
     */
    @PostMapping("/init-payment")
    public ResponseEntity<?> initPayment(@RequestBody Map<String, Object> body) {
        try {
            Long escrowId = Long.parseLong(body.get("escrowId").toString());
            log.info("[STRIPE CTRL] Init paiement pour escrow #{}", escrowId);

            Map<String, String> result = stripeService.initPayment(escrowId);
            return ResponseEntity.ok(result);

        } catch (Exception ex) {
            log.error("[STRIPE CTRL] Erreur init : {}", ex.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // ──────────────────────────────────────────────
    // 3. CONFIRMER LE PAIEMENT (appelé par le frontend après succès Stripe.js)
    // ──────────────────────────────────────────────

    /**
     * POST /api/stripe/confirm
     * Body: { "paymentIntentId": "pi_xxx", "escrowId": 7 }
     *
     * Flux :
     *   1. Vérifie le paiement chez Stripe (anti-fraude)
     *   2. Enregistre le paymentIntentId sur l'escrow
     *   3. Facture liée → Encaissée   (argent reçu)
     *   4. Escrow reste LOCKED        (libération à la livraison)
     */
    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(@RequestBody Map<String, Object> body) {
        try {
            String paymentIntentId = body.get("paymentIntentId").toString();
            Long escrowId = Long.parseLong(body.get("escrowId").toString());

            // 1. Vérifier le statut réel chez Stripe
            String stripeStatus = stripeService.getPaymentStatus(paymentIntentId);
            log.info("[STRIPE CTRL] Confirmation paiement {} → Status Stripe : {}", paymentIntentId, stripeStatus);

            if (!"succeeded".equals(stripeStatus)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "stripeStatus", stripeStatus));
            }

            // 2. Enregistrer le paymentIntentId sur l'escrow
            escrowRepository.findById(escrowId).ifPresent(esc -> {
                esc.setKonnectPaymentRef(paymentIntentId);
                escrowRepository.save(esc);

                // 3. Mettre la facture liée → Encaissée
                com.marketplace.backend.entity.finance.Invoice invoice = null;

                if (esc.getLinkedInvoiceId() != null) {
                    invoice = invoiceRepository.findById(esc.getLinkedInvoiceId()).orElse(null);
                }
                if (invoice == null && esc.getProject() != null) {
                    invoice = invoiceRepository.findAll().stream()
                            .filter(i -> "UNPAID".equals(i.getStatus())
                                    && esc.getProject().equalsIgnoreCase(i.getProject()))
                            .findFirst().orElse(null);
                }
                if (invoice != null && !"PAID".equals(invoice.getStatus())) {
                    invoice.setStatus("PAID");
                    invoiceRepository.save(invoice);
                    log.info("[STRIPE CTRL] 📄 Facture #{} → PAID (escrow #{} reste LOCKED)",
                            invoice.getId(), escrowId);
                }
                // 4. Escrow reste LOCKED — sera libéré à la livraison
                log.info("[STRIPE CTRL] ✅ Escrow #{} LOCKED — fonds sécurisés chez Stripe", escrowId);
            });

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Paiement confirmé — Fonds sécurisés en escrow",
                    "stripeStatus", stripeStatus,
                    "escrowId", escrowId
            ));

        } catch (Exception ex) {
            log.error("[STRIPE CTRL] Erreur confirmation : {}", ex.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // ──────────────────────────────────────────────
    // 4. WEBHOOK STRIPE (sans JWT — appelé par Stripe)
    // ──────────────────────────────────────────────

    /**
     * POST /api/stripe/webhook
     * Stripe envoie un événement signé quand un paiement réussit.
     * On vérifie la signature puis on libère l'escrow si livraison confirmée.
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {

        try {
            Event event;

            if (sigHeader != null && !webhookSecret.isBlank()) {
                // Mode production — vérification de signature Stripe
                event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            } else {
                // Mode développement — parsing sans vérification
                event = Event.GSON.fromJson(payload, Event.class);
                log.warn("[STRIPE WEBHOOK] ⚠️ Signature non vérifiée (mode dev)");
            }

            log.info("[STRIPE WEBHOOK] Événement reçu : {}", event.getType());

            // Traitement selon le type d'événement
            switch (event.getType()) {

                case "payment_intent.succeeded" -> {
                    Optional<StripeObject> stripeObject = event.getDataObjectDeserializer().getObject();
                    if (stripeObject.isPresent()) {
                        PaymentIntent intent = (PaymentIntent) stripeObject.get();
                        String escrowIdStr = intent.getMetadata().get("escrowId");
                        if (escrowIdStr != null) {
                            Long escrowId = Long.parseLong(escrowIdStr);
                            log.info("[STRIPE WEBHOOK] ✅ Paiement réussi pour escrow #{}", escrowId);
                            // Ne pas libérer ici — la libération se fait à la LIVRAISON
                            // (flux automatique déjà en place : polling → LIVREE → RELEASED)
                        }
                    }
                }

                case "payment_intent.payment_failed" -> {
                    log.warn("[STRIPE WEBHOOK] ❌ Paiement échoué");
                }

                default -> log.info("[STRIPE WEBHOOK] Événement ignoré : {}", event.getType());
            }

            return ResponseEntity.ok("OK");

        } catch (SignatureVerificationException ex) {
            log.error("[STRIPE WEBHOOK] Signature invalide : {}", ex.getMessage());
            return ResponseEntity.badRequest().body("Signature invalide");
        } catch (Exception ex) {
            log.error("[STRIPE WEBHOOK] Erreur : {}", ex.getMessage());
            return ResponseEntity.ok("ERROR");
        }
    }
}
