package com.marketplace.backend.service;

import com.marketplace.backend.entity.EventAttachment;
import com.marketplace.backend.entity.PlatformEvent;
import com.marketplace.backend.repository.EventAttachmentRepository;
import com.marketplace.backend.repository.PlatformEventRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class EventAttachmentService {

  private final EventAttachmentRepository attachmentRepository;
  private final PlatformEventRepository platformEventRepository;

  @Value("${app.upload.dir:uploads/events}")
  private String uploadDir;

  private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
  private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
      "application/pdf",
      "image/jpeg",
      "image/png",
      "image/jpg"
  );

  @Transactional
  public EventAttachment uploadAttachment(Long eventId, MultipartFile file) {
    // Validate event exists
    PlatformEvent event = platformEventRepository.findById(eventId)
        .orElseThrow(() -> new IllegalArgumentException("Event not found"));

    // Validate file
    validateFile(file);

    try {
      // Create upload directory if it doesn't exist
      Path uploadPath = Paths.get(uploadDir);
      if (!Files.exists(uploadPath)) {
        Files.createDirectories(uploadPath);
      }

      // Generate unique filename
      String originalFilename = file.getOriginalFilename();
      String fileExtension = StringUtils.getFilenameExtension(originalFilename);
      String uniqueFilename = generateUniqueFilename(originalFilename);
      Path filePath = uploadPath.resolve(uniqueFilename);

      // Save file to disk
      Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

      // Save metadata to database
      EventAttachment attachment = EventAttachment.builder()
          .fileName(originalFilename)
          .fileType(file.getContentType())
          .filePath(filePath.toString())
          .fileSize(file.getSize())
          .platformEvent(event)
          .uploadedAt(LocalDateTime.now())
          .build();

      return attachmentRepository.save(attachment);

    } catch (IOException e) {
      throw new RuntimeException("Failed to store file", e);
    }
  }

  @Transactional(readOnly = true)
  public List<EventAttachment> getEventAttachments(Long eventId) {
    PlatformEvent event = platformEventRepository.findById(eventId)
        .orElseThrow(() -> new IllegalArgumentException("Event not found"));
    
    return attachmentRepository.findByPlatformEvent(event);
  }

  @Transactional(readOnly = true)
  public EventAttachment getAttachmentById(Long attachmentId) {
    return attachmentRepository.findById(attachmentId)
        .orElseThrow(() -> new IllegalArgumentException("Attachment not found"));
  }

  public byte[] downloadAttachment(Long attachmentId) {
    EventAttachment attachment = getAttachmentById(attachmentId);
    
    try {
      Path filePath = Paths.get(attachment.getFilePath());
      if (!Files.exists(filePath)) {
        throw new RuntimeException("File not found on disk");
      }
      return Files.readAllBytes(filePath);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read file", e);
    }
  }

  @Transactional
  public void deleteAttachment(Long attachmentId) {
    EventAttachment attachment = getAttachmentById(attachmentId);
    
    try {
      // Delete file from disk
      Path filePath = Paths.get(attachment.getFilePath());
      if (Files.exists(filePath)) {
        Files.delete(filePath);
      }
      
      // Delete from database
      attachmentRepository.delete(attachment);
      
    } catch (IOException e) {
      throw new RuntimeException("Failed to delete file", e);
    }
  }

  @Transactional
  public void deleteAttachmentsByEventId(Long eventId) {
    List<EventAttachment> attachments = attachmentRepository.findByPlatformEventId(eventId);
    
    for (EventAttachment attachment : attachments) {
      try {
        // Delete file from disk
        Path filePath = Paths.get(attachment.getFilePath());
        if (Files.exists(filePath)) {
          Files.delete(filePath);
        }
      } catch (IOException e) {
        // Log error but continue with database deletion
        System.err.println("Failed to delete file: " + attachment.getFilePath());
      }
    }
    
    // Delete from database
    attachmentRepository.deleteByPlatformEventId(eventId);
  }

  private void validateFile(MultipartFile file) {
    if (file.isEmpty()) {
      throw new IllegalArgumentException("File is empty");
    }

    if (file.getSize() > MAX_FILE_SIZE) {
      throw new IllegalArgumentException("File size exceeds maximum limit of 5MB");
    }

    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
      throw new IllegalArgumentException("File type not allowed. Only PDF, JPG, and PNG files are accepted");
    }
  }

  private String generateUniqueFilename(String originalFilename) {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    String uuid = UUID.randomUUID().toString().substring(0, 8);
    String fileExtension = StringUtils.getFilenameExtension(originalFilename);
    
    if (fileExtension != null && !fileExtension.isEmpty()) {
      return String.format("%s_%s.%s", timestamp, uuid, fileExtension);
    } else {
      return String.format("%s_%s", timestamp, uuid);
    }
  }
}
