package com.marketplace.backend.config;

import com.marketplace.backend.entity.Delivery;
import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.ExchangeRequest;
import com.marketplace.backend.entity.Listing;
import com.marketplace.backend.entity.PlatformEvent;
import com.marketplace.backend.entity.Reservation;
import com.marketplace.backend.entity.SolidarityAssociation;
import com.marketplace.backend.entity.StockItem;
import com.marketplace.backend.entity.Transporter;
import com.marketplace.backend.entity.TransportOffer;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.WalletTransaction;
import com.marketplace.backend.entity.enums.DeliveryStatus;
import com.marketplace.backend.entity.enums.EventStatus;
import com.marketplace.backend.entity.enums.ExchangeRequestStatus;
import com.marketplace.backend.entity.enums.ListingStatus;
import com.marketplace.backend.entity.enums.ReservationStatus;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.entity.enums.StockItemStatus;
import com.marketplace.backend.entity.enums.TransportOfferStatus;
import com.marketplace.backend.entity.enums.UserAccountStatus;
import com.marketplace.backend.entity.enums.WalletTransactionStatus;
import com.marketplace.backend.repository.DeliveryRepository;
import com.marketplace.backend.repository.EnterpriseRepository;
import com.marketplace.backend.repository.ExchangeRequestRepository;
import com.marketplace.backend.repository.ListingRepository;
import com.marketplace.backend.repository.PlatformEventRepository;
import com.marketplace.backend.repository.ReservationRepository;
import com.marketplace.backend.repository.SolidarityAssociationRepository;
import com.marketplace.backend.repository.StockItemRepository;
import com.marketplace.backend.repository.TransporterRepository;
import com.marketplace.backend.repository.TransportOfferRepository;
import com.marketplace.backend.repository.UserRepository;
import com.marketplace.backend.repository.WalletTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
  private final TransporterRepository transporterRepository;
  private final ListingRepository listingRepository;
  private final StockItemRepository stockItemRepository;
  private final PlatformEventRepository platformEventRepository;
  private final ReservationRepository reservationRepository;
  private final SolidarityAssociationRepository solidarityAssociationRepository;
  private final WalletTransactionRepository walletTransactionRepository;
  private final DeliveryRepository deliveryRepository;
  private final ExchangeRequestRepository exchangeRequestRepository;
  private final TransportOfferRepository transportOfferRepository;
  private final PasswordEncoder passwordEncoder;

  @Bean
  CommandLineRunner seedAdminAndDemo() {
    return args -> {
      if (userRepository.findByEmail("admin@marketplace.com").isEmpty()) {
        User admin = User.builder()
            .email("admin@marketplace.com")
            .password(passwordEncoder.encode("admin123"))
            .fullName("Admin Principal")
            .role(Role.ROLE_ADMIN)
            .enabled(true)
            .accountStatus(UserAccountStatus.active)
            .verified(true)
            .phone("")
            .city("Tunis")
            .build();
        userRepository.save(admin);
      }

      if (userRepository.findByEmail("slim@entreprise.tn").isEmpty()) {
        User entUser = User.builder()
            .email("slim@entreprise.tn")
            .password(passwordEncoder.encode("demo123"))
            .fullName("Slim Ben Ali")
            .role(Role.ROLE_ENTERPRISE)
            .enabled(true)
            .accountStatus(UserAccountStatus.active)
            .verified(true)
            .phone("+216 71 234 567")
            .city("Tunis")
            .build();
        Enterprise ent = Enterprise.builder()
            .user(entUser)
            .companyName("Industrie Slim SARL")
            .sector("Metallurgy")
            .taxId("TN123")
            .listingsCount(0)
            .ordersCount(12)
            .revenue("14200")
            .build();
        entUser.setEnterprise(ent);
        userRepository.save(entUser);

        Enterprise e = enterpriseRepository.findByUserId(entUser.getId()).orElseThrow();
        Listing l1 = Listing.builder()
            .enterprise(e)
            .title("Aluminum Scrap 2T")
            .category("Metal")
            .price(new BigDecimal("1200"))
            .quantityLabel("2,000 kg")
            .status(ListingStatus.active)
            .aiInsight("High demand — act fast")
            .views(48)
            .enquiries(5)
            .postedLabel("Mar 1")
            .build();
        listingRepository.save(l1);
        e.setListingsCount(1);
        enterpriseRepository.save(e);

        stockItemRepository.save(
            StockItem.builder()
                .enterprise(e)
                .name("Aluminum Scrap")
                .category("Metal")
                .quantity(2000)
                .unit("kg")
                .conditionLabel("Good")
                .status(StockItemStatus.listed)
                .aiInsight("Shortage in 2 weeks")
                .build());

        reservationRepository.save(
            Reservation.builder()
                .typeLabel("Machine")
                .item("CNC Milling 3-axis")
                .companyName("Industrie Slim")
                .fromDate(LocalDate.of(2025, 3, 20))
                .toDate(LocalDate.of(2025, 3, 22))
                .price(new BigDecimal("450"))
                .status(ReservationStatus.confirmed)
                .enterprise(e)
                .build());

        User slim = userRepository.findByEmail("slim@entreprise.tn").orElseThrow();
        walletTransactionRepository.save(
            WalletTransaction.builder()
                .user(slim)
                .label("Aluminum Scrap sale")
                .typeLabel("Escrow Release")
                .amount(new BigDecimal("1200"))
                .positiveFlag(true)
                .status(WalletTransactionStatus.completed)
                .valueDate(LocalDate.of(2025, 3, 14))
                .build());
      }

      if (userRepository.findByEmail("karim@transport.tn").isEmpty()) {
        User trUser = User.builder()
            .email("karim@transport.tn")
            .password(passwordEncoder.encode("demo123"))
            .fullName("Karim Transport")
            .role(Role.ROLE_TRANSPORTER)
            .enabled(true)
            .accountStatus(UserAccountStatus.active)
            .verified(true)
            .phone("+216 72 345 678")
            .city("Sfax")
            .build();
        Transporter tr = Transporter.builder()
            .user(trUser)
            .companyName("Karim Logistics")
            .sector("Transport & Logistics")
            .taxId("TN456")
            .listingsCount(0)
            .ordersCount(34)
            .revenue("8750")
            .build();
        trUser.setTransporter(tr);
        userRepository.save(trUser);

        Transporter t = transporterRepository.findByUserId(trUser.getId()).orElseThrow();
        transportOfferRepository.save(
            TransportOffer.builder()
                .transporter(t)
                .fromLocation("Gabès")
                .toLocation("Tunis")
                .cargoDescription("Steel Offcuts 2T")
                .weightLabel("2,000kg")
                .proposedEarn(new BigDecimal("420"))
                .status(TransportOfferStatus.open)
                .build());
      }

      Enterprise entForEx = enterpriseRepository.findAll().stream().findFirst().orElse(null);
      if (entForEx != null && exchangeRequestRepository.count() == 0) {
        exchangeRequestRepository.save(
            ExchangeRequest.builder()
                .recipientEnterprise(entForEx)
                .fromCompanyName("Textile Mona SA")
                .fromAvatar("TM")
                .item("CNC Milling Machine")
                .typeLabel("Machine Rental")
                .fromDate(LocalDate.of(2025, 3, 25))
                .toDate(LocalDate.of(2025, 3, 27))
                .durationLabel("2 days")
                .price(new BigDecimal("400"))
                .message("We need your CNC machine for a short production run.")
                .status(ExchangeRequestStatus.pending)
                .receivedLabel("10 min ago")
                .urgent(true)
                .build());
      }

      if (platformEventRepository.count() == 0) {
        platformEventRepository.save(
            PlatformEvent.builder()
                .title("B2B Industrial Fair 2025")
                .eventDate(LocalDate.of(2025, 4, 10))
                .location("Tunis")
                .participants(42)
                .status(EventStatus.upcoming)
                .typeLabel("Conference")
                .build());
      }

      if (solidarityAssociationRepository.count() == 0) {
        User admin = userRepository.findByEmail("admin@marketplace.com").orElse(null);
        Long adminId = admin != null ? admin.getId() : null;

        solidarityAssociationRepository.saveAll(List.of(
            SolidarityAssociation.builder()
                .name("Tunis Eco-Challenge")
                .mission("Organizing massive beach cleanups and waste sorting across the coastal areas of Tunis.")
                .members(1250)
                .donations(4500.0)
                .goalAmount(10000.0)
                .statusLabel("active")
                .userId(adminId)
                .build(),
            SolidarityAssociation.builder()
                .name("Green Horizon Sfax")
                .mission("Combatting desertification and industrial pollution through urban reforestation projects.")
                .members(840)
                .donations(2100.0)
                .goalAmount(15000.0)
                .statusLabel("active")
                .userId(adminId)
                .build(),
            SolidarityAssociation.builder()
                .name("Sousse Circular Hub")
                .mission("Transforming textile waste from the Sahel region into high-quality recycled accessories.")
                .members(420)
                .donations(1250.0)
                .goalAmount(5000.0)
                .statusLabel("active")
                .userId(adminId)
                .build(),
            SolidarityAssociation.builder()
                .name("Bizerte Marine Life")
                .mission("Monitoring and protecting the biodiversity of the Bizerte lagoon and nearby marine parks.")
                .members(600)
                .donations(3800.0)
                .goalAmount(8000.0)
                .statusLabel("active")
                .userId(adminId)
                .build(),
            SolidarityAssociation.builder()
                .name("Djerba Sun Source")
                .mission("Assisting local hospitality businesses in transitioning to 100% renewable energy.")
                .members(310)
                .donations(8900.0)
                .goalAmount(25000.0)
                .statusLabel("pending")
                .userId(adminId)
                .build()));
      }

      if (walletTransactionRepository.count() <= 1 && userRepository.count() > 1) {
        User any = userRepository.findAll().stream()
            .filter(u -> u.getRole() == Role.ROLE_ENTERPRISE)
            .findFirst()
            .orElse(null);
        if (any != null) {
          walletTransactionRepository.save(
              WalletTransaction.builder()
                  .user(any)
                  .label("Platform commission")
                  .typeLabel("Fee")
                  .amount(new BigDecimal("-54"))
                  .positiveFlag(false)
                  .status(WalletTransactionStatus.completed)
                  .valueDate(LocalDate.of(2025, 3, 14))
                  .build());
        }
      }

      Enterprise ee = enterpriseRepository.findAll().stream().findFirst().orElse(null);
      Transporter tt = transporterRepository.findAll().stream().findFirst().orElse(null);
      if (ee != null && tt != null && deliveryRepository.count() == 0) {
        deliveryRepository.save(
            Delivery.builder()
                .enterprise(ee)
                .transporter(tt)
                .productLabel("Aluminum Scrap 2T")
                .fromLocation("Tunis")
                .toLocation("Sfax")
                .clientName("Industrie Slim")
                .status(DeliveryStatus.delivered)
                .co2Label("12kg")
                .dateLabel("Mar 10")
                .amount(new BigDecimal("1200"))
                .earnAmount(new BigDecimal("190"))
                .build());
      }
    };
  }
}
