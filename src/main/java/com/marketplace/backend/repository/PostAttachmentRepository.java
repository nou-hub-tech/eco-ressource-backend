package com.marketplace.backend.repository;

import com.marketplace.backend.entity.PostAttachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostAttachmentRepository extends JpaRepository<PostAttachment, Long> {

  List<PostAttachment> findByListingId(Long listingId);
}
