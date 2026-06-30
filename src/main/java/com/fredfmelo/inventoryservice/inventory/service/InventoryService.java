package com.fredfmelo.inventoryservice.inventory.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fredfmelo.eventdrivencore.exception.BusinessException;
import com.fredfmelo.eventdrivencore.outbox.service.OutboxService;
import com.fredfmelo.inventoryservice.inventory.event.InventoryReservedEvent;
import com.fredfmelo.inventoryservice.inventory.event.InventoryUnavailableEvent;
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

    static final String REASON_INSUFFICIENT_STOCK = "INSUFFICIENT_STOCK";
    static final String REASON_PRODUCT_NOT_FOUND = "PRODUCT_NOT_FOUND";
    static final String REASON_PRODUCT_INACTIVE = "PRODUCT_INACTIVE";

    private final OutboxService outboxService;
    private final ProductRepository productRepository;

    public void reserve(PaymentApprovedEvent paymentApprovedEvent) {
        log.info("Reserving inventory for orderId={}", paymentApprovedEvent.orderId());

        List<OrderItem> items = paymentApprovedEvent.items();
        if (items == null || items.isEmpty()) {
            throw new BusinessException("Order has no items to reserve", 422);
        }

        Map<UUID, Integer> itemsByProductId = mergeItemsByProduct(items);

        Optional<String> validationFailure = validateProductsForReservation(itemsByProductId);
        if (validationFailure.isPresent()) {
            publishInventoryUnavailable(paymentApprovedEvent, items, validationFailure.get());
            return;
        }

        boolean reserved = productRepository.reserveOrderItems(itemsByProductId);
        if (!reserved) {
            publishInventoryUnavailable(paymentApprovedEvent, items, REASON_INSUFFICIENT_STOCK);
            return;
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

    private Optional<String> validateProductsForReservation(Map<UUID, Integer> itemsByProductId) {
        for (UUID productId : itemsByProductId.keySet()) {
            Optional<ProductEntity> product = productRepository.findProductById(productId);
            if (product.isEmpty()) {
                return Optional.of(REASON_PRODUCT_NOT_FOUND);
            }
            if (!"ACTIVE".equals(product.get().getStatus())) {
                return Optional.of(REASON_PRODUCT_INACTIVE);
            }
        }
        return Optional.empty();
    }

    private void publishInventoryUnavailable(PaymentApprovedEvent paymentApprovedEvent,
            List<OrderItem> items,
            String reason) {
        InventoryUnavailableEvent event = new InventoryUnavailableEvent(
                UUID.randomUUID(),
                paymentApprovedEvent.traceId(),
                "INVENTORY_UNAVAILABLE",
                Instant.now(),
                paymentApprovedEvent.orderId(),
                items,
                reason);

        outboxService.save(event);

        log.warn("Inventory unavailable orderId={} reason={}", paymentApprovedEvent.orderId(), reason);
    }
}
