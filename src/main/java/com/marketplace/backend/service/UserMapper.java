package com.marketplace.backend.service;

import com.marketplace.backend.dto.AdminUserDto;
import com.marketplace.backend.dto.UserResponseDto;
import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.Transporter;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.entity.enums.UserAccountStatus;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  private static final DateTimeFormatter JOINED =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public UserResponseDto toUserResponse(User u) {
    String routeRole =
        switch (u.getRole()) {
          case ROLE_ADMIN -> "admin";
          case ROLE_ENTERPRISE -> "enterprise";
          case ROLE_TRANSPORTER -> "transporter";
        };
    String company = null;
    if (u.getEnterprise() != null) {
      company = u.getEnterprise().getCompanyName();
    } else if (u.getTransporter() != null) {
      company = u.getTransporter().getCompanyName();
    }
    String avatar = initials(u.getFullName());
    return UserResponseDto.builder()
        .id(String.valueOf(u.getId()))
        .name(u.getFullName())
        .email(u.getEmail())
        .role(routeRole)
        .company(company)
        .avatar(avatar)
        .build();
  }

  public AdminUserDto toAdminUserDto(User u) {
    String role =
        u.getRole() == Role.ROLE_ENTERPRISE
            ? "enterprise"
            : u.getRole() == Role.ROLE_TRANSPORTER ? "transporter" : "enterprise";
    if (u.getRole() == Role.ROLE_ADMIN) {
      role = "enterprise";
    }
    String company = "";
    int listings = 0;
    int orders = 0;
    String revenue = "0";
    if (u.getEnterprise() != null) {
      Enterprise e = u.getEnterprise();
      company = e.getCompanyName();
      listings = e.getListingsCount() != null ? e.getListingsCount() : 0;
      orders = e.getOrdersCount() != null ? e.getOrdersCount() : 0;
      revenue = e.getRevenue() != null ? e.getRevenue() : "0";
    } else if (u.getTransporter() != null) {
      Transporter t = u.getTransporter();
      company = t.getCompanyName();
      listings = t.getListingsCount() != null ? t.getListingsCount() : 0;
      orders = t.getOrdersCount() != null ? t.getOrdersCount() : 0;
      revenue = t.getRevenue() != null ? t.getRevenue() : "0";
    }
    UserAccountStatus st = u.getAccountStatus();
    String status =
        st == null
            ? "active"
            : switch (st) {
              case active -> "active";
              case pending -> "pending";
              case suspended -> "suspended";
            };
    if (u.getRole() == Role.ROLE_ADMIN) {
      company = "Platform";
      status = "active";
    }
    return AdminUserDto.builder()
        .id("USR-" + String.format("%03d", u.getId()))
        .name(u.getFullName())
        .email(u.getEmail())
        .company(company)
        .role(u.getRole() == Role.ROLE_TRANSPORTER ? "transporter" : "enterprise")
        .status(status)
        .phone(u.getPhone() != null ? u.getPhone() : "")
        .city(u.getCity() != null ? u.getCity() : "")
        .joined(u.getCreatedAt() != null ? JOINED.format(u.getCreatedAt()) : "")
        .listings(listings)
        .orders(orders)
        .revenue(revenue)
        .verified(Boolean.TRUE.equals(u.getVerified()))
        .avatar(initials(u.getFullName()))
        .build();
  }

  private static String initials(String fullName) {
    if (fullName == null || fullName.isBlank()) {
      return "?";
    }
    String[] p = fullName.trim().split("\\s+");
    if (p.length == 1) {
      return p[0].substring(0, Math.min(2, p[0].length())).toUpperCase();
    }
    return ("" + p[0].charAt(0) + p[p.length - 1].charAt(0)).toUpperCase();
  }
}
