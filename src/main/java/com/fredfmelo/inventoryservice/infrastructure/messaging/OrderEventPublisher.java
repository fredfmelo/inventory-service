package com.fredfmelo.inventoryservice.infrastructure.messaging;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fredfmelo.inventoryservice.config.ServiceConfig;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final SnsClient snsClient;

    private final ObjectMapper mapper;

    private final ServiceConfig config;

    public void publish(Object event) {

        try {
            PublishRequest request = PublishRequest
                    .builder()
                    .topicArn(config.getSns().getOrderTopicArn())
                    .message(mapper.writeValueAsString(event))
                    .messageAttributes(
                            Map.of("eventType",
                                    MessageAttributeValue.builder()
                                            .dataType("String")
                                           // .stringValue(event.eventType())
                                            .build()))
                    .build();
            snsClient.publish(request);
        } catch (JsonProcessingException | SdkException ex) {
            throw new RuntimeException(ex);
        }
    }
}