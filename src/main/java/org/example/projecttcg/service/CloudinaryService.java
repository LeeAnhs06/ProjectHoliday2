package org.example.projecttcg.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${spring.cloudinary.cloud-name:}")
    private String cloudName;

    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // Check if Cloudinary is configured with valid values
        if (cloudName == null || cloudName.trim().isEmpty() || cloudName.equals("YOUR_CLOUD_NAME")) {
            log.warn("[CLOUDINARY MOCK] Cloudinary credentials are not configured. Returning mock image URL.");
            // Default placeholder card image to avoid breaking the application flow
            return "https://images.pokemontcg.io/sv3/223.png";
        }

        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            log.error("Cloudinary upload failed: {}", e.getMessage());
            throw new RuntimeException("Lỗi khi tải ảnh lên Cloudinary", e);
        }
    }
}
