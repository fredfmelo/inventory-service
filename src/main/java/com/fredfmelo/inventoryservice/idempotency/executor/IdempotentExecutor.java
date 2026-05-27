package com.fredfmelo.inventoryservice.idempotency.executor;

import org.springframework.stereotype.Component;

import com.fredfmelo.inventoryservice.common.exception.TechnicalException;
import com.fredfmelo.inventoryservice.idempotency.event.IdempotentEvent;
import com.fredfmelo.inventoryservice.idempotency.service.IdempotencyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentExecutor {

    private final IdempotencyService idempotencyService;

    public void execute(IdempotentEvent event, Runnable action) {

        if (!idempotencyService.acquire(event.eventId())) {
            log.info("Duplicate event ignored eventId={}", event.eventId());
            return;
        }

        try {
            action.run();

            idempotencyService.markProcessed(event.eventId());

        } catch (Exception ex) {
            log.error("Error processing eventId={}", event.eventId(), ex);

            throw new TechnicalException("Error processing event", ex);
        }
    }
}