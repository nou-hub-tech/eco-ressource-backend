package com.marketplace.backend.entity.finance;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FinancingRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String projectName;

    private Double amountRequested;
    private Double amountApproved;

    private Double interestRate; 
    private int durationMonths;

    @Enumerated(EnumType.STRING)
    private FinancingStatus status;
}

