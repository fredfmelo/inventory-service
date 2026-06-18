package com.fredfmelo.inventoryservice.product.service;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fredfmelo.eventdrivencore.exception.TechnicalException;
import com.fredfmelo.inventoryservice.api.model.ProductImageResponse;
import com.fredfmelo.inventoryservice.config.ServiceConfig;
import com.fredfmelo.inventoryservice.product.domain.ProductImageEntity;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class S3PresignedUrlService {

    private final S3Presigner s3Presigner;
    private final ServiceConfig serviceConfig;

    public List<ProductImageResponse> toImageResponses(List<ProductImageEntity> images) {
        if (images == null || images.isEmpty()) {
            return Collections.emptyList();
        }

        return images.stream()
                .map(this::toImageResponse)
                .toList();
    }

    private ProductImageResponse toImageResponse(ProductImageEntity image) {
        ProductImageResponse response = new ProductImageResponse();
        response.setImageId(UUID.fromString(image.getImageId()));
        response.setUrl(presign(image.getS3Key()));
        return response;
    }

    private URI presign(String s3Key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(serviceConfig.getAws().getS3().getBucketName())
                    .key(s3Key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(
                            serviceConfig.getAws().getS3().getPresignedUrlExpirationMinutes()))
                    .getObjectRequest(getObjectRequest)
                    .build();

            return URI.create(s3Presigner.presignGetObject(presignRequest).url().toString());

        } catch (SdkException ex) {
            throw new TechnicalException("Failed to generate presigned URL for image: " + s3Key, ex);
        }
    }
}
