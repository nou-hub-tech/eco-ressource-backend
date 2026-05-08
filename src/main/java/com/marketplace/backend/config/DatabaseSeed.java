package com.marketplace.backend.config;

import com.marketplace.backend.entity.Delivery;
import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.ExchangeRequest;
import com.marketplace.backend.entity.Listing;
import com.marketplace.backend.entity.PlatformEvent;
import com.marketplace.backend.entity.Product;
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
import com.marketplace.backend.entity.workspace.WorkspaceEnums.NotificationChannel;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.OrderStatus;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.PaymentStatus;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.ReservationCategory;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.ReservationRole;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.SlotPortfolio;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.SlotStatus;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.SlotType;
import com.marketplace.backend.entity.workspace.WorkspacePayloads;
import com.marketplace.backend.entity.workspace.WorkspaceReservation;
import com.marketplace.backend.entity.workspace.WorkspaceReservationSlot;
import com.marketplace.backend.entity.workspace.WorkspaceOrder;
import com.marketplace.backend.repository.DeliveryRepository;
import com.marketplace.backend.repository.EnterpriseRepository;
import com.marketplace.backend.repository.ExchangeRequestRepository;
import com.marketplace.backend.repository.ListingRepository;
import com.marketplace.backend.repository.PlatformEventRepository;
import com.marketplace.backend.repository.ProductRepository;
import com.marketplace.backend.repository.ReservationRepository;
import com.marketplace.backend.repository.SolidarityAssociationRepository;
import com.marketplace.backend.repository.StockItemRepository;
import com.marketplace.backend.repository.TransporterRepository;
import com.marketplace.backend.repository.TransportOfferRepository;
import com.marketplace.backend.repository.UserRepository;
import com.marketplace.backend.repository.WalletTransactionRepository;
import com.marketplace.backend.repository.workspace.WorkspaceOrderRepository;
import com.marketplace.backend.repository.workspace.WorkspaceReservationRepository;
import com.marketplace.backend.repository.workspace.WorkspaceReservationSlotRepository;
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
  private final ProductRepository productRepository;
  private final StockItemRepository stockItemRepository;
  private final PlatformEventRepository platformEventRepository;
  private final ReservationRepository reservationRepository;
  private final SolidarityAssociationRepository solidarityAssociationRepository;
  private final WalletTransactionRepository walletTransactionRepository;
  private final DeliveryRepository deliveryRepository;
  private final ExchangeRequestRepository exchangeRequestRepository;
  private final TransportOfferRepository transportOfferRepository;
  private final WorkspaceReservationSlotRepository workspaceReservationSlotRepository;
  private final WorkspaceReservationRepository workspaceReservationRepository;
  private final WorkspaceOrderRepository workspaceOrderRepository;
  private final PasswordEncoder passwordEncoder;

  @Bean
  CommandLineRunner seedAdminAndDemo() {
    return args -> {
      if (userRepository.findByEmail("admin@marketplace.com").isEmpty()) {
        User admin =
            User.builder()
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
        User entUser =
            User.builder()
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
        Enterprise ent =
            Enterprise.builder()
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
        Listing l1 =
            Listing.builder()
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

        Product product =
            productRepository.save(
                Product.builder()
                    .name("Aluminum Scrap")
                    .category("Metal")
                    .description("Industrial aluminum scrap for recycling")
                    .materialType("Aluminum")
                    .recyclable(true)
                    .image("aluminum-scrap.jpg")
                    .enterprise(e)
                    .build());

        stockItemRepository.save(
            StockItem.builder()
                .companyId(e.getId())
                .enterprise(e)
                .condition("Good")
                .location("Tunis Warehouse")
                .quantity(2000)
                .status("up")
                .unit("kg")
                .product(product)
                .unitPrice(0.6)
                .deleted(false)
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
        User trUser =
            User.builder()
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
        Transporter tr =
            Transporter.builder()
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

      Enterprise entForEx =
          enterpriseRepository.findAll().stream().findFirst().orElse(null);
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
                .latitude(36.8065)
                .longitude(10.1815)
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
        User any =
            userRepository.findAll().stream()
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

      seedReservationWorkspace();
    };
  }

  private void seedReservationWorkspace() {
    Enterprise enterprise = enterpriseRepository.findAll().stream().findFirst().orElse(null);
    if (enterprise == null
        || !workspaceReservationSlotRepository.findByEnterpriseIdOrderByNameAsc(enterprise.getId()).isEmpty()) {
      return;
    }

    WorkspaceReservationSlot radesDock =
        WorkspaceReservationSlot.builder()
            .id("SLT-201")
            .name("Rades Dock 2")
            .zone("Rades Port Zone")
            .city("Ben Arous")
            .ownerCompany(enterprise.getCompanyName())
            .portfolio(SlotPortfolio.owned)
            .type(SlotType.Dock)
            .coordinates(List.of(36.7687, 10.2759))
            .capacity(16)
            .occupied(12)
            .status(SlotStatus.peak)
            .equipment(List.of("RFID gate", "Cold chain", "Truck ramp"))
            .heatmap(workspaceHeatmap(SlotStatus.peak))
            .forecast(workspaceForecast(SlotStatus.peak))
            .enterprise(enterprise)
            .build();

    WorkspaceReservationSlot mghiraWarehouse =
        WorkspaceReservationSlot.builder()
            .id("SLT-202")
            .name("Mghira South Warehouse")
            .zone("Mghira Industrial Zone")
            .city("Ben Arous")
            .ownerCompany(enterprise.getCompanyName())
            .portfolio(SlotPortfolio.owned)
            .type(SlotType.Storage)
            .coordinates(List.of(36.7189, 10.2243))
            .capacity(26)
            .occupied(9)
            .status(SlotStatus.available)
            .equipment(List.of("Smart shelving", "Barcode tunnel", "Dry zone"))
            .heatmap(workspaceHeatmap(SlotStatus.available))
            .forecast(workspaceForecast(SlotStatus.available))
            .enterprise(enterprise)
            .build();

    WorkspaceReservationSlot lacLounge =
        WorkspaceReservationSlot.builder()
            .id("SLT-203")
            .name("Lac 1 Meeting Lounge")
            .zone("Les Berges du Lac")
            .city("Tunis")
            .ownerCompany("Carthage Packaging")
            .portfolio(SlotPortfolio.partner)
            .type(SlotType.Meeting)
            .coordinates(List.of(36.8447, 10.2824))
            .capacity(12)
            .occupied(6)
            .status(SlotStatus.balanced)
            .equipment(List.of("Video conference suite", "Wall display", "Phone booths"))
            .heatmap(workspaceHeatmap(SlotStatus.balanced))
            .forecast(workspaceForecast(SlotStatus.balanced))
            .enterprise(enterprise)
            .build();

    WorkspaceReservationSlot sousseWorkshop =
        WorkspaceReservationSlot.builder()
            .id("SLT-204")
            .name("Sousse Line 3 Workshop")
            .zone("Sousse Industrial Zone")
            .city("Sousse")
            .ownerCompany("Sousse Metal Services")
            .portfolio(SlotPortfolio.partner)
            .type(SlotType.Production)
            .coordinates(List.of(35.8361, 10.6258))
            .capacity(18)
            .occupied(14)
            .status(SlotStatus.maintenance)
            .equipment(List.of("Energy sensors", "Material scanner", "Quality bridge"))
            .heatmap(workspaceHeatmap(SlotStatus.maintenance))
            .forecast(workspaceForecast(SlotStatus.maintenance))
            .enterprise(enterprise)
            .build();

    WorkspaceReservationSlot sfaxHub =
        WorkspaceReservationSlot.builder()
            .id("SLT-205")
            .name("Sfax Poudriere Hub")
            .zone("Route de la Poudriere")
            .city("Sfax")
            .ownerCompany(enterprise.getCompanyName())
            .portfolio(SlotPortfolio.owned)
            .type(SlotType.Production)
            .coordinates(List.of(34.7684, 10.7605))
            .capacity(20)
            .occupied(13)
            .status(SlotStatus.balanced)
            .equipment(List.of("Optical sorting", "Fast weighing", "Export pallet zone"))
            .heatmap(workspaceHeatmap(SlotStatus.balanced))
            .forecast(workspaceForecast(SlotStatus.balanced))
            .enterprise(enterprise)
            .build();

    workspaceReservationSlotRepository.saveAll(
        List.of(radesDock, mghiraWarehouse, lacLounge, sousseWorkshop, sfaxHub));

    WorkspaceReservation reservation401 =
        WorkspaceReservation.builder()
            .id("RES-401")
            .code("RSV-401")
            .title("PET pallet loading")
            .customer("El Amen Recycling")
            .resource("Logistics dock 2")
            .slotId("SLT-201")
            .slotName("Rades Dock 2")
            .role(ReservationRole.provider)
            .city("Ben Arous")
            .category(ReservationCategory.Space)
            .startAt(LocalDate.parse("2026-05-08").atTime(9, 0))
            .endAt(LocalDate.parse("2026-05-08").atTime(12, 0))
            .headcount(8)
            .amount(money("4600"))
            .status(com.marketplace.backend.entity.workspace.WorkspaceEnums.ReservationStatus.risk)
            .notes("The client expects a quick truck turn with exit paperwork and customs validation before noon.")
            .tags(List.of("Export", "Customs", "Priority"))
            .contactName("Hatem Jlassi")
            .contactEmail("hatem.jlassi@elamen.tn")
            .contactPhone("+216 98 112 334")
            .notificationChannel(NotificationChannel.both)
            .enterprise(enterprise)
            .build();

    WorkspaceReservation reservation402 =
        WorkspaceReservation.builder()
            .id("RES-402")
            .code("RSV-402")
            .title("Reusable packaging review")
            .customer("Carthage Packaging")
            .resource("Client review lounge")
            .slotId("SLT-203")
            .slotName("Lac 1 Meeting Lounge")
            .role(ReservationRole.consumer)
            .city("Tunis")
            .category(ReservationCategory.Space)
            .startAt(LocalDate.parse("2026-05-09").atTime(10, 0))
            .endAt(LocalDate.parse("2026-05-09").atTime(12, 30))
            .headcount(6)
            .amount(money("1850"))
            .status(com.marketplace.backend.entity.workspace.WorkspaceEnums.ReservationStatus.confirmed)
            .notes("Planning session for a joint offer around reusable packaging flows.")
            .tags(List.of("Partnership", "Client", "Lake"))
            .contactName("Meriem Kacem")
            .contactEmail("meriem.kacem@carthagepack.tn")
            .contactPhone("+216 55 240 118")
            .notificationChannel(NotificationChannel.email)
            .confirmationChannel(NotificationChannel.email)
            .confirmationDestination("meriem.kacem@carthagepack.tn")
            .confirmationSentAt(LocalDate.parse("2026-05-03").atTime(9, 0))
            .confirmationSummary("Confirmation routed to meriem.kacem@carthagepack.tn.")
            .enterprise(enterprise)
            .build();

    WorkspaceReservation reservation403 =
        WorkspaceReservation.builder()
            .id("RES-403")
            .code("RSV-403")
            .title("Finished leather storage")
            .customer("Maison du Cuir Sahel")
            .resource("Buffer warehouse B")
            .slotId("SLT-202")
            .slotName("Mghira South Warehouse")
            .role(ReservationRole.provider)
            .city("Ben Arous")
            .category(ReservationCategory.Storage)
            .startAt(LocalDate.parse("2026-05-10").atTime(8, 0))
            .endAt(LocalDate.parse("2026-05-10").atTime(16, 0))
            .headcount(4)
            .amount(money("2400"))
            .status(com.marketplace.backend.entity.workspace.WorkspaceEnums.ReservationStatus.confirmed)
            .notes("Short-term storage before transfer toward La Goulette port.")
            .tags(List.of("Storage", "Transit"))
            .contactName("Alaa Siala")
            .contactEmail("alaa.siala@cuirsahel.tn")
            .contactPhone("+216 24 781 225")
            .notificationChannel(NotificationChannel.sms)
            .confirmationChannel(NotificationChannel.sms)
            .confirmationDestination("+216 24 781 225")
            .confirmationSentAt(LocalDate.parse("2026-05-04").atTime(8, 30))
            .confirmationSummary("Confirmation routed to +216 24 781 225.")
            .enterprise(enterprise)
            .build();

    WorkspaceReservation reservation404 =
        WorkspaceReservation.builder()
            .id("RES-404")
            .code("RSV-404")
            .title("Circular steel cutting")
            .customer("Sousse Metal Services")
            .resource("Cutting line 3")
            .slotId("SLT-204")
            .slotName("Sousse Line 3 Workshop")
            .role(ReservationRole.consumer)
            .city("Sousse")
            .category(ReservationCategory.Production)
            .startAt(LocalDate.parse("2026-05-11").atTime(14, 0))
            .endAt(LocalDate.parse("2026-05-11").atTime(17, 0))
            .headcount(9)
            .amount(money("5200"))
            .status(com.marketplace.backend.entity.workspace.WorkspaceEnums.ReservationStatus.pending)
            .notes("Pilot batch mission with same-evening quality approval expected.")
            .tags(List.of("Metal", "Pilot", "Quality"))
            .contactName("Saber Ben Romdhane")
            .contactEmail("saber@smetal.tn")
            .contactPhone("+216 29 600 441")
            .notificationChannel(NotificationChannel.email)
            .enterprise(enterprise)
            .build();

    WorkspaceReservation reservation405 =
        WorkspaceReservation.builder()
            .id("RES-405")
            .code("RSV-405")
            .title("High-volume carton sorting")
            .customer("Central Carton Office")
            .resource("South sorting cell")
            .slotId("SLT-205")
            .slotName("Sfax Poudriere Hub")
            .role(ReservationRole.provider)
            .city("Sfax")
            .category(ReservationCategory.Production)
            .startAt(LocalDate.parse("2026-05-12").atTime(8, 30))
            .endAt(LocalDate.parse("2026-05-12").atTime(13, 30))
            .headcount(11)
            .amount(money("3900"))
            .status(com.marketplace.backend.entity.workspace.WorkspaceEnums.ReservationStatus.confirmed)
            .notes("Heavy run planned to smooth production before a transfer toward Kairouan.")
            .tags(List.of("Output", "Sfax"))
            .contactName("Wafa Ben Abdallah")
            .contactEmail("wafa@cartoncentre.tn")
            .contactPhone("+216 21 908 664")
            .notificationChannel(NotificationChannel.both)
            .confirmationChannel(NotificationChannel.both)
            .confirmationDestination("wafa@cartoncentre.tn and +216 21 908 664")
            .confirmationSentAt(LocalDate.parse("2026-05-04").atTime(10, 15))
            .confirmationSummary("Confirmation routed to wafa@cartoncentre.tn and +216 21 908 664.")
            .enterprise(enterprise)
            .build();

    WorkspaceReservation reservation406 =
        WorkspaceReservation.builder()
            .id("RES-406")
            .code("RSV-406")
            .title("Agri-export dock passage")
            .customer("Bizerte Agro Export")
            .resource("Logistics dock 2")
            .slotId("SLT-201")
            .slotName("Rades Dock 2")
            .role(ReservationRole.provider)
            .city("Ben Arous")
            .category(ReservationCategory.Space)
            .startAt(LocalDate.parse("2026-05-08").atTime(11, 30))
            .endAt(LocalDate.parse("2026-05-08").atTime(14, 30))
            .headcount(7)
            .amount(money("4100"))
            .status(com.marketplace.backend.entity.workspace.WorkspaceEnums.ReservationStatus.risk)
            .notes("Tight window with temperature control and proof of loading required before departure.")
            .tags(List.of("Agri", "Temperature", "Proof"))
            .contactName("Riadh Mhiri")
            .contactEmail("riadh@bizerteagro.tn")
            .contactPhone("+216 27 540 223")
            .notificationChannel(NotificationChannel.sms)
            .enterprise(enterprise)
            .build();

    WorkspaceReservation reservation407 =
        WorkspaceReservation.builder()
            .id("RES-407")
            .code("RSV-407")
            .title("Packaging sector presentation")
            .customer("Carthage Packaging")
            .resource("Signature room")
            .slotId("SLT-203")
            .slotName("Lac 1 Meeting Lounge")
            .role(ReservationRole.consumer)
            .city("Tunis")
            .category(ReservationCategory.Space)
            .startAt(LocalDate.parse("2026-05-13").atTime(15, 0))
            .endAt(LocalDate.parse("2026-05-13").atTime(17, 0))
            .headcount(5)
            .amount(money("1650"))
            .status(com.marketplace.backend.entity.workspace.WorkspaceEnums.ReservationStatus.pending)
            .notes("Presentation session with a regional delegation and sample finalization.")
            .tags(List.of("Presentation", "Tunis"))
            .contactName("Nesrine Chaari")
            .contactEmail("nesrine.chaari@carthagepack.tn")
            .contactPhone("+216 20 114 907")
            .notificationChannel(NotificationChannel.email)
            .enterprise(enterprise)
            .build();

    workspaceReservationRepository.saveAll(
        List.of(
            reservation401,
            reservation402,
            reservation403,
            reservation404,
            reservation405,
            reservation406,
            reservation407));

    workspaceOrderRepository.saveAll(
        List.of(
            WorkspaceOrder.builder()
                .id("ORD-901")
                .code("OR-901")
                .invoiceNumber("INV-901")
                .customer("El Amen Recycling")
                .reservationId("RES-401")
                .slotId("SLT-201")
                .role(ReservationRole.provider)
                .city("Ben Arous")
                .amount(money("4600"))
                .tax(money("874"))
                .total(money("5474"))
                .createdAt(LocalDate.parse("2026-05-04"))
                .dueDate(LocalDate.parse("2026-05-12"))
                .status(OrderStatus.processing)
                .paymentStatus(PaymentStatus.review)
                .items(List.of(
                    new WorkspacePayloads.OrderLineItem("Main dock access", 1, money("3100")),
                    new WorkspacePayloads.OrderLineItem("Document handling", 1, money("900")),
                    new WorkspacePayloads.OrderLineItem("Loading crew", 1, money("600"))))
                .enterprise(enterprise)
                .build(),
            WorkspaceOrder.builder()
                .id("ORD-902")
                .code("OR-902")
                .invoiceNumber("INV-902")
                .customer("Carthage Packaging")
                .reservationId("RES-402")
                .slotId("SLT-203")
                .role(ReservationRole.consumer)
                .city("Tunis")
                .amount(money("1850"))
                .tax(money("351.5"))
                .total(money("2201.5"))
                .createdAt(LocalDate.parse("2026-05-03"))
                .dueDate(LocalDate.parse("2026-05-10"))
                .status(OrderStatus.invoiced)
                .paymentStatus(PaymentStatus.pending)
                .items(List.of(
                    new WorkspacePayloads.OrderLineItem("Meeting lounge access", 1, money("1320")),
                    new WorkspacePayloads.OrderLineItem("Partner welcome pack", 1, money("530"))))
                .enterprise(enterprise)
                .build(),
            WorkspaceOrder.builder()
                .id("ORD-903")
                .code("OR-903")
                .invoiceNumber("INV-903")
                .customer("Maison du Cuir Sahel")
                .reservationId("RES-403")
                .slotId("SLT-202")
                .role(ReservationRole.provider)
                .city("Ben Arous")
                .amount(money("2400"))
                .tax(money("456"))
                .total(money("2856"))
                .createdAt(LocalDate.parse("2026-05-02"))
                .dueDate(LocalDate.parse("2026-05-14"))
                .status(OrderStatus.fulfilled)
                .paymentStatus(PaymentStatus.paid)
                .items(List.of(
                    new WorkspacePayloads.OrderLineItem("Temporary storage", 2, money("860")),
                    new WorkspacePayloads.OrderLineItem("Weekend handling", 1, money("680"))))
                .enterprise(enterprise)
                .build(),
            WorkspaceOrder.builder()
                .id("ORD-904")
                .code("OR-904")
                .invoiceNumber("INV-904")
                .customer("Sousse Metal Services")
                .reservationId("RES-404")
                .slotId("SLT-204")
                .role(ReservationRole.consumer)
                .city("Sousse")
                .amount(money("5200"))
                .tax(money("988"))
                .total(money("6188"))
                .createdAt(LocalDate.parse("2026-05-05"))
                .dueDate(LocalDate.parse("2026-05-16"))
                .status(OrderStatus.draft)
                .paymentStatus(PaymentStatus.review)
                .items(List.of(
                    new WorkspacePayloads.OrderLineItem("Line 3 machine time", 1, money("3800")),
                    new WorkspacePayloads.OrderLineItem("Extended quality control", 1, money("900")),
                    new WorkspacePayloads.OrderLineItem("Urgent slot", 1, money("500"))))
                .enterprise(enterprise)
                .build(),
            WorkspaceOrder.builder()
                .id("ORD-905")
                .code("OR-905")
                .invoiceNumber("INV-905")
                .customer("Central Carton Office")
                .reservationId("RES-405")
                .slotId("SLT-205")
                .role(ReservationRole.provider)
                .city("Sfax")
                .amount(money("3900"))
                .tax(money("741"))
                .total(money("4641"))
                .createdAt(LocalDate.parse("2026-05-04"))
                .dueDate(LocalDate.parse("2026-05-13"))
                .status(OrderStatus.invoiced)
                .paymentStatus(PaymentStatus.pending)
                .items(List.of(
                    new WorkspacePayloads.OrderLineItem("South sorting capacity", 1, money("2850")),
                    new WorkspacePayloads.OrderLineItem("Midday output review", 1, money("650")),
                    new WorkspacePayloads.OrderLineItem("Pallet preparation", 1, money("400"))))
                .enterprise(enterprise)
                .build(),
            WorkspaceOrder.builder()
                .id("ORD-906")
                .code("OR-906")
                .invoiceNumber("INV-906")
                .customer("Bizerte Agro Export")
                .reservationId("RES-406")
                .slotId("SLT-201")
                .role(ReservationRole.provider)
                .city("Ben Arous")
                .amount(money("4100"))
                .tax(money("779"))
                .total(money("4879"))
                .createdAt(LocalDate.parse("2026-05-05"))
                .dueDate(LocalDate.parse("2026-05-15"))
                .status(OrderStatus.processing)
                .paymentStatus(PaymentStatus.pending)
                .items(List.of(
                    new WorkspacePayloads.OrderLineItem("Priority dock access", 1, money("2800")),
                    new WorkspacePayloads.OrderLineItem("Temperature follow-up", 1, money("850")),
                    new WorkspacePayloads.OrderLineItem("Loading proof", 1, money("450"))))
                .enterprise(enterprise)
                .build()));
  }

  private List<List<Integer>> workspaceHeatmap(SlotStatus status) {
    int base =
        switch (status) {
          case peak -> 72;
          case maintenance -> 78;
          case balanced -> 48;
          case available -> 22;
        };

    return List.of(
        row(base + 0, base + 4, base + 8, base + 12, base + 9, base + 4, base - 4),
        row(base - 6, base - 2, base + 3, base + 11, base + 6, base + 0, base - 11),
        row(base - 14, base - 9, base - 1, base + 6, base + 2, base - 6, base - 18),
        row(base - 10, base - 4, base + 5, base + 10, base + 5, base - 3, base - 15),
        row(base + 2, base + 7, base + 13, base + 17, base + 14, base + 8, base + 0),
        row(base - 24, base - 18, base - 12, base - 8, base - 12, base - 17, base - 29),
        row(base - 36, base - 30, base - 24, base - 21, base - 25, base - 30, base - 37));
  }

  private List<Integer> workspaceForecast(SlotStatus status) {
    int base =
        switch (status) {
          case peak -> 82;
          case maintenance -> 88;
          case balanced -> 58;
          case available -> 38;
        };
    return List.of(base + 6, base + 8, base + 4, base + 2, base + 7, base - 2, base - 6);
  }

  private List<Integer> row(int... values) {
    return java.util.Arrays.stream(values)
        .map(value -> Math.max(12, Math.min(96, value)))
        .boxed()
        .toList();
  }

  private BigDecimal money(String value) {
    return new BigDecimal(value);
  }
}
