package com.fredfmelo.inventoryservice.inventory.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fredfmelo.eventdrivencore.exception.BusinessException;
import com.fredfmelo.eventdrivencore.outbox.service.OutboxService;
import com.fredfmelo.inventoryservice.inventory.event.InventoryReservedEvent;
import com.fredfmelo.inventoryservice.inventory.event.PaymentApprovedEvent;
import com.fredfmelo.inventoryservice.product.domain.ProductEntity;
import com.fredfmelo.inventoryservice.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final OutboxService outboxService;
    private final ProductRepository productRepository;

    public void reserve(PaymentApprovedEvent paymentApprovedEvent) {
        log.info("Reserving inventory for orderId={}", paymentApprovedEvent.orderId());

        UUID productId = UUID.fromString(paymentApprovedEvent.orderId());

        ProductEntity product = productRepository.findProductById(productId)
                .orElseThrow(() -> new BusinessException("Product not found: " + productId, 404));

        if (!"ACTIVE".equals(product.getStatus())) {
            throw new BusinessException("Product is not available for reservation: " + productId, 422);
        }

        boolean reserved = productRepository.reserveInventory(productId, 1);

        if (!reserved) {
            throw new BusinessException("Insufficient inventory for product: " + productId, 422);
        }

        InventoryReservedEvent inventoryReservedEvent = new InventoryReservedEvent(
                UUID.randomUUID(),
                paymentApprovedEvent.traceId(),
                "INVENTORY_RESERVED",
                Instant.now(),
                paymentApprovedEvent.orderId());

        outboxService.save(inventoryReservedEvent);

        log.info("Inventory reserved orderId={} productId={}", paymentApprovedEvent.orderId(), productId);
    }
}
