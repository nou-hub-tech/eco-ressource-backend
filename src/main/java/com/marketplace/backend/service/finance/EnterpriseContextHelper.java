package com.marketplace.backend.service.finance;

import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.repository.EnterpriseRepository;
import com.marketplace.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * ══════════════════════════════════════════════════════════════
 *  🏢 Helper partagé — Entreprise de l'utilisateur connecté
 * ══════════════════════════════════════════════════════════════
 *
 * Récupère le companyName et l'Enterprise de l'utilisateur
 * authentifié via Spring Security. Utilisé par les services IA
 * pour filtrer les données par entreprise.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnterpriseContextHelper {

    private final UserRepository userRepository;
    private final EnterpriseRepository enterpriseRepository;

    /**
     * Retourne le companyName de l'enterprise connectée,
     * ou null si non authentifié / non trouvé.
     */
    public String getCurrentCompanyName() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return null;
            }
            User user = userRepository.findByEmailWithProfiles(auth.getName()).orElse(null);
            if (user == null) return null;

            Enterprise enterprise = enterpriseRepository.findByUserId(user.getId()).orElse(null);
            if (enterprise == null) return null;

            return enterprise.getCompanyName();
        } catch (Exception e) {
            log.error("[ENTERPRISE-CONTEXT] Erreur : {}", e.getMessage());
            return null;
        }
    }

    /**
     * Retourne l'Enterprise de l'utilisateur connecté, ou null.
     */
    public Enterprise getCurrentEnterprise() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return null;
            }
            User user = userRepository.findByEmailWithProfiles(auth.getName()).orElse(null);
            if (user == null) return null;
            return enterpriseRepository.findByUserId(user.getId()).orElse(null);
        } catch (Exception e) {
            log.error("[ENTERPRISE-CONTEXT] Erreur : {}", e.getMessage());
            return null;
        }
    }
}
