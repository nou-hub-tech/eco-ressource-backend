package com.marketplace.backend.controller;

import com.marketplace.backend.dto.JwtResponse;
import com.marketplace.backend.dto.LoginRequest;
import com.marketplace.backend.dto.RegisterRequest;
import com.marketplace.backend.dto.UserResponseDto;
import com.marketplace.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  /** Profil courant (companyId inclus) pour rafraîchir la session sans se reconnecter. */
  @GetMapping("/me")
  public ResponseEntity<UserResponseDto> me(
      @AuthenticationPrincipal UserDetails principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    try {
      return ResponseEntity.ok(authService.profileForEmail(principal.getUsername()));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping("/login")
  public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest req) {
    try {
      return ResponseEntity.ok(authService.login(req));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
  }

  @PostMapping("/register")
  public ResponseEntity<JwtResponse> register(@Valid @RequestBody RegisterRequest req) {
    try {
      return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }
}
