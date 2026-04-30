package com.marketplace.backend.config;

import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.ReservationSlot;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.entity.enums.SlotStatus;
import com.marketplace.backend.entity.enums.UserAccountStatus;
import com.marketplace.backend.repository.EnterpriseRepository;
import com.marketplace.backend.repository.ReservationSlotRepository;
import com.marketplace.backend.repository.UserRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DatabaseSeed {

  private final UserRepository userRepository;
  private final EnterpriseRepository enterpriseRepository;
  private final ReservationSlotRepository reservationSlotRepository;
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
      User slimUser = userRepository.findByEmailWithProfiles("slim@entreprise.tn")
        .orElseGet(() ->
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
          )
        );

      ensureEnterpriseProfile(slimUser, "Slim Recycling");

      User providerUser = userRepository.findByEmailWithProfiles("nour@entreprise.tn")
        .orElseGet(() ->
          userRepository.save(
            User.builder()
              .email("nour@entreprise.tn")
              .password(passwordEncoder.encode("nour123"))
              .fullName("Nour Provider")
              .role(Role.ROLE_ENTERPRISE)
              .enabled(true)
              .accountStatus(UserAccountStatus.active)
              .verified(true)
              .build()
          )
        );

      Enterprise providerEnterprise = ensureEnterpriseProfile(providerUser, "Nour Industries");
      ensureProviderSlot(providerEnterprise);

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

  private Enterprise ensureEnterpriseProfile(User user, String companyName) {
    User hydrated = userRepository.findByIdWithProfiles(user.getId()).orElse(user);
    if (hydrated.getEnterprise() != null) {
      return hydrated.getEnterprise();
    }

    Enterprise enterprise = Enterprise.builder()
      .user(hydrated)
      .companyName(companyName)
      .sector("Circular Economy")
      .taxId("AUTO-" + hydrated.getId())
      .listingsCount(0)
      .ordersCount(0)
      .revenue("0")
      .build();
    hydrated.setEnterprise(enterprise);
    return userRepository.save(hydrated).getEnterprise();
  }

  private void ensureProviderSlot(Enterprise enterprise) {
    if (enterprise == null || enterprise.getId() == null) {
      return;
    }

    if (!reservationSlotRepository.findActiveByEnterpriseId(enterprise.getId()).isEmpty()) {
      return;
    }

    reservationSlotRepository.save(
      ReservationSlot.builder()
        .machine("Provider Machine A")
        .date(LocalDate.now().plusDays(1))
        .startHour(9)
        .endHour(12)
        .status(SlotStatus.open)
        .solar(true)
        .discountPct(10)
        .enterprise(enterprise)
        .deleted(false)
        .build()
    );
  }
}
