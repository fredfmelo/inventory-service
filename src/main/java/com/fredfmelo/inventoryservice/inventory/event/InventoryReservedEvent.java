package com.fredfmelo.inventoryservice.inventory.event;

import java.time.Instant;
import java.util.UUID;

public record InventoryReservedEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String orderId
) {
}