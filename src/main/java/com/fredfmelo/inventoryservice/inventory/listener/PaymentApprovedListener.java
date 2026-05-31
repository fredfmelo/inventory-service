package com.fredfmelo.inventoryservice.inventory.listener;

import org.springframework.stereotype.Component;

import com.fredfmelo.eventdrivencore.idempotency.executor.IdempotentExecutor;
import com.fredfmelo.inventoryservice.inventory.event.PaymentApprovedEvent;
import com.fredfmelo.inventoryservice.inventory.service.InventoryService;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentApprovedListener {

    private final InventoryService inventoryService;
    private final IdempotentExecutor idempotentExecutor;

    @SqsListener("${aws.sqs.inventory-queue}")
    public void consume(PaymentApprovedEvent event) {
        idempotentExecutor.execute(event, () -> inventoryService.reserve(event));
    }
}