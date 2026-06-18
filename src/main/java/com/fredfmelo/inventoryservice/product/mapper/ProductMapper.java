package com.fredfmelo.inventoryservice.product.mapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.fredfmelo.inventoryservice.api.model.CreateProductResponse;
import com.fredfmelo.inventoryservice.api.model.GetProductResponse;
import com.fredfmelo.inventoryservice.api.model.ProductStatus;
import com.fredfmelo.inventoryservice.api.model.ProductSummaryResponse;
import com.fredfmelo.inventoryservice.product.domain.InventoryEntity;
import com.fredfmelo.inventoryservice.product.domain.ProductEntity;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(source = "product.productId", target = "productId")
    @Mapping(source = "product.sellerId", target = "sellerId")
    @Mapping(source = "product.title", target = "title")
    @Mapping(source = "product.description", target = "description")
    @Mapping(source = "product.price", target = "price")
    @Mapping(source = "product.availableQuantity", target = "availableQuantity")
    @Mapping(target = "images", ignore = true)
    @Mapping(source = "product.status", target = "status")
    @Mapping(source = "product.createdAt", target = "createdAt")
    @Mapping(source = "product.updatedAt", target = "updatedAt")
    ProductSummaryResponse toSummaryResponse(ProductEntity product, InventoryEntity inventory);

    @Mapping(source = "product.productId", target = "productId")
    @Mapping(source = "product.sellerId", target = "sellerId")
    @Mapping(source = "product.title", target = "title")
    @Mapping(source = "product.description", target = "description")
    @Mapping(source = "product.price", target = "price")
    @Mapping(source = "product.availableQuantity", target = "availableQuantity")
    @Mapping(source = "inventory.reservedQuantity", target = "reservedQuantity")
    @Mapping(target = "images", ignore = true)
    @Mapping(source = "product.status", target = "status")
    @Mapping(source = "product.createdAt", target = "createdAt")
    @Mapping(source = "product.updatedAt", target = "updatedAt")
    GetProductResponse toGetResponse(ProductEntity product, InventoryEntity inventory);

    @Mapping(source = "productId", target = "productId")
    @Mapping(source = "status", target = "status")
    CreateProductResponse toCreateResponse(ProductEntity product);

    default ProductStatus mapStatus(String status) {
        if (status == null) {
            return null;
        }
        return ProductStatus.valueOf(status);
    }

    default OffsetDateTime mapInstant(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atOffset(ZoneOffset.UTC);
    }
}
