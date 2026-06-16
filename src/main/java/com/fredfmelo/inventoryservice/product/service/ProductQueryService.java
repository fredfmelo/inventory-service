package com.fredfmelo.inventoryservice.product.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fredfmelo.eventdrivencore.exception.BusinessException;
import com.fredfmelo.inventoryservice.api.model.GetProductResponse;
import com.fredfmelo.inventoryservice.api.model.ProductSummaryResponse;
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
public class ProductQueryService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public List<ProductSummaryResponse> getProducts(Boolean active) {
        return productRepository.findAll(active).stream()
                .map(product -> productMapper.toSummaryResponse(product, null))
                .toList();
    }

    public List<ProductSummaryResponse> getMyProducts(UserContext userContext) {
        return productRepository.findBySellerId(userContext.userId()).stream()
                .map(product -> productMapper.toSummaryResponse(product, null))
                .toList();
    }

    public GetProductResponse getProductById(UUID productId) {
        ProductEntity product = productRepository.findProductById(productId)
                .orElseThrow(() -> new BusinessException("Product not found: " + productId, 404));

        InventoryEntity inventory = productRepository.findInventoryById(productId)
                .orElseThrow(() -> new BusinessException("Inventory not found for product: " + productId, 404));

        return productMapper.toGetResponse(product, inventory);
    }
}
