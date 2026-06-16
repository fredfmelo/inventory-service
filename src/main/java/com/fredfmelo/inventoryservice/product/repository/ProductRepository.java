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
    private static final String PRODUCT_PREFIX = "PRODUCT#";
    private static final String SELLER_PRODUCTS_INDEX = "seller-products-index";

    private final DynamoDbTable<ProductEntity> productTable;
    private final DynamoDbTable<InventoryEntity> inventoryTable;
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
            return Optional.ofNullable(productTable.getItem(key));
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
            return Optional.ofNullable(inventoryTable.getItem(key));
        } catch (SdkException ex) {
            throw new TechnicalException("Failed to find inventory by product id", ex);
        }
    }

    public List<ProductEntity> findAll(Boolean active) {
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
        try {
            DynamoDbIndex<ProductEntity> sellerIndex = productTable.index(SELLER_PRODUCTS_INDEX);

            QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                    .queryConditional(QueryConditional.keyEqualTo(
                            Key.builder().partitionValue(sellerId.toString()).build()))
                    .build();

            List<ProductEntity> products = new ArrayList<>();
            sellerIndex.query(request).stream()
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
            InventoryEntity inventory = new InventoryEntity();
            inventory.setPk(PRODUCT_PREFIX + productId);
            inventory.setSk(INVENTORY_SK);
            inventory.setProductId(productId.toString());
            inventory.setAvailableQuantity(availableQuantity);

            inventoryTable.updateItem(UpdateItemEnhancedRequest.builder(InventoryEntity.class)
                    .item(inventory)
                    .ignoreNulls(true)
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
                            "PK", AttributeValue.fromS(pk),
                            "SK", AttributeValue.fromS(INVENTORY_SK)))
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

    private Expression buildMarketplaceFilter(Boolean active) {
        if (active == null) {
            return Expression.builder()
                    .expression("#sk = :sk AND #status = :status")
                    .putExpressionName("#sk", "SK")
                    .putExpressionName("#status", "status")
                    .putExpressionValue(":sk", AttributeValue.fromS(METADATA_SK))
                    .putExpressionValue(":status", AttributeValue.fromS("ACTIVE"))
                    .build();
        }

        return Expression.builder()
                .expression("#sk = :sk AND #status = :status")
                .putExpressionName("#sk", "SK")
                .putExpressionName("#status", "status")
                .putExpressionValue(":sk", AttributeValue.fromS(METADATA_SK))
                .putExpressionValue(":status", AttributeValue.fromS(active ? "ACTIVE" : "INACTIVE"))
                .build();
    }
}
