package com.marketplace.backend.service;

import com.marketplace.backend.dto.ExchangeRequestDto;
import com.marketplace.backend.dto.ListingDto;
import com.marketplace.backend.dto.ListingModerationRequest;
import com.marketplace.backend.dto.MarketplaceListingRequest;
import com.marketplace.backend.dto.ReservationDto;
import com.marketplace.backend.dto.WalletTransactionDto;
import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.ExchangeRequest;
import com.marketplace.backend.entity.Listing;
import com.marketplace.backend.entity.Reservation;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.WalletTransaction;
import com.marketplace.backend.entity.enums.ExchangeRequestStatus;
import com.marketplace.backend.entity.enums.ListingStatus;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.repository.EnterpriseRepository;
import com.marketplace.backend.repository.ExchangeRequestRepository;
import com.marketplace.backend.repository.ListingRepository;
import com.marketplace.backend.repository.ReservationRepository;
import com.marketplace.backend.repository.WalletTransactionRepository;
import com.marketplace.backend.security.SecurityUserHelper;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListingService {

  private final ListingRepository listingRepository;
  private final EnterpriseRepository enterpriseRepository;
  private final SecurityUserHelper securityUserHelper;
  private final ExchangeRequestRepository exchangeRequestRepository;
  private final ReservationRepository reservationRepository;
  private final WalletTransactionRepository walletTransactionRepository;

  @Transactional(readOnly = true)
  public List<ListingDto> findAll() {
    return listingRepository.findAll().stream().map(ListingDto::from).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public ListingDto getById(Long id) {
    Listing l = listingRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    return ListingDto.from(l);
  }

  @Transactional(readOnly = true)
  public List<ListingDto> findMine(Authentication auth) {
    Enterprise e = securityUserHelper.requireEnterprise(auth);
    return listingRepository.findByEnterpriseId(e.getId()).stream()
        .map(ListingDto::from)
        .collect(Collectors.toList());
  }

  private Enterprise resolveEnterpriseForWrite(Authentication auth, Long enterpriseId) {
    User u = securityUserHelper.requireUser(auth);
    if (enterpriseId != null) {
      Enterprise target =
          enterpriseRepository
              .findById(enterpriseId)
              .orElseThrow(() -> new IllegalArgumentException("Enterprise not found"));
      if (u.getRole() != Role.ROLE_ADMIN) {
        if (u.getEnterprise() == null || !u.getEnterprise().getId().equals(target.getId())) {
          throw new IllegalArgumentException("Forbidden");
        }
      }
      return target;
    }
    return securityUserHelper.requireEnterprise(auth);
  }

  @Transactional
  public ListingDto create(Authentication auth, MarketplaceListingRequest req) {
    Enterprise e = resolveEnterpriseForWrite(auth, req.getEnterpriseId());
    Listing l =
        Listing.builder()
            .enterprise(e)
            .title(req.getTitle())
            .category(req.getCategory())
            .price(req.getPrice())
            .quantityLabel(req.getQuantityLabel())
            .status(ListingStatus.valueOf(req.getStatus().toLowerCase(Locale.ROOT)))
            .aiInsight(req.getAiInsight())
            .build();
    listingRepository.save(l);
    e.setListingsCount((e.getListingsCount() != null ? e.getListingsCount() : 0) + 1);
    enterpriseRepository.save(e);
    return ListingDto.from(l);
  }

  @Transactional
  public ListingDto update(Long id, Authentication auth, MarketplaceListingRequest req) {
    User u = securityUserHelper.requireUser(auth);
    Listing l = listingRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    if (req.getEnterpriseId() != null) {
      if (u.getRole() != Role.ROLE_ADMIN) {
        throw new IllegalArgumentException("Forbidden");
      }
      Enterprise ent =
          enterpriseRepository
              .findById(req.getEnterpriseId())
              .orElseThrow(() -> new IllegalArgumentException("Enterprise not found"));
      l.setEnterprise(ent);
    } else if (u.getRole() != Role.ROLE_ADMIN) {
      if (u.getEnterprise() == null
          || !u.getEnterprise().getId().equals(l.getEnterprise().getId())) {
        throw new IllegalArgumentException("Forbidden");
      }
    }
    l.setTitle(req.getTitle());
    l.setCategory(req.getCategory());
    l.setPrice(req.getPrice());
    l.setQuantityLabel(req.getQuantityLabel());
    l.setStatus(ListingStatus.valueOf(req.getStatus().toLowerCase(Locale.ROOT)));
    l.setAiInsight(req.getAiInsight());
    listingRepository.save(l);
    return ListingDto.from(l);
  }

  @Transactional
  public void delete(Long id, Authentication auth) {
    User u = securityUserHelper.requireUser(auth);
    Listing l = listingRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    Enterprise owner = l.getEnterprise();
    if (u.getRole() != Role.ROLE_ADMIN) {
      if (u.getEnterprise() == null || !u.getEnterprise().getId().equals(owner.getId())) {
        throw new IllegalArgumentException("Forbidden");
      }
    }
    listingRepository.delete(l);
    owner.setListingsCount(Math.max(0, (owner.getListingsCount() != null ? owner.getListingsCount() : 1) - 1));
    enterpriseRepository.save(owner);
  }

  @Transactional
  public ListingDto moderate(Long id, ListingModerationRequest req) {
    Listing l =
        listingRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    l.setStatus(ListingStatus.valueOf(req.getStatus().toLowerCase(Locale.ROOT)));
    listingRepository.save(l);
    return ListingDto.from(l);
  }

  @Transactional(readOnly = true)
  public List<ExchangeRequestDto> myExchangeRequests(Authentication auth) {
    Enterprise e = securityUserHelper.requireEnterprise(auth);
    return exchangeRequestRepository.findByRecipientEnterpriseId(e.getId()).stream()
        .map(this::toExchangeDto)
        .collect(Collectors.toList());
  }

  @Transactional
  public ExchangeRequestDto updateExchangeStatus(Long id, String status, Authentication auth) {
    User u = securityUserHelper.requireUser(auth);
    ExchangeRequest r =
        exchangeRequestRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Not found"));
    if (u.getRole() != Role.ROLE_ADMIN) {
      Enterprise e = u.getEnterprise();
      if (e == null || !r.getRecipientEnterprise().getId().equals(e.getId())) {
        throw new IllegalArgumentException("Forbidden");
      }
    }
    r.setStatus(ExchangeRequestStatus.valueOf(status.toLowerCase(Locale.ROOT)));
    exchangeRequestRepository.save(r);
    return toExchangeDto(r);
  }

  @Transactional(readOnly = true)
  public List<ReservationDto> myReservations(Authentication auth) {
    Enterprise e = securityUserHelper.requireEnterprise(auth);
    return reservationRepository.findByEnterpriseId(e.getId()).stream()
        .map(this::toReservationDto)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<WalletTransactionDto> myWalletTransactions(Authentication auth) {
    User u = securityUserHelper.requireUser(auth);
    return walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(u.getId()).stream()
        .map(this::toWalletDto)
        .collect(Collectors.toList());
  }

  private ExchangeRequestDto toExchangeDto(ExchangeRequest r) {
    return ExchangeRequestDto.builder()
        .id("REQ-" + String.format("%03d", r.getId()))
        .from(r.getFromCompanyName())
        .avatar(r.getFromAvatar())
        .item(r.getItem())
        .type(r.getTypeLabel())
        .from_date(r.getFromDate().toString())
        .to_date(r.getToDate().toString())
        .duration(r.getDurationLabel())
        .price(r.getPrice())
        .message(r.getMessage())
        .status(r.getStatus().name())
        .received(r.getReceivedLabel())
        .urgent(r.isUrgent())
        .build();
  }

  private ReservationDto toReservationDto(Reservation r) {
    return ReservationDto.builder()
        .id("RES-" + String.format("%03d", r.getId()))
        .type(r.getTypeLabel())
        .item(r.getItem())
        .company(r.getCompanyName())
        .from(r.getFromDate().toString())
        .to(r.getToDate().toString())
        .price(r.getPrice())
        .status(r.getStatus().name())
        .build();
  }

  private WalletTransactionDto toWalletDto(WalletTransaction t) {
    return WalletTransactionDto.builder()
        .id("TXN-" + t.getId())
        .label(t.getLabel())
        .type(t.getTypeLabel())
        .amount(t.getAmount())
        .positive(t.getPositiveFlag())
        .status(t.getStatus().name())
        .date(t.getValueDate() != null ? t.getValueDate().toString() : "")
        .from(t.getFromParty())
        .to(t.getToParty())
        .build();
  }
}
