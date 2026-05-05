package com.marketplace.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * Sauvegarde le fichier et retourne son nom unique.
     * Utilise le répertoire temp Java en fallback si le dossier configuré échoue.
     */
    public String storeFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new RuntimeException("Nom de fichier invalide");
        }

        // Extension (lowercase)
        String extension = "";
        int dot = originalFilename.lastIndexOf('.');
        if (dot >= 0) {
            extension = originalFilename.substring(dot).toLowerCase();
        }

        // Nom de fichier unique
        String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

        // Essayer le répertoire configuré, sinon utiliser le temp Java
        File targetDir = resolveDir();
        File targetFile = new File(targetDir, fileName);

        try {
            file.transferTo(targetFile);
            System.out.println("[UPLOAD] ✅ Fichier sauvegardé : " + targetFile.getAbsolutePath());
            return fileName;
        } catch (IOException e) {
            System.err.println("[UPLOAD] ❌ Erreur : " + e.getMessage());
            throw new RuntimeException("Impossible de sauvegarder le fichier : " + e.getMessage(), e);
        }
    }

    /**
     * Retourne le répertoire d'upload (configuré ou temp Java).
     * Crée le répertoire s'il n'existe pas.
     */
    public File resolveDir() {
        // 1. Essayer le chemin configuré
        File configured = new File(uploadDir).getAbsoluteFile();
        if (configured.exists() || configured.mkdirs()) {
            System.out.println("[UPLOAD] 📁 Dossier : " + configured.getAbsolutePath());
            return configured;
        }

        // 2. Fallback : répertoire temp Java (toujours accessible sur Windows)
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "eco-uploads");
        if (!tempDir.exists()) tempDir.mkdirs();
        System.out.println("[UPLOAD] ⚠️ Fallback vers dossier temp : " + tempDir.getAbsolutePath());
        return tempDir;
    }
}