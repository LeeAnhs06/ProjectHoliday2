package org.example.projecttcg.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Value("${spring.cloudinary.cloud-name:YOUR_CLOUD_NAME}")
    private String cloudName;

    @Value("${spring.cloudinary.api-key:YOUR_API_KEY}")
    private String apiKey;

    @Value("${spring.cloudinary.api-secret:YOUR_API_SECRET}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }
}
