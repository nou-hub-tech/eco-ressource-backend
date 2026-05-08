package com.marketplace.backend.repository.workspace;

import com.marketplace.backend.entity.workspace.WorkspaceReservation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceReservationRepository extends JpaRepository<WorkspaceReservation, String> {

  List<WorkspaceReservation> findByEnterpriseIdOrderByStartAtAsc(Long enterpriseId);

  List<WorkspaceReservation> findByEnterpriseIdAndSlotId(Long enterpriseId, String slotId);
}
