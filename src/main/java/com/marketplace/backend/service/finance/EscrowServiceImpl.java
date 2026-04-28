package com.marketplace.backend.service.finance;

import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.finance.EscrowStatus;
import com.marketplace.backend.entity.finance.escrow;
import com.marketplace.backend.event.EscrowReleasedEvent;
import com.marketplace.backend.repository.UserRepository;
import com.marketplace.backend.repository.finance.EscrowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EscrowServiceImpl implements IEscrowService {

    private final EscrowRepository escrowRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public List<escrow> retrieveAllEscrow() {
        return escrowRepository.findAll();
    }

    @Override
    public escrow retrieveEscrow(Long id) {
        return escrowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Escrow introuvable : " + id));
    }

    @Override
    public escrow addEscrow(escrow e) {
        if (e.getStatus() == EscrowStatus.RELEASED) {
            e.setReleaseDate(LocalDate.now().toString());
        }
        return escrowRepository.save(e);
    }

    @Override
    public void removeEscrow(Long id) {
        escrowRepository.deleteById(id);
    }

    @Override
    public escrow modifyEscrow(escrow e) {
        if (e.getStatus() == EscrowStatus.RELEASED) {
            e.setReleaseDate(LocalDate.now().toString());
        }
        return escrowRepository.save(e);
    }

    /**
     * Libère un escrow (LOCKED → RELEASED) et envoie un email de notification
     * à l'utilisateur actuellement connecté (récupéré via le token JWT).
     */
    @Override
    @Transactional
    public escrow releaseEscrow(Long id) {

        // 1. Mettre à jour le statut de l'escrow
        escrow e = escrowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Escrow introuvable : " + id));
        e.setStatus(EscrowStatus.RELEASED);
        String releaseDate = LocalDate.now().toString();
        e.setReleaseDate(releaseDate);
        escrow saved = escrowRepository.save(e);

        // 2. Récupérer l'email de l'utilisateur connecté via le contexte Spring Security
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            String connectedEmail = authentication.getName(); // = email du token JWT

            userRepository.findByEmail(connectedEmail).ifPresentOrElse(
                connectedUser -> {
                    EscrowReleasedEvent event = new EscrowReleasedEvent(
                            saved.getIdescrow(),
                            saved.getProject(),
                            saved.getAmount(),
                            releaseDate,
                            connectedUser.getEmail(),
                            connectedUser.getFullName()
                    );
                    eventPublisher.publishEvent(event);
                    log.info("[ESCROW] 📢 Email envoyé à l'utilisateur connecté : {}",
                            connectedUser.getEmail());
                },
                () -> log.warn("[ESCROW] ⚠️ Utilisateur '{}' introuvable en base", connectedEmail)
            );
        } else {
            log.warn("[ESCROW] ⚠️ Aucun utilisateur authentifié dans le contexte — email non envoyé");
        }

        return saved;
    }
}
