package com.fredfmelo.inventoryservice.idempotency.repository;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.fredfmelo.inventoryservice.config.ServiceConfig;
import com.fredfmelo.inventoryservice.idempotency.entity.IdempotencyEntity;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

@Repository
@RequiredArgsConstructor
public class IdempotencyRepository {

    private final ServiceConfig serviceConfig;
    private final DynamoDbEnhancedClient client;

    private DynamoDbTable<IdempotencyEntity> table() {
        return client.table(
                serviceConfig.getDynamodb().getTableName(),
                TableSchema.fromBean(IdempotencyEntity.class));
    }

    /*
     * Answers the question:
     * Can I process this message?
     *
     * This is the heart of the idempotency flow.
     * Its purpose is to atomically acquire ownership of an event.
     *
     * Save the entity only if it does not already exist.
     *
     * If successful, this thread owns the event processing.
     * Concurrent threads processing the same event will fail.
     */
    public boolean acquire(IdempotencyEntity entity) {
        try {
            PutItemEnhancedRequest<IdempotencyEntity> request = PutItemEnhancedRequest.builder(
                    IdempotencyEntity.class)
                    .item(entity)
                    .conditionExpression(Expression.builder().expression("attribute_not_exists(pk)").build())
                    .build();

            table().putItem(request);

            return true;

        } catch (ConditionalCheckFailedException ex) {
            return false;
        }
    }

    public void save(IdempotencyEntity entity) {
        table().putItem(entity);
    }

    public IdempotencyEntity find(UUID eventId) {

        return table().getItem(
                Key.builder().partitionValue("IDEMPOTENCY#" + eventId)
                        .sortValue("METADATA")
                        .build());
    }
}