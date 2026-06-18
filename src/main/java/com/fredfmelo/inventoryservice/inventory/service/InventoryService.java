package com.fredfmelo.inventoryservice.inventory.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fredfmelo.eventdrivencore.exception.BusinessException;
import com.fredfmelo.eventdrivencore.outbox.service.OutboxService;
import com.fredfmelo.inventoryservice.inventory.event.InventoryReservedEvent;
import com.fredfmelo.inventoryservice.inventory.event.OrderItem;
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

        List<OrderItem> items = paymentApprovedEvent.items();
        if (items == null || items.isEmpty()) {
            throw new BusinessException("Order has no items to reserve", 422);
        }

        Map<UUID, Integer> itemsByProductId = mergeItemsByProduct(items);

        for (Map.Entry<UUID, Integer> entry : itemsByProductId.entrySet()) {
            validateProductForReservation(entry.getKey(), entry.getValue());
        }

        boolean reserved = productRepository.reserveOrderItems(itemsByProductId);
        if (!reserved) {
            throw new BusinessException("Insufficient inventory for order: " + paymentApprovedEvent.orderId(), 422);
        }

        InventoryReservedEvent inventoryReservedEvent = new InventoryReservedEvent(
                UUID.randomUUID(),
                paymentApprovedEvent.traceId(),
                "INVENTORY_RESERVED",
                Instant.now(),
                paymentApprovedEvent.orderId());

        outboxService.save(inventoryReservedEvent);

        log.info("Inventory reserved orderId={} products={}", paymentApprovedEvent.orderId(), itemsByProductId.size());
    }

    private Map<UUID, Integer> mergeItemsByProduct(List<OrderItem> items) {
        Map<UUID, Integer> merged = new HashMap<>();
        for (OrderItem item : items) {
            if (item.quantity() <= 0) {
                throw new BusinessException("Item quantity must be greater than zero", 422);
            }
            UUID productId = UUID.fromString(item.productId());
            merged.merge(productId, item.quantity(), Integer::sum);
        }
        return merged;
    }

    private void validateProductForReservation(UUID productId, int quantity) {
        ProductEntity product = productRepository.findProductById(productId)
                .orElseThrow(() -> new BusinessException("Product not found: " + productId, 404));

        if (!"ACTIVE".equals(product.getStatus())) {
            throw new BusinessException("Product is not available for reservation: " + productId, 422);
        }

        log.debug("Validated productId={} quantity={} for reservation", productId, quantity);
    }
}
