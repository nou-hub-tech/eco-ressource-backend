package com.marketplace.backend.service;

import com.marketplace.backend.dto.WalletTransactionRequest;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.WalletTransaction;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.entity.enums.WalletTransactionStatus;
import com.marketplace.backend.repository.UserRepository;
import com.marketplace.backend.repository.WalletTransactionRepository;
import com.marketplace.backend.security.SecurityUserHelper;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletTransactionService {

  private final WalletTransactionRepository walletTransactionRepository;
  private final UserRepository userRepository;
  private final SecurityUserHelper securityUserHelper;

  private void assertCanAccessUser(Authentication auth, Long userId) {
    User u = securityUserHelper.requireUser(auth);
    if (u.getRole() == Role.ROLE_ADMIN) {
      return;
    }
    if (!u.getId().equals(userId)) {
      throw new IllegalArgumentException("Forbidden");
    }
  }

  @Transactional(readOnly = true)
  public List<WalletTransaction> findAll(Authentication auth) {
    User u = securityUserHelper.requireUser(auth);
    if (u.getRole() == Role.ROLE_ADMIN) {
      return walletTransactionRepository.findAll();
    }
    return walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(u.getId());
  }

  @Transactional(readOnly = true)
  public WalletTransaction getById(Long id, Authentication auth) {
    WalletTransaction t =
        walletTransactionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    assertCanAccessUser(auth, t.getUser().getId());
    return t;
  }

  @Transactional
  public WalletTransaction create(Authentication auth, WalletTransactionRequest req) {
    User actor = securityUserHelper.requireUser(auth);
    if (actor.getRole() != Role.ROLE_ADMIN && !actor.getId().equals(req.getUserId())) {
      throw new IllegalArgumentException("Forbidden");
    }
    User owner =
        userRepository.findById(req.getUserId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
    WalletTransaction t =
        WalletTransaction.builder()
            .user(owner)
            .label(req.getLabel())
            .typeLabel(req.getTypeLabel())
            .amount(req.getAmount())
            .positiveFlag(req.getPositiveFlag())
            .fromParty(req.getFromParty())
            .toParty(req.getToParty())
            .status(
                WalletTransactionStatus.valueOf(req.getStatus().toLowerCase(Locale.ROOT)))
            .valueDate(req.getValueDate())
            .build();
    return walletTransactionRepository.save(t);
  }

  @Transactional
  public WalletTransaction update(Long id, Authentication auth, WalletTransactionRequest req) {
    WalletTransaction t =
        walletTransactionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    User actor = securityUserHelper.requireUser(auth);
    if (actor.getRole() != Role.ROLE_ADMIN && !actor.getId().equals(t.getUser().getId())) {
      throw new IllegalArgumentException("Forbidden");
    }
    if (actor.getRole() != Role.ROLE_ADMIN && !actor.getId().equals(req.getUserId())) {
      throw new IllegalArgumentException("Forbidden");
    }
    User owner =
        userRepository.findById(req.getUserId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
    t.setUser(owner);
    t.setLabel(req.getLabel());
    t.setTypeLabel(req.getTypeLabel());
    t.setAmount(req.getAmount());
    t.setPositiveFlag(req.getPositiveFlag());
    t.setFromParty(req.getFromParty());
    t.setToParty(req.getToParty());
    t.setStatus(WalletTransactionStatus.valueOf(req.getStatus().toLowerCase(Locale.ROOT)));
    t.setValueDate(req.getValueDate());
    return walletTransactionRepository.save(t);
  }

  @Transactional
  public void delete(Long id, Authentication auth) {
    WalletTransaction t =
        walletTransactionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    User actor = securityUserHelper.requireUser(auth);
    if (actor.getRole() != Role.ROLE_ADMIN && !actor.getId().equals(t.getUser().getId())) {
      throw new IllegalArgumentException("Forbidden");
    }
    walletTransactionRepository.delete(t);
  }
}
