package com.fredfmelo.inventoryservice.idempotency.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fredfmelo.inventoryservice.idempotency.entity.IdempotencyEntity;
import com.fredfmelo.inventoryservice.idempotency.repository.IdempotencyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String PREFIX = "IDEMPOTENCY#";
    private static final String METADATA = "METADATA";

    private static final String PROCESSING = "PROCESSING";
    private static final String PROCESSED = "PROCESSED";
    
    private static final long PROCESSING_TIMEOUT_MINUTES = 5;

    private final IdempotencyRepository repository;

    @Value("${spring.application.name}")
    private String serviceName;

    public boolean acquire(UUID eventId) {
        IdempotencyEntity entity = createEntity(eventId);

        if (repository.acquire(entity)) {
            return true;
        }

        IdempotencyEntity existing = repository.find(eventId);

        if (existing == null) {
            return false;
        }

        if (PROCESSED.equals(existing.getStatus())) {
            return false;
        }

        if (PROCESSING.equals(existing.getStatus())) {
            long age = Duration.between(
                    existing.getUpdatedAt(),
                    Instant.now()
            ).toMinutes();

            return age > PROCESSING_TIMEOUT_MINUTES;
        }

        return false;
    }

    public IdempotencyEntity createEntity(UUID eventId){
        IdempotencyEntity entity = new IdempotencyEntity();

        entity.setPk(PREFIX + eventId);
        entity.setSk(METADATA);

        entity.setService(serviceName);
        entity.setStatus(PROCESSING);

        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        return entity;
    }

    public void markProcessed(UUID eventId) {
        IdempotencyEntity entity = repository.find(eventId);

        entity.setStatus(PROCESSED);
        entity.setUpdatedAt(Instant.now());

        repository.save(entity);
    }
}