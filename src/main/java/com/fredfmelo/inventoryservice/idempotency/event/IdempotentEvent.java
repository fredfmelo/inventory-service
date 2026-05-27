package com.fredfmelo.inventoryservice.idempotency.event;

import java.util.UUID;

public interface IdempotentEvent {
    UUID eventId();
}
