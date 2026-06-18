package com.fredfmelo.inventoryservice.product.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.fredfmelo.eventdrivencore.exception.TechnicalException;
import com.fredfmelo.inventoryservice.config.ServiceConfig;
import com.fredfmelo.inventoryservice.product.domain.InventoryEntity;
import com.fredfmelo.inventoryservice.product.domain.ProductEntity;
import com.fredfmelo.inventoryservice.product.domain.ProductImageEntity;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@Slf4j
@Repository
public class ProductRepository {

    private static final String METADATA_SK = "METADATA";
    private static final String INVENTORY_SK = "INVENTORY";
    private static final String IMAGE_SK_PREFIX = "IMAGE#";
    private static final String PRODUCT_PREFIX = "PRODUCT#";
    private static final String SELLER_PRODUCTS_INDEX = "seller-products-index";

    private final DynamoDbTable<ProductEntity> productTable;
    private final DynamoDbTable<InventoryEntity> inventoryTable;
    private final DynamoDbTable<ProductImageEntity> imageTable;
    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public ProductRepository(DynamoDbEnhancedClient enhancedClient,
                             DynamoDbClient dynamoDbClient,
                             ServiceConfig serviceConfig) {
        this.enhancedClient = enhancedClient;
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = serviceConfig.tableName();
        this.productTable = enhancedClient.table(tableName, TableSchema.fromBean(ProductEntity.class));
        this.inventoryTable = enhancedClient.table(tableName, TableSchema.fromBean(InventoryEntity.class));
        this.imageTable = enhancedClient.table(tableName, TableSchema.fromBean(ProductImageEntity.class));
    }

    public void saveProductWithInventory(ProductEntity product, InventoryEntity inventory) {
        try {
            enhancedClient.transactWriteItems(TransactWriteItemsEnhancedRequest.builder()
                    .addPutItem(productTable, product)
                    .addPutItem(inventoryTable, inventory)
                    .build());
        } catch (SdkException ex) {
            throw new TechnicalException("Failed to save product with inventory", ex);
        }
    }

    public Optional<ProductEntity> findProductById(UUID productId) {
        try {
            Key key = Key.builder()
                    .partitionValue(PRODUCT_PREFIX + productId)
                    .sortValue(METADATA_SK)
                    .build();
            return Optional.ofNullable(productTable.getItem(r -> r.key(key)));
        } catch (SdkException ex) {
            throw new TechnicalException("Failed to find product by id", ex);
        }
    }

    public Optional<InventoryEntity> findInventoryById(UUID productId) {
        try {
            Key key = Key.builder()
                    .partitionValue(PRODUCT_PREFIX + productId)
                    .sortValue(INVENTORY_SK)
                    .build();
            return Optional.ofNullable(inventoryTable.getItem(r -> r.key(key)));
        } catch (SdkException ex) {
            throw new TechnicalException("Failed to find inventory by product id", ex);
        }
    }

    public List<ProductEntity> findAll(UUID sellerId, Boolean active) {
        if (sellerId != null) {
            return findBySellerId(sellerId, active);
        }
        try {
            Expression filterExpression = buildMarketplaceFilter(active);

            ScanEnhancedRequest request = ScanEnhancedRequest.builder()
                    .filterExpression(filterExpression)
                    .build();

            List<ProductEntity> products = new ArrayList<>();
            productTable.scan(request).items().forEach(products::add);
            return products;

        } catch (SdkException ex) {
            throw new TechnicalException("Failed to list products", ex);
        }
    }

    public List<ProductEntity> findBySellerId(UUID sellerId) {
        return findBySellerId(sellerId, null);
    }

    private List<ProductEntity> findBySellerId(UUID sellerId, Boolean active) {
        try {
            DynamoDbIndex<ProductEntity> sellerIndex = productTable.index(SELLER_PRODUCTS_INDEX);

            QueryEnhancedRequest.Builder requestBuilder = QueryEnhancedRequest.builder()
                    .queryConditional(QueryConditional.keyEqualTo(
                            Key.builder().partitionValue(sellerId.toString()).build()));

            if (active != null) {
                requestBuilder.filterExpression(Expression.builder()
                        .expression("#status = :status")
                        .putExpressionName("#status", "status")
                        .putExpressionValue(":status", AttributeValue.fromS(active ? "ACTIVE" : "INACTIVE"))
                        .build());
            }

            List<ProductEntity> products = new ArrayList<>();
            sellerIndex.query(requestBuilder.build()).stream()
                    .flatMap(page -> page.items().stream())
                    .forEach(products::add);
            return products;

        } catch (SdkException ex) {
            throw new TechnicalException("Failed to list products by seller", ex);
        }
    }

    public void updateProduct(ProductEntity product) {
        try {
            productTable.updateItem(product);
        } catch (SdkException ex) {
            throw new TechnicalException("Failed to update product", ex);
        }
    }

    public void updateInventoryAvailableQuantity(UUID productId, int availableQuantity) {
        try {
            dynamoDbClient.updateItem(UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of(
                            "pk", AttributeValue.fromS(PRODUCT_PREFIX + productId),
                            "sk", AttributeValue.fromS(INVENTORY_SK)))
                    .updateExpression("SET availableQuantity = :qty")
                    .expressionAttributeValues(Map.of(
                            ":qty", AttributeValue.fromN(String.valueOf(availableQuantity))))
                    .build());
        } catch (SdkException ex) {
            throw new TechnicalException("Failed to update inventory", ex);
        }
    }

    public boolean reserveInventory(UUID productId, int quantity) {
        try {
            String pk = PRODUCT_PREFIX + productId;

            UpdateItemRequest request = UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of(
                            "pk", AttributeValue.fromS(pk),
                            "sk", AttributeValue.fromS(INVENTORY_SK)))
                    .updateExpression(
                            "SET availableQuantity = availableQuantity - :qty, "
                            + "reservedQuantity = reservedQuantity + :qty")
                    .conditionExpression("availableQuantity >= :qty")
                    .expressionAttributeValues(Map.of(
                            ":qty", AttributeValue.fromN(String.valueOf(quantity))))
                    .build();

            dynamoDbClient.updateItem(request);
            return true;

        } catch (ConditionalCheckFailedException ex) {
            log.warn("Insufficient inventory for product={}", productId);
            return false;
        } catch (SdkException ex) {
            throw new TechnicalException("Failed to reserve inventory", ex);
        }
    }

    public void saveImages(List<ProductImageEntity> images) {
        try {
            var writeRequest = TransactWriteItemsEnhancedRequest.builder();
            images.forEach(image -> writeRequest.addPutItem(imageTable, image));
            enhancedClient.transactWriteItems(writeRequest.build());
        } catch (SdkException ex) {
            throw new TechnicalException("Failed to save product images", ex);
        }
    }

    public void appendImageKeys(UUID productId, List<String> newKeys) {
        try {
            List<AttributeValue> keyValues = newKeys.stream()
                    .map(AttributeValue::fromS)
                    .toList();

            dynamoDbClient.updateItem(UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of(
                            "pk", AttributeValue.fromS(PRODUCT_PREFIX + productId),
                            "sk", AttributeValue.fromS(METADATA_SK)))
                    .updateExpression(
                            "SET imageKeys = list_append(if_not_exists(imageKeys, :empty), :newKeys)")
                    .expressionAttributeValues(Map.of(
                            ":empty", AttributeValue.fromL(List.of()),
                            ":newKeys", AttributeValue.fromL(keyValues)))
                    .build());
        } catch (SdkException ex) {
            throw new TechnicalException("Failed to append image keys to product", ex);
        }
    }

    public List<ProductImageEntity> findImagesByProductId(UUID productId) {
        try {
            QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                    .queryConditional(QueryConditional.sortBeginsWith(Key.builder()
                            .partitionValue(PRODUCT_PREFIX + productId)
                            .sortValue(IMAGE_SK_PREFIX)
                            .build()))
                    .build();

            List<ProductImageEntity> images = new ArrayList<>();
            imageTable.query(request).stream()
                    .flatMap(page -> page.items().stream())
                    .forEach(images::add);
            return images;

        } catch (SdkException ex) {
            throw new TechnicalException("Failed to find images by product id", ex);
        }
    }

    public Optional<ProductImageEntity> findImageById(UUID productId, UUID imageId) {
        try {
            Key key = Key.builder()
                    .partitionValue(PRODUCT_PREFIX + productId)
                    .sortValue(IMAGE_SK_PREFIX + imageId)
                    .build();
            return Optional.ofNullable(imageTable.getItem(r -> r.key(key)));
        } catch (SdkException ex) {
            throw new TechnicalException("Failed to find image by id", ex);
        }
    }

    public void deleteImage(UUID productId, UUID imageId) {
        try {
            Key key = Key.builder()
                    .partitionValue(PRODUCT_PREFIX + productId)
                    .sortValue(IMAGE_SK_PREFIX + imageId)
                    .build();
            imageTable.deleteItem(r -> r.key(key));
        } catch (SdkException ex) {
            throw new TechnicalException("Failed to delete product image", ex);
        }
    }

    public void removeImageKey(UUID productId, String s3Key) {
        try {
            ProductEntity product = findProductById(productId)
                    .orElseThrow(() -> new TechnicalException("Product not found: " + productId));

            List<String> imageKeys = product.getImageKeys();
            if (imageKeys == null || imageKeys.isEmpty()) {
                return;
            }

            List<AttributeValue> updatedKeys = imageKeys.stream()
                    .filter(key -> !key.equals(s3Key))
                    .map(AttributeValue::fromS)
                    .toList();

            dynamoDbClient.updateItem(UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of(
                            "pk", AttributeValue.fromS(PRODUCT_PREFIX + productId),
                            "sk", AttributeValue.fromS(METADATA_SK)))
                    .updateExpression("SET imageKeys = :keys")
                    .expressionAttributeValues(Map.of(
                            ":keys", AttributeValue.fromL(updatedKeys)))
                    .build());
        } catch (SdkException ex) {
            throw new TechnicalException("Failed to remove image key from product", ex);
        }
    }

    private Expression buildMarketplaceFilter(Boolean active) {
        if (active == null) {
            return Expression.builder()
                    .expression("#sk = :sk AND #status = :status")
                    .putExpressionName("#sk", "sk")
                    .putExpressionName("#status", "status")
                    .putExpressionValue(":sk", AttributeValue.fromS(METADATA_SK))
                    .putExpressionValue(":status", AttributeValue.fromS("ACTIVE"))
                    .build();
        }

        return Expression.builder()
                .expression("#sk = :sk AND #status = :status")
                .putExpressionName("#sk", "sk")
                .putExpressionName("#status", "status")
                .putExpressionValue(":sk", AttributeValue.fromS(METADATA_SK))
                .putExpressionValue(":status", AttributeValue.fromS(active ? "ACTIVE" : "INACTIVE"))
                .build();
    }
}
