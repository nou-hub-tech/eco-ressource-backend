package com.marketplace.backend.config;

import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.entity.enums.UserAccountStatus;
import com.marketplace.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DatabaseSeed {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Bean
  CommandLineRunner seedUsers() {
    return args -> {

      // ========================
      // ADMIN USER
      // ========================
      if (userRepository.findByEmail("admin@marketplace.com").isEmpty()) {
        userRepository.save(
          User.builder()
            .email("admin@marketplace.com")
            .password(passwordEncoder.encode("admin123"))
            .fullName("Admin")
            .role(Role.ROLE_ADMIN)
            .enabled(true)
            .accountStatus(UserAccountStatus.active)
            .verified(true)
            .build()
        );
      }

      // ========================
      // ENTERPRISE USER
      // ========================
      if (userRepository.findByEmail("slim@entreprise.tn").isEmpty()) {
        userRepository.save(
          User.builder()
            .email("slim@entreprise.tn")
            .password(passwordEncoder.encode("slim123"))
            .fullName("Slim Enterprise")
            .role(Role.ROLE_ENTERPRISE)
            .enabled(true)
            .accountStatus(UserAccountStatus.active)
            .verified(true)
            .build()
        );
      }

      // ========================
      // TRANSPORTER USER
      // ========================
      if (userRepository.findByEmail("karim@transport.tn").isEmpty()) {
        userRepository.save(
          User.builder()
            .email("karim@transport.tn")
            .password(passwordEncoder.encode("karim123"))
            .fullName("Karim Transport")
            .role(Role.ROLE_TRANSPORTER)
            .enabled(true)
            .accountStatus(UserAccountStatus.active)
            .verified(true)
            .build()
        );
      }
    };
  }
}
