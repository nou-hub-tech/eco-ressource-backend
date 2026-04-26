package com.marketplace.backend.controller;

import com.marketplace.backend.dto.EventDocumentResponse;
import com.marketplace.backend.entity.EventDocument;
import com.marketplace.backend.service.EventDocumentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/platform-events")
@RequiredArgsConstructor
public class EventDocumentController {

  private final EventDocumentService eventDocumentService;

  @PostMapping("/{eventId}/documents")
  public ResponseEntity<EventDocumentResponse> uploadDocument(
      @PathVariable Long eventId,
      @RequestParam("file") MultipartFile file) {
    try {
      return ResponseEntity.status(HttpStatus.CREATED).body(eventDocumentService.uploadDocument(eventId, file));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @GetMapping("/{eventId}/documents")
  public ResponseEntity<List<EventDocumentResponse>> getDocuments(@PathVariable Long eventId) {
    return ResponseEntity.ok(eventDocumentService.getEventDocuments(eventId));
  }

  @DeleteMapping("/documents/{documentId}")
  public ResponseEntity<Void> deleteDocument(@PathVariable Long documentId) {
    try {
      eventDocumentService.deleteDocument(documentId);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/documents/{documentId}/download")
  public ResponseEntity<Resource> downloadDocument(@PathVariable Long documentId) {
    try {
      Resource resource = eventDocumentService.downloadDocumentAsResource(documentId);
      EventDocument docEntity = eventDocumentService.getDocumentEntity(documentId);
      
      String contentType = docEntity.getFileType();
      if (contentType == null) {
          contentType = "application/octet-stream";
      }

      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType(contentType))
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + docEntity.getFileName() + "\"")
          .body(resource);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      return ResponseEntity.internalServerError().build();
    }
  }
}
