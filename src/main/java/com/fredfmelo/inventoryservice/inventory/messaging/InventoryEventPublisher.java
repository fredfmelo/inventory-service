package com.fredfmelo.inventoryservice.inventory.messaging;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fredfmelo.inventoryservice.config.ServiceConfig;
import com.fredfmelo.inventoryservice.inventory.event.InventoryReservedEvent;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Component
@RequiredArgsConstructor
public class InventoryEventPublisher {

    private final SnsClient snsClient;
    private final ServiceConfig config;
    private final ObjectMapper objectMapper;

    public void publish(InventoryReservedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            PublishRequest request = PublishRequest.builder()
                    .topicArn(config.getSns().getOrderTopicArn())
                    .message(payload)
                    .messageAttributes(Map.of(
                            "eventType",
                            MessageAttributeValue.builder()
                                    .dataType("String")
                                    .stringValue(event.eventType())
                                    .build()
                    ))
                    .build();

            snsClient.publish(request);

        } catch (Exception ex) {
            throw new RuntimeException("Failed to publish inventory event", ex);
        }
    }
}