package com.fredfmelo.inventoryservice.inventory.event;

import java.time.Instant;
import java.util.UUID;

import com.fredfmelo.eventdrivencore.event.Event;

public record PaymentApprovedEvent(UUID eventId,
        String traceId,
        String eventType,
        Instant occurredAt,
        String orderId) implements Event {
}