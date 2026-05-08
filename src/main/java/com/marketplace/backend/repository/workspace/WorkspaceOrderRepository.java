package com.marketplace.backend.repository.workspace;

import com.marketplace.backend.entity.workspace.WorkspaceOrder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceOrderRepository extends JpaRepository<WorkspaceOrder, String> {

  List<WorkspaceOrder> findByEnterpriseIdOrderByCreatedAtDesc(Long enterpriseId);

  List<WorkspaceOrder> findByEnterpriseIdAndReservationId(Long enterpriseId, String reservationId);
}
