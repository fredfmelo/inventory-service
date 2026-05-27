package com.fredfmelo.inventoryservice.inventory.messaging;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fredfmelo.inventoryservice.common.exception.TechnicalException;
import com.fredfmelo.inventoryservice.config.ServiceConfig;
import com.fredfmelo.inventoryservice.outbox.publisher.OutboxEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryEventPublisher implements OutboxEventPublisher{

    private static final String EVENT_TYPE = "eventType";
    private static final String DATA_TYPE_STRING = "String";

    private final SnsClient snsClient;
    private final ServiceConfig config;

    @Override
    public void publish(String payload, String eventType) {
        try {
            PublishRequest request = PublishRequest.builder()
                    .topicArn(config.getSns().getOrderTopicArn())
                    .message(payload)
                    .messageAttributes(buildAttributes(eventType))
                    .build();

            snsClient.publish(request);

            log.info("Published eventType={}", eventType);

        } catch (SdkException ex) {
            throw new TechnicalException("Error publishing event", ex);
        }
    }

    private Map<String, MessageAttributeValue> buildAttributes(String eventType) {
        return Map.of(EVENT_TYPE,
                MessageAttributeValue.builder()
                        .dataType(DATA_TYPE_STRING)
                        .stringValue(eventType)
                        .build());
    }
}