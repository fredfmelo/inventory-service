package com.fredfmelo.inventoryservice.inventory.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fredfmelo.eventdrivencore.outbox.service.OutboxService;
import com.fredfmelo.inventoryservice.inventory.event.InventoryReservedEvent;
import com.fredfmelo.inventoryservice.inventory.event.PaymentApprovedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final OutboxService outboxService;

    public void reserve(PaymentApprovedEvent paymentApprovedEvent) {
        log.info("Reserving inventory order={}", paymentApprovedEvent.orderId());

        InventoryReservedEvent inventoryReservedEvent = new InventoryReservedEvent(UUID.randomUUID(),
                paymentApprovedEvent.traceId(),
                "INVENTORY_RESERVED",
                Instant.now(),
                paymentApprovedEvent.orderId());

        simulateInventory();

        // TODO: when the real inventory structure is define, replace this save with a transactionalRepository that saves the business and outbox entity in the same transaction
        outboxService.save(inventoryReservedEvent);
    }

    private void simulateInventory() {
        log.info("[BUSINESS-FLOW-PLACEHOLDER] Simulating inventory...");
    }
}