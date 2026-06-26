package com.foundry.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foundry.outbox.OutboxPayload;
import com.foundry.outbox.OutboxRepository;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TelemetryService {
    private final DataRecordRepository recordRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    private static final TextMapSetter<Map<String, String>> setter = (carrier, key, value) -> carrier.put(key, value);

    @Transactional // Guarantees both inserts pass or fail together
    public DataRecord processIncomingTelemetry(DataRecord record) {
        DataRecord savedRecord = recordRepository.save(record);

        String payloadString;
        try {
            payloadString = objectMapper.writeValueAsString(savedRecord);
        } catch (Exception e) {
            throw new IllegalStateException("Serialization block failed", e);
        }

        // Trace Propagation capturing end-to-end telemetry context across boundaries
        Map<String, String> contextCarrier = new HashMap<>();
        GlobalOpenTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), contextCarrier, setter);
        String traceparent = contextCarrier.get("traceparent");

        OutboxPayload outboxEvent = new OutboxPayload();
        outboxEvent.setAggregateType("TelemetryRecord");
        outboxEvent.setAggregateId(savedRecord.getId().toString());
        outboxEvent.setPayload(payloadString);
        outboxEvent.setTraceparent(traceparent);

        outboxRepository.save(outboxEvent);
        return savedRecord;
    }
}