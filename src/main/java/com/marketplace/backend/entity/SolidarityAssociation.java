package com.marketplace.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "solidarity_associations")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolidarityAssociation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 2000)
    private String mission;

    @Column(nullable = false)
    private Integer members;

    @Column(nullable = false)
    private Double donations;

    @Column(nullable = false)
    private String statusLabel;

    @Column(length = 2000)
    private String aiInsight;

    @Column(name = "goal_amount")
    private Double goalAmount;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @OneToMany(mappedBy = "association", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Donation> donationsList;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (goalAmount == null) {
            goalAmount = 0.0;
        }
    }
}
