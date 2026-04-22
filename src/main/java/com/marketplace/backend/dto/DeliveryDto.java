package com.marketplace.backend.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryDto {

  private String id;
  private String product;
  private String client;
  private String from;
  private String to;
  private String transporter;
  private String status;
  private String co2;
  private String date;
  private String amount;
  private BigDecimal earn;
  private String pickup;
  private String delivery;
  private String route;
  private String eta;
  private String cargo;
  private String weight;
}
