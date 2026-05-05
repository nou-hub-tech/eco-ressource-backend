package com.marketplace.backend.controller;

import com.marketplace.backend.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/files")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    public FileUploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Fichier vide reçu"));
            }
            String fileName = fileStorageService.storeFile(file);
            String fileUrl = "/files/" + fileName;
            System.out.println("[UPLOAD] ✅ URL retournée : " + fileUrl);
            return ResponseEntity.ok(Map.of("url", fileUrl, "filename", fileName));
        } catch (Exception e) {
            System.err.println("[UPLOAD] ❌ Erreur upload : " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Erreur inconnue"));
        }
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxSize(
            org.springframework.web.multipart.MaxUploadSizeExceededException exc) {
        System.err.println("[UPLOAD] ❌ Fichier trop volumineux : " + exc.getMessage());
        return ResponseEntity.status(413)
                .body(Map.of("error", "Fichier trop volumineux. Maximum autorisé : 10MB"));
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        try {
            // Utilise le même répertoire que storeFile()
            Path filePath = fileStorageService.resolveDir().toPath().resolve(filename).normalize();

            System.out.println("[UPLOAD] Serving file: " + filePath.toAbsolutePath());

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                System.out.println("[UPLOAD] File not found: " + filePath);
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = MediaType.IMAGE_JPEG_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            System.out.println("Error serving file: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    // Debug endpoint — vérifie le dossier d'upload
    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debug() {
        try {
            java.io.File dir = fileStorageService.resolveDir();
            return ResponseEntity.ok(Map.of(
                    "uploadDir", dir.getAbsolutePath(),
                    "exists", dir.exists(),
                    "isDirectory", dir.isDirectory(),
                    "canWrite", dir.canWrite()));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("error", e.getMessage()));
        }
    }
}
