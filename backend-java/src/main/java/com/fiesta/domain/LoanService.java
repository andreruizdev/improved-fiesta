package com.fiesta.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiesta.outbox.OutboxEvent;
import com.fiesta.outbox.OutboxEventRepository;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoanService {
    private final LoanApplicationRepository loanRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    private static final TextMapSetter<Map<String, String>> setter =
            (carrier, key, value) -> carrier.put(key, value);

    @Transactional
    public LoanApplication submitApplication(LoanApplication application) {
        LoanApplication savedApp = loanRepository.save(application);

        String payload;
        try {
            payload = objectMapper.writeValueAsString(savedApp);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize loan application", e);
        }

        Map<String, String> traceContext = new HashMap<>();
        GlobalOpenTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), traceContext, setter);
        String traceparent = traceContext.get("traceparent");

        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("LoanApplication");
        event.setAggregateId(savedApp.getId().toString());
        event.setEventType("ApplicationSubmitted");
        event.setPayload(payload);
        event.setTraceparent(traceparent);

        outboxRepository.save(event);

        return savedApp;
    }
}
