package com.marketplace.backend.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;

@Service
public class EventFileStorageService {

  private final Path fileStorageLocation = Paths.get("uploads", "events").toAbsolutePath().normalize();
  
  // 5 MB limit
  private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
  private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
      "application/pdf",
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // docx
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // xlsx
      "image/png",
      "image/jpeg"
  );

  @PostConstruct
  public void init() {
    try {
      Files.createDirectories(this.fileStorageLocation);
    } catch (Exception ex) {
      throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
    }
  }

  public String storeFile(MultipartFile file) {
    if (file.getSize() > MAX_FILE_SIZE) {
      throw new IllegalArgumentException("File size exceeds 5MB limit!");
    }
    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
      throw new IllegalArgumentException("Invalid file type! Allowed types: PDF, DOCX, XLSX, PNG, JPG.");
    }

    String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
    if (originalFileName.contains("..")) {
      throw new IllegalArgumentException("Sorry! Filename contains invalid path sequence " + originalFileName);
    }

    String extension = "";
    int i = originalFileName.lastIndexOf('.');
    if (i > 0) {
      extension = originalFileName.substring(i);
    }

    String storedFileName = UUID.randomUUID().toString() + extension;

    try {
      Path targetLocation = this.fileStorageLocation.resolve(storedFileName);
      Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
      return storedFileName;
    } catch (IOException ex) {
      throw new RuntimeException("Could not store file " + originalFileName + ". Please try again!", ex);
    }
  }

  public Resource loadFileAsResource(String storedFileName) {
    try {
      Path filePath = this.fileStorageLocation.resolve(storedFileName).normalize();
      Resource resource = new UrlResource(filePath.toUri());
      if (resource.exists()) {
        return resource;
      } else {
        throw new RuntimeException("File not found " + storedFileName);
      }
    } catch (MalformedURLException ex) {
      throw new RuntimeException("File not found " + storedFileName, ex);
    }
  }

  public void deleteFile(String storedFileName) {
    try {
      Path filePath = this.fileStorageLocation.resolve(storedFileName).normalize();
      Files.deleteIfExists(filePath);
    } catch (IOException ex) {
      throw new RuntimeException("Could not delete file " + storedFileName, ex);
    }
  }
}
