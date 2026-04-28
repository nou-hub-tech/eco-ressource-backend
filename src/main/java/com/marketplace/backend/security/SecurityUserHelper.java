package com.marketplace.backend.security;

import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.Transporter;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.repository.EnterpriseRepository;
import com.marketplace.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUserHelper {

  private final UserRepository userRepository;
  private final EnterpriseRepository enterpriseRepository;

  public User requireUser(Authentication auth) {
    return userRepository
        .findByEmailWithProfiles(auth.getName())
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
  }

  public Enterprise requireEnterprise(Authentication auth) {
    User u = requireUser(auth);
    if (u.getRole() == Role.ROLE_ADMIN) {
      return enterpriseRepository
          .findAll()
          .stream()
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("No enterprise in system"));
    }
    if (u.getEnterprise() == null) {
      throw new IllegalArgumentException("Enterprise profile required");
    }
    return u.getEnterprise();
  }

  public Enterprise requireEnterpriseStrict(Authentication auth) {
    User u = requireUser(auth);
    if (u.getEnterprise() == null) {
      throw new IllegalArgumentException("Enterprise profile required");
    }
    return u.getEnterprise();
  }

  public Transporter requireTransporter(Authentication auth) {
    User u = requireUser(auth);
    if (u.getTransporter() == null) {
      throw new IllegalArgumentException("Transporter profile required");
    }
    return u.getTransporter();
  }
}
