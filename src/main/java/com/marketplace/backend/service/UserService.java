package com.marketplace.backend.service;

import com.marketplace.backend.dto.AdminUserDto;
import com.marketplace.backend.dto.UserCreateRequest;
import com.marketplace.backend.dto.UserStatusRequest;
import com.marketplace.backend.dto.UserUpdateRequest;
import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.Transporter;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.entity.enums.UserAccountStatus;
import com.marketplace.backend.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional(readOnly = true)
  public List<AdminUserDto> findAllForAdmin() {
    return userRepository.findAllWithProfiles().stream().map(AdminUserDto::from).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<AdminUserDto> listNonAdminUsers() {
    return userRepository.findAllWithProfiles().stream()
        .filter(u -> u.getRole() != Role.ROLE_ADMIN)
        .map(AdminUserDto::from)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public long countNonAdminUsers() {
    return userRepository.findAll().stream().filter(u -> u.getRole() != Role.ROLE_ADMIN).count();
  }

  @Transactional(readOnly = true)
  public AdminUserDto getForAdmin(Long id) {
    User u =
        userRepository
            .findByIdWithProfiles(id)
            .orElseThrow(() -> new IllegalArgumentException("Not found"));
    return AdminUserDto.from(u);
  }

  @Transactional
  public AdminUserDto create(UserCreateRequest req) {
    if (userRepository.existsByEmail(req.getEmail())) {
      throw new IllegalArgumentException("Email already registered");
    }
    Role role =
        "transporter".equalsIgnoreCase(req.getRole())
            ? Role.ROLE_TRANSPORTER
            : Role.ROLE_ENTERPRISE;
    User user =
        User.builder()
            .email(req.getEmail())
            .password(passwordEncoder.encode(req.getPassword()))
            .fullName(req.getName())
            .role(role)
            .enabled(true)
            .accountStatus(UserAccountStatus.active)
            .phone(req.getPhone())
            .verified(false)
            .build();
    if (role == Role.ROLE_ENTERPRISE) {
      Enterprise ent =
          Enterprise.builder()
              .user(user)
              .companyName(req.getCompanyName())
              .sector(req.getSector())
              .taxId(req.getTaxId())
              .listingsCount(0)
              .ordersCount(0)
              .revenue("0")
              .build();
      user.setEnterprise(ent);
    } else {
      Transporter tra =
          Transporter.builder()
              .user(user)
              .companyName(req.getCompanyName())
              .sector(req.getSector())
              .taxId(req.getTaxId())
              .listingsCount(0)
              .ordersCount(0)
              .revenue("0")
              .build();
      user.setTransporter(tra);
    }
    userRepository.save(user);
    User fresh =
        userRepository.findByIdWithProfiles(user.getId()).orElseThrow();
    return AdminUserDto.from(fresh);
  }

  @Transactional
  public AdminUserDto update(Long userId, UserUpdateRequest req) {
    User u = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Not found"));
    if (u.getRole() == Role.ROLE_ADMIN && req.getAccountStatus() != null) {
      throw new IllegalArgumentException("Cannot change admin account this way");
    }
    if (req.getFullName() != null) {
      u.setFullName(req.getFullName());
    }
    if (req.getPhone() != null) {
      u.setPhone(req.getPhone());
    }
    if (req.getCity() != null) {
      u.setCity(req.getCity());
    }
    if (req.getVerified() != null) {
      u.setVerified(req.getVerified());
    }
    if (req.getPassword() != null && !req.getPassword().isBlank()) {
      u.setPassword(passwordEncoder.encode(req.getPassword()));
    }
    if (req.getAccountStatus() != null && u.getRole() != Role.ROLE_ADMIN) {
      UserAccountStatus st =
          switch (req.getAccountStatus()) {
            case "active" -> UserAccountStatus.active;
            case "pending" -> UserAccountStatus.pending;
            case "suspended" -> UserAccountStatus.suspended;
            default -> throw new IllegalArgumentException("Invalid status");
          };
      u.setAccountStatus(st);
    }
    userRepository.save(u);
    User fresh =
        userRepository.findByIdWithProfiles(userId).orElseThrow(() -> new IllegalArgumentException("Not found"));
    return AdminUserDto.from(fresh);
  }

  @Transactional
  public AdminUserDto updateStatus(Long userId, UserStatusRequest req) {
    User u = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Not found"));
    if (u.getRole() == Role.ROLE_ADMIN) {
      throw new IllegalArgumentException("Cannot modify admin");
    }
    UserAccountStatus st =
        switch (req.getStatus()) {
          case "active" -> UserAccountStatus.active;
          case "pending" -> UserAccountStatus.pending;
          case "suspended" -> UserAccountStatus.suspended;
          default -> throw new IllegalArgumentException("Invalid status");
        };
    u.setAccountStatus(st);
    if ("active".equals(req.getStatus())) {
      u.setVerified(true);
    }
    userRepository.save(u);
    User fresh =
        userRepository
            .findByIdWithProfiles(userId)
            .orElseThrow(() -> new IllegalArgumentException("Not found"));
    return AdminUserDto.from(fresh);
  }

  @Transactional
  public void delete(Long userId) {
    User u = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Not found"));
    if (u.getRole() == Role.ROLE_ADMIN) {
      throw new IllegalArgumentException("Cannot delete admin");
    }
    userRepository.delete(u);
  }
}
