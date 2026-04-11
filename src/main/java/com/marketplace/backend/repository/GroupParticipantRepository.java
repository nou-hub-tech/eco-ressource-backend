package com.marketplace.backend.repository;

import com.marketplace.backend.entity.GroupParticipant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupParticipantRepository extends JpaRepository<GroupParticipant, Long> {

  List<GroupParticipant> findByGroupId(Long groupId);

  Optional<GroupParticipant> findByGroupIdAndCompanyId(Long groupId, Long companyId);

  boolean existsByGroupIdAndCompanyId(Long groupId, Long companyId);
}
