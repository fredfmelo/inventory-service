package com.fredfmelo.inventoryservice.outbox.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.fredfmelo.inventoryservice.config.ServiceConfig;
import com.fredfmelo.inventoryservice.outbox.entity.OutboxEntity;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@Repository
@RequiredArgsConstructor
public class OutboxRepository {

    private static final String INDEX_NAME = "outbox-status-created-at-index";
    private static final String PENDING_STATUS = "OUTBOX_STATUS#PENDING";

    private final DynamoDbEnhancedClient client;
    private final ServiceConfig serviceConfig;

    private DynamoDbTable<OutboxEntity> table() {
        return client.table(
                serviceConfig.getDynamodb().getTableName(),
                TableSchema.fromBean(OutboxEntity.class));
    }

    public void save(OutboxEntity entity) {
        table().putItem(entity);
    }

    public List<OutboxEntity> findPending() {
        DynamoDbIndex<OutboxEntity> index = table().index(INDEX_NAME);

        return index.query(request ->
                        request.queryConditional(QueryConditional.keyEqualTo(
                                        Key.builder().partitionValue(PENDING_STATUS).build())))
                .stream()
                .flatMap(page -> page.items().stream())
                .toList();
    }
}