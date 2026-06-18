package com.fredfmelo.inventoryservice.product.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fredfmelo.eventdrivencore.exception.BusinessException;
import com.fredfmelo.eventdrivencore.exception.TechnicalException;
import com.fredfmelo.inventoryservice.config.ServiceConfig;
import com.fredfmelo.inventoryservice.product.domain.ProductEntity;
import com.fredfmelo.inventoryservice.product.domain.ProductImageEntity;
import com.fredfmelo.inventoryservice.product.repository.ProductRepository;
import com.fredfmelo.inventoryservice.security.Role;
import com.fredfmelo.inventoryservice.security.UserContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageCommandService {

    private static final String IMAGE_SK_PREFIX = "IMAGE#";
    private static final String S3_KEY_PATTERN = "inventory/products/%s/%s.%s";

    private final ProductRepository productRepository;
    private final S3Client s3Client;
    private final ServiceConfig serviceConfig;

    public void uploadImages(UUID productId, List<MultipartFile> files, UserContext userContext) {
        if (userContext == null) {
            throw new BusinessException("Authentication required", 401);
        }
        if (!userContext.hasRole(Role.SELLER) && !userContext.isAdmin()) {
            throw new BusinessException("Role '" + Role.SELLER + "' is required to perform this action", 403);
        }

        ProductEntity product = productRepository.findProductById(productId)
                .orElseThrow(() -> new BusinessException("Product not found: " + productId, 404));

        if (!product.getSellerId().equals(userContext.userId().toString()) && !userContext.isAdmin()) {
            throw new BusinessException("Only the product owner can upload images", 403);
        }

        List<ProductImageEntity> imageEntities = files.stream()
                .map(file -> uploadAndBuildEntity(productId, file))
                .toList();

        List<String> newKeys = imageEntities.stream()
                .map(ProductImageEntity::getS3Key)
                .toList();

        productRepository.saveImages(imageEntities);
        productRepository.appendImageKeys(productId, newKeys);

        log.info("Uploaded {} image(s) for productId={} sellerId={}",
                files.size(), productId, userContext.userId());
    }

    public void deleteImage(UUID productId, UUID imageId, UserContext userContext) {
        if (userContext == null) {
            throw new BusinessException("Authentication required", 401);
        }
        if (!userContext.hasRole(Role.SELLER) && !userContext.isAdmin()) {
            throw new BusinessException("Role '" + Role.SELLER + "' is required to perform this action", 403);
        }

        ProductEntity product = productRepository.findProductById(productId)
                .orElseThrow(() -> new BusinessException("Product not found: " + productId, 404));

        if (!product.getSellerId().equals(userContext.userId().toString()) && !userContext.isAdmin()) {
            throw new BusinessException("Only the product owner can delete images", 403);
        }

        ProductImageEntity image = productRepository.findImageById(productId, imageId)
                .orElseThrow(() -> new BusinessException("Image not found: " + imageId, 404));

        deleteFromS3(image.getS3Key());
        productRepository.deleteImage(productId, imageId);
        productRepository.removeImageKey(productId, image.getS3Key());

        log.info("Deleted imageId={} for productId={} sellerId={}",
                imageId, productId, userContext.userId());
    }

    private ProductImageEntity uploadAndBuildEntity(UUID productId, MultipartFile file) {
        UUID imageId = UUID.randomUUID();
        String extension = resolveExtension(file.getOriginalFilename(), file.getContentType());
        String s3Key = String.format(S3_KEY_PATTERN, productId, imageId, extension);

        uploadToS3(s3Key, file);

        ProductImageEntity entity = new ProductImageEntity();
        entity.setPk("PRODUCT#" + productId);
        entity.setSk(IMAGE_SK_PREFIX + imageId);
        entity.setImageId(imageId.toString());
        entity.setProductId(productId.toString());
        entity.setS3Key(s3Key);
        entity.setCreatedAt(Instant.now());
        return entity;
    }

    private void deleteFromS3(String s3Key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(serviceConfig.getAws().getS3().getBucketName())
                    .key(s3Key)
                    .build());
        } catch (SdkException ex) {
            throw new TechnicalException("Failed to delete image from S3: " + s3Key, ex);
        }
    }

    private void uploadToS3(String s3Key, MultipartFile file) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(serviceConfig.getAws().getS3().getBucketName())
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

        } catch (SdkException ex) {
            throw new TechnicalException("Failed to upload image to S3: " + s3Key, ex);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to read image file for upload", ex);
        }
    }

    private String resolveExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        }
        if (contentType != null) {
            return switch (contentType) {
                case "image/jpeg" -> "jpg";
                case "image/png" -> "png";
                case "image/webp" -> "webp";
                case "image/gif" -> "gif";
                default -> "bin";
            };
        }
        return "bin";
    }
}
