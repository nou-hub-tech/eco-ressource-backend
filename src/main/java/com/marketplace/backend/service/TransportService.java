package com.marketplace.backend.service;

import com.marketplace.backend.dto.DeliveryDto;
import com.marketplace.backend.dto.TransportOfferRequest;
import com.marketplace.backend.entity.Delivery;
import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.Transporter;
import com.marketplace.backend.entity.TransportOffer;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.enums.DeliveryStatus;
import com.marketplace.backend.entity.enums.TransportOfferStatus;
import com.marketplace.backend.repository.DeliveryRepository;
import com.marketplace.backend.repository.EnterpriseRepository;
import com.marketplace.backend.repository.TransportOfferRepository;
import com.marketplace.backend.repository.TransporterRepository;
import com.marketplace.backend.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransportService {

  private final DeliveryRepository deliveryRepository;
  private final TransportOfferRepository transportOfferRepository;
  private final UserRepository userRepository;
  private final TransporterRepository transporterRepository;
  private final EnterpriseRepository enterpriseRepository;

  @Transactional(readOnly = true)
  public List<DeliveryDto> deliveriesForEnterprise(Authentication auth) {
    Enterprise e = requireEnterprise(auth);
    return deliveryRepository.findByEnterpriseId(e.getId()).stream()
        .map(this::toDeliveryDto)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<DeliveryDto> deliveriesForTransporter(Authentication auth) {
    Transporter t = requireTransporter(auth);
    return deliveryRepository.findByTransporterId(t.getId()).stream()
        .map(this::toDeliveryDto)
        .collect(Collectors.toList());
  }

  @Transactional
  public void createOffer(Authentication auth, TransportOfferRequest req) {
    Transporter t = requireTransporter(auth);
    TransportOffer o =
        TransportOffer.builder()
            .transporter(t)
            .fromLocation(req.getFromLocation())
            .toLocation(req.getToLocation())
            .cargoDescription(req.getCargoDescription())
            .weightLabel(req.getWeightLabel())
            .proposedEarn(req.getProposedEarn())
            .status(TransportOfferStatus.open)
            .build();
    transportOfferRepository.save(o);
  }

  private Enterprise requireEnterprise(Authentication auth) {
    User u =
        userRepository
            .findByEmailWithProfiles(auth.getName())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    if (u.getEnterprise() == null) {
      throw new IllegalArgumentException("Enterprise profile required");
    }
    return u.getEnterprise();
  }

  private Transporter requireTransporter(Authentication auth) {
    User u =
        userRepository
            .findByEmailWithProfiles(auth.getName())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    if (u.getTransporter() == null) {
      throw new IllegalArgumentException("Transporter profile required");
    }
    return u.getTransporter();
  }

  private DeliveryDto toDeliveryDto(Delivery d) {
    String id = "DEL-" + d.getId();
    String status = formatDeliveryStatus(d.getStatus());
    String amountStr =
        d.getAmount() != null ? String.valueOf(d.getAmount().intValue()) : "";
    return DeliveryDto.builder()
        .id(id)
        .product(d.getProductLabel())
        .client(d.getClientName())
        .from(d.getFromLocation())
        .to(d.getToLocation())
        .transporter(d.getTransporter() != null ? d.getTransporter().getCompanyName() : "")
        .status(status)
        .co2(d.getCo2Label())
        .date(d.getDateLabel())
        .amount(amountStr)
        .earn(d.getEarnAmount())
        .pickup(d.getPickupLabel())
        .delivery(d.getDeliveryLabel())
        .route(d.getFromLocation() + " → " + d.getToLocation())
        .cargo(d.getProductLabel())
        .weight("")
        .eta(d.getDateLabel())
        .build();
  }

  private static String formatDeliveryStatus(DeliveryStatus s) {
    if (s == null) {
      return "";
    }
    return switch (s) {
      case in_transit -> "in-transit";
      case transit -> "TRANSIT";
      case scheduled -> "SCHEDULED";
      default -> s.name().replace('_', '-');
    };
  }
}
