package com.marketplace.backend.controller.finance;

import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.finance.EscrowStatus;
import com.marketplace.backend.entity.finance.escrow;
import com.marketplace.backend.repository.EnterpriseRepository;
import com.marketplace.backend.repository.UserRepository;
import com.marketplace.backend.repository.finance.EscrowRepository;
import com.marketplace.backend.service.finance.IEscrowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/escrow")
@RequiredArgsConstructor
public class EscrowController {

    private final IEscrowService service;
    private final UserRepository userRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final EscrowRepository escrowRepository;

    // ─── CRUD standard ───────────────────────────────────────────

    @PostMapping("/add")
    public escrow add(@RequestBody escrow e) {
        // Associer automatiquement l'entreprise connectée
        Enterprise ent = getCurrentEnterprise();
        if (ent != null) e.setEnterpriseId(ent.getId());
        return service.addEscrow(e);
    }

    /** Tous les escrows (admin) */
    @GetMapping("/all")
    public List<escrow> getAll() {
        return service.retrieveAllEscrow();
    }

    /**
     * 🏢 Escrows de l'entreprise connectée seulement
     * GET /api/escrow/my
     */
    @GetMapping("/my")
    public ResponseEntity<List<escrow>> getMyEscrows() {
        Enterprise ent = getCurrentEnterprise();
        if (ent == null) {
            log.warn("[ESCROW] Entreprise introuvable pour l'utilisateur connecté");
            return ResponseEntity.ok(List.of());
        }
        List<escrow> escrows = escrowRepository.findByEnterpriseId(ent.getId());
        log.info("[ESCROW] {} escrows trouvés pour l'entreprise '{}'", escrows.size(), ent.getCompanyName());
        return ResponseEntity.ok(escrows);
    }

    @PutMapping("/update")
    public escrow update(@RequestBody escrow e) {
        return service.modifyEscrow(e);
    }

    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable Long id) {
        service.removeEscrow(id);
    }

    @GetMapping("/released")
    public List<escrow> getReleased() {
        return service.retrieveAllEscrow()
                .stream()
                .filter(e -> e.getStatus() == EscrowStatus.RELEASED)
                .toList();
    }

    @PostMapping("/release/{id}")
    public escrow release(@PathVariable Long id) {
        return service.releaseEscrow(id);
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
            log.error("[ESCROW] Erreur récupération entreprise : {}", e.getMessage());
            return null;
        }
    }
}
