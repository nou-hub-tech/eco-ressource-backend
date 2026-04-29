package com.marketplace.backend.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ReservationStatus {
  CONFIRMED,
  PENDING,
  CANCELLED

  ;

  @JsonCreator
  public static ReservationStatus fromJson(String value) {
    if (value == null) return null;
    return ReservationStatus.valueOf(value.trim().toUpperCase());
  }

  @JsonValue
  public String toJson() {
    return name().toLowerCase();
  }
}
