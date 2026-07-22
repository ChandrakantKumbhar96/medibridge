package com.medibridge.record;

import com.medibridge.common.exception.BadRequestException;
import com.medibridge.common.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

/**
 * Local disk storage for uploaded medical reports.
 *
 * <p>Three deliberate protections:
 * <ul>
 *   <li>the stored filename is a generated UUID, never the user's filename -
 *       a name like {@code ../../application-local.yml} would otherwise let an
 *       upload escape the storage directory;</li>
 *   <li>the content type is checked against an allow-list, not the extension;</li>
 *   <li>the resolved path is verified to still sit inside the storage root
 *       before any read or write.</li>
 * </ul>
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp");

    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final Path root;

    public FileStorageService(@Value("${medibridge.storage.upload-dir}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(root);
            log.info("Medical report storage: {}", root);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create upload directory: " + root, e);
        }
    }

    /** @return the relative path to persist in medical_report.report_data_url */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("File is too large. Maximum size is 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException(
                    "Unsupported file type. Allowed: PDF, JPEG, PNG, WEBP");
        }

        String storedName = UUID.randomUUID() + extensionFor(contentType);
        Path target = root.resolve(storedName).normalize();

        if (!target.startsWith(root)) {
            throw new BadRequestException("Invalid file path");
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file", e);
        }

        return storedName;
    }

    public byte[] read(String storedName) {
        Path target = root.resolve(storedName).normalize();

        if (!target.startsWith(root)) {
            throw new BadRequestException("Invalid file path");
        }
        if (!Files.exists(target)) {
            throw new ResourceNotFoundException("File no longer available");
        }

        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read file", e);
        }
    }

    public void delete(String storedName) {
        try {
            Path target = root.resolve(storedName).normalize();
            if (target.startsWith(root)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException e) {
            log.warn("Could not delete stored file {}: {}", storedName, e.getMessage());
        }
    }

    private String extensionFor(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "application/pdf" -> ".pdf";
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }

    /** '2.4 MB' - the display format MedicalRecords.jsx already renders. */
    public static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
