package com.fredfmelo.inventoryservice.inventory.listener;

import org.springframework.stereotype.Component;

import com.fredfmelo.inventoryservice.inventory.event.PaymentApprovedEvent;
import com.fredfmelo.inventoryservice.inventory.service.InventoryService;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentApprovedListener {

    private final InventoryService inventoryService;

    @SqsListener("${aws.sqs.inventory-queue}")
    public void consume(PaymentApprovedEvent event) {
        inventoryService.reserve(event);
    }
}