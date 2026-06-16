package com.fredfmelo.inventoryservice.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fredfmelo.inventoryservice.api.model.CreateProductRequest;
import com.fredfmelo.inventoryservice.api.model.CreateProductResponse;
import com.fredfmelo.inventoryservice.api.model.GetProductResponse;
import com.fredfmelo.inventoryservice.api.model.ProductSummaryResponse;
import com.fredfmelo.inventoryservice.api.model.UpdateInventoryRequest;
import com.fredfmelo.inventoryservice.api.model.UpdateProductRequest;
import com.fredfmelo.inventoryservice.product.service.ProductCommandService;
import com.fredfmelo.inventoryservice.product.service.ProductQueryService;
import com.fredfmelo.inventoryservice.security.UserContext;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductCommandService productCommandService;
    private final ProductQueryService productQueryService;

    @PostMapping
    public ResponseEntity<CreateProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal UserContext userContext) {

        CreateProductResponse response = productCommandService.createProduct(request, userContext);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProductSummaryResponse>> getProducts(
            @RequestParam(required = false) Boolean active) {

        return ResponseEntity.ok(productQueryService.getProducts(active));
    }

    @GetMapping("/me")
    public ResponseEntity<List<ProductSummaryResponse>> getMyProducts(
            @AuthenticationPrincipal UserContext userContext) {

        return ResponseEntity.ok(productQueryService.getMyProducts(userContext));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<GetProductResponse> getProductById(
            @PathVariable UUID productId) {

        return ResponseEntity.ok(productQueryService.getProductById(productId));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<Void> updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductRequest request,
            @AuthenticationPrincipal UserContext userContext) {

        productCommandService.updateProduct(productId, request, userContext);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deactivateProduct(
            @PathVariable UUID productId,
            @AuthenticationPrincipal UserContext userContext) {

        productCommandService.deactivateProduct(productId, userContext);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{productId}/inventory")
    public ResponseEntity<Void> updateInventory(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateInventoryRequest request,
            @AuthenticationPrincipal UserContext userContext) {

        productCommandService.updateInventory(productId, request, userContext);
        return ResponseEntity.ok().build();
    }
}
