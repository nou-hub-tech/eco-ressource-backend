package com.marketplace.backend.service;

import com.marketplace.backend.dto.EventDocumentResponse;
import com.marketplace.backend.entity.EventDocument;
import com.marketplace.backend.entity.PlatformEvent;
import com.marketplace.backend.repository.EventDocumentRepository;
import com.marketplace.backend.repository.PlatformEventRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class EventDocumentService {

  private final EventDocumentRepository eventDocumentRepository;
  private final PlatformEventRepository platformEventRepository;
  private final EventFileStorageService fileStorageService;

  @Transactional
  public EventDocumentResponse uploadDocument(Long eventId, MultipartFile file) {
    PlatformEvent event = platformEventRepository.findById(eventId)
        .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));

    String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "document");
    String storedFileName = fileStorageService.storeFile(file);

    EventDocument document = EventDocument.builder()
        .platformEvent(event)
        .fileName(originalFileName)
        .storedFileName(storedFileName)
        .fileType(file.getContentType())
        .fileSize(file.getSize())
        .build();

    document = eventDocumentRepository.save(document);
    return mapToResponse(document);
  }

  @Transactional(readOnly = true)
  public List<EventDocumentResponse> getEventDocuments(Long eventId) {
    return eventDocumentRepository.findByPlatformEventId(eventId).stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  @Transactional
  public void deleteDocument(Long documentId) {
    EventDocument document = eventDocumentRepository.findById(documentId)
        .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));

    fileStorageService.deleteFile(document.getStoredFileName());
    eventDocumentRepository.delete(document);
  }

  @Transactional(readOnly = true)
  public Resource downloadDocumentAsResource(Long documentId) {
    EventDocument document = eventDocumentRepository.findById(documentId)
        .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));
    return fileStorageService.loadFileAsResource(document.getStoredFileName());
  }
  
  @Transactional(readOnly = true)
  public EventDocument getDocumentEntity(Long documentId) {
       return eventDocumentRepository.findById(documentId)
        .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));
  }

  private EventDocumentResponse mapToResponse(EventDocument document) {
    return EventDocumentResponse.builder()
        .id(document.getId())
        .platformEventId(document.getPlatformEvent().getId())
        .fileName(document.getFileName())
        .fileType(document.getFileType())
        .fileSize(document.getFileSize())
        .uploadedAt(document.getUploadedAt())
        .build();
  }
}
