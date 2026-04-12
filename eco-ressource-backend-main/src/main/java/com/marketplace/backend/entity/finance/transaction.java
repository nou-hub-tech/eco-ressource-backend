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
public class transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idtransaction;
    private String project;
    @Enumerated(EnumType.STRING)
    private TransactionType type;
    private Double amount;
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
    private String date;
}
