package com.fredfmelo.inventoryservice.inventory.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fredfmelo.inventoryservice.inventory.event.InventoryReservedEvent;
import com.fredfmelo.inventoryservice.inventory.event.PaymentApprovedEvent;
import com.fredfmelo.inventoryservice.inventory.messaging.InventoryEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryEventPublisher publisher;

    public void reserve(PaymentApprovedEvent event) {
        log.info("Reserving inventory order={}",event.orderId());

        InventoryReservedEvent reserved =new InventoryReservedEvent(
                        UUID.randomUUID(),
                        "INVENTORY_RESERVED",
                        Instant.now(),
                        event.orderId());

        log.info("Inventory reserved {}", reserved);

        publisher.publish(reserved);

        log.info("Inventory reserved {}", reserved);
    }
}