package com.fredfmelo.inventoryservice.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.multipart.MultipartFile;

import com.fredfmelo.inventoryservice.api.ProductsApi;
import com.fredfmelo.inventoryservice.api.model.CreateProductRequest;
import com.fredfmelo.inventoryservice.api.model.CreateProductResponse;
import com.fredfmelo.inventoryservice.api.model.GetProductResponse;
import com.fredfmelo.inventoryservice.api.model.ProductSummaryResponse;
import com.fredfmelo.inventoryservice.api.model.UpdateInventoryRequest;
import com.fredfmelo.inventoryservice.api.model.UpdateProductRequest;
import com.fredfmelo.inventoryservice.product.service.ProductCommandService;
import com.fredfmelo.inventoryservice.product.service.ProductImageCommandService;
import com.fredfmelo.inventoryservice.product.service.ProductQueryService;
import com.fredfmelo.inventoryservice.security.UserContext;
import com.fredfmelo.inventoryservice.security.UserContextExtractor;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ProductController implements ProductsApi {

    private final ProductCommandService productCommandService;
    private final ProductImageCommandService productImageCommandService;
    private final ProductQueryService productQueryService;
    private final UserContextExtractor userContextExtractor;
    private final NativeWebRequest nativeWebRequest;

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.of(nativeWebRequest);
    }

    @Override
    public ResponseEntity<CreateProductResponse> createProduct(CreateProductRequest createProductRequest) {
        UserContext userContext = userContextExtractor.extract(nativeWebRequest);
        CreateProductResponse response = productCommandService.createProduct(createProductRequest, userContext);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<List<ProductSummaryResponse>> getProducts(Boolean active, UUID sellerId) {
        return ResponseEntity.ok(productQueryService.getProducts(sellerId, active));
    }

    @Override
    public ResponseEntity<List<ProductSummaryResponse>> getMyProducts() {
        UserContext userContext = userContextExtractor.extract(nativeWebRequest);
        return ResponseEntity.ok(productQueryService.getMyProducts(userContext));
    }

    @Override
    public ResponseEntity<GetProductResponse> getProductById(UUID productId) {
        return ResponseEntity.ok(productQueryService.getProductById(productId));
    }

    @Override
    public ResponseEntity<Void> updateProduct(UUID productId, UpdateProductRequest updateProductRequest) {
        UserContext userContext = userContextExtractor.extract(nativeWebRequest);
        productCommandService.updateProduct(productId, updateProductRequest, userContext);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> deactivateProduct(UUID productId) {
        UserContext userContext = userContextExtractor.extract(nativeWebRequest);
        productCommandService.deactivateProduct(productId, userContext);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> uploadProductImages(UUID productId, List<MultipartFile> files) {
        UserContext userContext = userContextExtractor.extract(nativeWebRequest);
        productImageCommandService.uploadImages(productId, files, userContext);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<Void> updateInventory(UUID productId, UpdateInventoryRequest updateInventoryRequest) {
        UserContext userContext = userContextExtractor.extract(nativeWebRequest);
        productCommandService.updateInventory(productId, updateInventoryRequest, userContext);
        return ResponseEntity.ok().build();
    }
}
