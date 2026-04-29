package com.marketplace.backend.controller;

import com.marketplace.backend.entity.EventAttachment;
import com.marketplace.backend.service.EventAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
public class AttachmentController {

  private final EventAttachmentService attachmentService;

  @GetMapping("/{id}/download")
  public ResponseEntity<Resource> downloadAttachment(@PathVariable Long id) {
    try {
      EventAttachment attachment = attachmentService.getAttachmentById(id);
      byte[] fileContent = attachmentService.downloadAttachment(id);
      
      ByteArrayResource resource = new ByteArrayResource(fileContent);
      
      String contentType = attachment.getFileType();
      if (contentType == null) {
        contentType = "application/octet-stream";
      }
      
      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType(contentType))
          .header(HttpHeaders.CONTENT_DISPOSITION, 
                  "attachment; filename=\"" + attachment.getFileName() + "\"")
          .body(resource);
          
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteAttachment(@PathVariable Long id) {
    try {
      attachmentService.deleteAttachment(id);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
