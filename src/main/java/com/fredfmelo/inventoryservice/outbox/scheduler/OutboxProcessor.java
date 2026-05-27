package com.fredfmelo.inventoryservice.outbox.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fredfmelo.inventoryservice.outbox.entity.OutboxEntity;
import com.fredfmelo.inventoryservice.outbox.publisher.OutboxEventPublisher;
import com.fredfmelo.inventoryservice.outbox.repository.OutboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxProcessor {

    private static final String PROCESSED = "PROCESSED";
    private static final String PROCESSED_STATUS_PK = "OUTBOX_STATUS#PROCESSED";

    private final OutboxRepository repository;
    private final OutboxEventPublisher publisher;

    @Scheduled(fixedDelay = 5000)
    public void process() {
        var pendingEvents = repository.findPending();

        for (OutboxEntity event : pendingEvents) {
            log.info("[SCHEDULER] Found {} pending events", pendingEvents.size());

            try {
                publisher.publish(event.getPayload(), event.getEventType());

                updateAndSaveOutbox(event);

                log.info("Outbox processed={}", event.getPk());

            } catch (Exception ex) {
                log.error("Error processing outbox={}", event.getPk(), ex);
            }
        }
    }

    private void updateAndSaveOutbox(OutboxEntity event) {
        event.setStatus(PROCESSED);
        event.setOutboxStatusPk(PROCESSED_STATUS_PK);

        repository.save(event);
    }
}