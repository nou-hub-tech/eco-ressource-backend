package com.marketplace.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    public static final long MAX_LISTING_IMAGE_BYTES = 5L * 1024 * 1024;

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * Enregistre une image pour une annonce (multipart) : type {@code image/*}, taille plafonnée (5 Mo).
     *
     * @return nom de fichier relatif, à exposer via {@code GET /files/{filename}}
     */
    public String storeListingImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fichier vide");
        }
        if (file.getSize() > MAX_LISTING_IMAGE_BYTES) {
            throw new IllegalArgumentException("Image trop volumineuse (max 5 Mo)");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("Seules les images (image/*) sont acceptées");
        }
        return storeFile(file);
    }

    public String storeFile(MultipartFile file) {
        try {
            // Get absolute path
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

            // Create directory if it doesn't exist
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("Created upload directory: " + uploadPath);
            }

            // Generate unique filename to avoid collisions
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

            // Save file
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("File saved to: " + filePath.toString());
            return fileName;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not store file: " + e.getMessage());
        }
    }
}