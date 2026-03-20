package com.premiersport.product.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class S3Service {

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    /**
     * Generates a presigned PUT URL so the browser can upload directly to S3.
     *
     * @param originalFilename original filename from the client (used to detect extension)
     * @return map with presignedUrl and the final objectKey (to save as the image URL)
     */
    public Map<String, String> generatePresignedPutUrl(String originalFilename) {
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String key = "products/" + UUID.randomUUID() + ext;

        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build()) {

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            PresignedPutObjectRequest presigned = presigner.presignPutObject(r -> r
                    .signatureDuration(Duration.ofMinutes(10))
                    .putObjectRequest(putRequest));

            URL url = presigned.url();
            String publicUrl = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;

            log.info("Generated presigned URL for key: {}", key);
            return Map.of(
                    "presignedUrl", url.toString(),
                    "publicUrl", publicUrl,
                    "key", key
            );
        }
    }
}
