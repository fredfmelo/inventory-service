package com.fredfmelo.inventoryservice.outbox.service;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fredfmelo.inventoryservice.common.exception.TechnicalException;
import com.fredfmelo.inventoryservice.outbox.entity.OutboxEntity;
import com.fredfmelo.inventoryservice.outbox.repository.OutboxRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxService {

    private static final String METADATA = "METADATA";
    private static final String PENDING = "PENDING";
    private static final String OUTBOX_PREFIX = "OUTBOX#";

    private final OutboxRepository repository;
    private final ObjectMapper objectMapper;

    public void save(String eventId, String eventType, Object payload) {

        try {
            OutboxEntity entity = new OutboxEntity();

            entity.setPk(OUTBOX_PREFIX + eventId);
            entity.setSk(METADATA);
            entity.setEventType(eventType);
            entity.setStatus(PENDING);
            entity.setOutboxStatusPk("OUTBOX_STATUS#PENDING");
            entity.setOutboxCreatedAtSk(Instant.now().toString());
            entity.setCreatedAt(Instant.now());

            entity.setPayload(objectMapper.writeValueAsString(payload));

            repository.save(entity);

        } catch (JsonProcessingException ex) {
            throw new TechnicalException("Error serializing outbox payload", ex);
        }
    }
}