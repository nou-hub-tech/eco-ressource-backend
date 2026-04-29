package com.marketplace.backend.service;

import com.marketplace.backend.dto.JwtResponse;
import com.marketplace.backend.dto.LoginRequest;
import com.marketplace.backend.dto.RegisterRequest;
import com.marketplace.backend.dto.UserResponseDto;
import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.Transporter;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.entity.enums.UserAccountStatus;
import com.marketplace.backend.repository.UserRepository;
import com.marketplace.backend.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtUtils jwtUtils;

  @Transactional(readOnly = true)
  public UserResponseDto profileForEmail(String email) {
    User user =
        userRepository
            .findByEmailWithProfiles(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    return UserResponseDto.from(user);
  }

  @Transactional(readOnly = true)
  public JwtResponse login(LoginRequest req) {
    Authentication auth =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
    User user =
        userRepository
            .findByEmailWithProfiles(auth.getName())
            .orElseThrow(() -> new IllegalStateException("User missing after auth"));
    String token = jwtUtils.generateToken(user.getEmail(), user.getRole());
    UserResponseDto dto = UserResponseDto.from(user);
    return JwtResponse.builder()
        .token(token)
        .type("Bearer")
        .role(user.getRole().name())
        .user(dto)
        .build();
  }

  @Transactional
  public JwtResponse register(RegisterRequest req) {
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
      Transporter tr =
          Transporter.builder()
              .user(user)
              .companyName(req.getCompanyName())
              .sector(req.getSector())
              .taxId(req.getTaxId())
              .listingsCount(0)
              .ordersCount(0)
              .revenue("0")
              .build();
      user.setTransporter(tr);
    }
    userRepository.save(user);
    User reloaded =
        userRepository.findByEmailWithProfiles(req.getEmail()).orElseThrow();
    String token = jwtUtils.generateToken(reloaded.getEmail(), reloaded.getRole());
    return JwtResponse.builder()
        .token(token)
        .type("Bearer")
        .role(reloaded.getRole().name())
        .user(UserResponseDto.from(reloaded))
        .build();
  }
}
