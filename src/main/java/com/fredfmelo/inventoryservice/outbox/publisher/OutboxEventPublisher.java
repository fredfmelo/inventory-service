package com.fredfmelo.inventoryservice.outbox.publisher;

public interface OutboxEventPublisher {

    void publish(String payload, String eventType);
}