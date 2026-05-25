package com.fredfmelo.inventoryservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aws")
public class ServiceConfig {

    private DynamoDb dynamodb;
    private Sns sns;
    private Sqs sqs;

    @Getter
    @Setter
    public static class DynamoDb {
        private String tableName;
    }

    @Getter
    @Setter
    public static class Sns {
        private String orderTopicArn;
    }

    @Getter
    @Setter
    public static class Sqs {
        private String inventoryQueue;
    }
}