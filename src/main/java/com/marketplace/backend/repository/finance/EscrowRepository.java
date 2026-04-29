package com.marketplace.backend.repository.finance;

import com.marketplace.backend.entity.finance.EscrowStatus;
import com.marketplace.backend.entity.finance.escrow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EscrowRepository extends JpaRepository<escrow, Long> {

    /** Escrows de l'entreprise connectée */
    List<escrow> findByEnterpriseId(Long enterpriseId);

    /** Tous les escrows avec un statut donné (utilisé par le polling de livraison) */
    List<escrow> findByStatus(EscrowStatus status);

    /** 🎯 Escrow directement lié à une commande de livraison */
    List<escrow> findByDeliveryOrderId(Long deliveryOrderId);

    /** 🎯 Escrow LOCKED lié à une commande de livraison */
    List<escrow> findByDeliveryOrderIdAndStatus(Long deliveryOrderId, EscrowStatus status);
}
