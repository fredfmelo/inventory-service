package com.fredfmelo.inventoryservice.inventory.event;

import java.time.Instant;
import java.util.UUID;

import com.fredfmelo.inventoryservice.idempotency.event.IdempotentEvent;

public record PaymentApprovedEvent(UUID eventId,
                String eventType,
                Instant occurredAt,
                String orderId) implements IdempotentEvent {
}