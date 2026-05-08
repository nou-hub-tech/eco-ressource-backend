package com.marketplace.backend.repository.workspace;

import com.marketplace.backend.entity.workspace.WorkspaceReservationSlot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceReservationSlotRepository
    extends JpaRepository<WorkspaceReservationSlot, String> {

  List<WorkspaceReservationSlot> findByEnterpriseIdOrderByNameAsc(Long enterpriseId);
}
