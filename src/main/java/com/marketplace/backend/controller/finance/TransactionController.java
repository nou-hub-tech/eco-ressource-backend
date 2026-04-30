package com.marketplace.backend.controller.finance;

import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.finance.transaction;
import com.marketplace.backend.repository.EnterpriseRepository;
import com.marketplace.backend.repository.UserRepository;
import com.marketplace.backend.repository.finance.TransactionRepository;
import com.marketplace.backend.service.finance.ITransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final ITransactionService service;
    private final UserRepository userRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final TransactionRepository transactionRepository;

    // ─── CRUD standard ───────────────────────────────────────────

    @PostMapping("/add")
    public transaction add(@RequestBody transaction t) {
        // Associer automatiquement l'entreprise connectée à la transaction
        Enterprise ent = getCurrentEnterprise();
        if (ent != null) t.setEnterpriseId(ent.getId());
        return service.addTransaction(t);
    }

    /** Toutes les transactions (admin) */
    @GetMapping("/all")
    public List<transaction> getAll() {
        return service.retrieveAllTransactions();
    }

    /**
     * 🏢 Transactions de l'entreprise connectée seulement
     * GET /api/transactions/my
     */
    @GetMapping("/my")
    public ResponseEntity<List<transaction>> getMyTransactions() {
        Enterprise ent = getCurrentEnterprise();
        if (ent == null) {
            log.warn("[TRANSACTIONS] Entreprise introuvable pour l'utilisateur connecté");
            return ResponseEntity.ok(List.of());
        }
        List<transaction> txs = transactionRepository.findByEnterpriseId(ent.getId());
        log.info("[TRANSACTIONS] {} transactions trouvées pour l'entreprise '{}'", txs.size(), ent.getCompanyName());
        return ResponseEntity.ok(txs);
    }

    @GetMapping("/{id}")
    public transaction getById(@PathVariable Long id) {
        return service.retrieveTransaction(id);
    }

    @PutMapping("/update")
    public transaction update(@RequestBody transaction t) {
        return service.modifyTransaction(t);
    }

    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable Long id) {
        service.removeTransaction(id);
    }

    @GetMapping("/positive")
    public List<transaction> getPositiveTransactions() {
        return service.retrieveAllTransactions()
                .stream()
                .filter(t -> t.getAmount() > 0)
                .toList();
    }

    // ─── Utilitaire ──────────────────────────────────────────────

    private Enterprise getCurrentEnterprise() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return null;
            User user = userRepository.findByEmailWithProfiles(auth.getName()).orElse(null);
            if (user == null) return null;
            return enterpriseRepository.findByUserId(user.getId()).orElse(null);
        } catch (Exception e) {
            log.error("[TRANSACTIONS] Erreur récupération entreprise : {}", e.getMessage());
            return null;
        }
    }
}
