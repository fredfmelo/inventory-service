package com.fredfmelo.inventoryservice.product.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fredfmelo.eventdrivencore.exception.BusinessException;
import com.fredfmelo.inventoryservice.api.model.CreateProductRequest;
import com.fredfmelo.inventoryservice.api.model.CreateProductResponse;
import com.fredfmelo.inventoryservice.api.model.UpdateInventoryRequest;
import com.fredfmelo.inventoryservice.api.model.UpdateProductRequest;
import com.fredfmelo.inventoryservice.product.domain.InventoryEntity;
import com.fredfmelo.inventoryservice.product.domain.ProductEntity;
import com.fredfmelo.inventoryservice.product.mapper.ProductMapper;
import com.fredfmelo.inventoryservice.product.repository.ProductRepository;
import com.fredfmelo.inventoryservice.security.UserContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCommandService {

    private static final String PRODUCT_PREFIX = "PRODUCT#";
    private static final String METADATA_SK = "METADATA";
    private static final String INVENTORY_SK = "INVENTORY";

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public CreateProductResponse createProduct(CreateProductRequest request, UserContext userContext) {
        UUID productId = UUID.randomUUID();
        Instant now = Instant.now();

        ProductEntity product = buildProductEntity(productId, request, userContext.userId(), now);
        InventoryEntity inventory = buildInventoryEntity(productId, request.getAvailableQuantity(), now);

        productRepository.saveProductWithInventory(product, inventory);

        log.info("Product created productId={} sellerId={}", productId, userContext.userId());

        return productMapper.toCreateResponse(product);
    }

    public void updateProduct(UUID productId, UpdateProductRequest request, UserContext userContext) {
        ProductEntity product = findProductOrThrow(productId);
        verifyOwnership(product, userContext);

        product.setTitle(request.getTitle());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice() != null ? BigDecimal.valueOf(request.getPrice()) : product.getPrice());
        product.setImageKeys(request.getImageKeys());
        product.setUpdatedAt(Instant.now());

        productRepository.updateProduct(product);

        log.info("Product updated productId={} sellerId={}", productId, userContext.userId());
    }

    public void deactivateProduct(UUID productId, UserContext userContext) {
        ProductEntity product = findProductOrThrow(productId);
        verifyOwnership(product, userContext);

        product.setStatus("INACTIVE");
        product.setUpdatedAt(Instant.now());

        productRepository.updateProduct(product);

        log.info("Product deactivated productId={} sellerId={}", productId, userContext.userId());
    }

    public void updateInventory(UUID productId, UpdateInventoryRequest request, UserContext userContext) {
        ProductEntity product = findProductOrThrow(productId);
        verifyOwnership(product, userContext);

        int availableQuantity = request.getAvailableQuantity();

        product.setAvailableQuantity(availableQuantity);
        product.setUpdatedAt(Instant.now());
        productRepository.updateProduct(product);

        productRepository.updateInventoryAvailableQuantity(productId, availableQuantity);

        log.info("Inventory updated productId={} availableQuantity={} sellerId={}",
                productId, availableQuantity, userContext.userId());
    }

    private ProductEntity findProductOrThrow(UUID productId) {
        return productRepository.findProductById(productId)
                .orElseThrow(() -> new BusinessException("Product not found: " + productId, 404));
    }

    private void verifyOwnership(ProductEntity product, UserContext userContext) {
        if (!product.getSellerId().equals(userContext.userId().toString()) && !userContext.isAdmin()) {
            throw new BusinessException("Only the product owner can perform this action", 403);
        }
    }

    private ProductEntity buildProductEntity(UUID productId, CreateProductRequest request,
                                             UUID sellerId, Instant now) {
        ProductEntity entity = new ProductEntity();
        entity.setPk(PRODUCT_PREFIX + productId);
        entity.setSk(METADATA_SK);
        entity.setProductId(productId.toString());
        entity.setSellerId(sellerId.toString());
        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setPrice(request.getPrice() != null ? BigDecimal.valueOf(request.getPrice()) : null);
        entity.setStatus("ACTIVE");
        entity.setImageKeys(request.getImageKeys() != null ? request.getImageKeys() : List.of());
        entity.setAvailableQuantity(request.getAvailableQuantity() != null ? request.getAvailableQuantity() : 0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    private InventoryEntity buildInventoryEntity(UUID productId, Integer availableQuantity, Instant now) {
        InventoryEntity entity = new InventoryEntity();
        entity.setPk(PRODUCT_PREFIX + productId);
        entity.setSk(INVENTORY_SK);
        entity.setProductId(productId.toString());
        entity.setAvailableQuantity(availableQuantity != null ? availableQuantity : 0);
        entity.setReservedQuantity(0);
        return entity;
    }
}
