package com.fredfmelo.inventoryservice.outbox.entity;

import java.time.Instant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Getter
@Setter
@NoArgsConstructor
@DynamoDbBean
public class OutboxEntity {

    private String pk;
    private String sk;

    private String eventType;
    private String payload;
    private String status;
    private Instant createdAt;

    private String outboxStatusPk;
    private String outboxCreatedAtSk;

    @DynamoDbPartitionKey
    public String getPk() {
        return pk;
    }

    @DynamoDbSortKey
    public String getSk() {
        return sk;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "outbox-status-created-at-index")
    public String getOutboxStatusPk() {
        return outboxStatusPk;
    }

    @DynamoDbSecondarySortKey(indexNames = "outbox-status-created-at-index")
    public String getOutboxCreatedAtSk() {
        return outboxCreatedAtSk;
    }
}