package com.fiesta.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiesta.outbox.OutboxEvent;
import com.fiesta.outbox.OutboxEventRepository;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoanServiceTest {

    @Mock
    private LoanApplicationRepository loanRepository;

    @Mock
    private OutboxEventRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private LoanService loanService;

    private LoanApplication sampleApplication;
    private LoanApplication savedApplication;
    private UUID sampleId;

    @BeforeEach
    void setUp() {
        sampleId = UUID.randomUUID();

        sampleApplication = new LoanApplication();
        sampleApplication.setApplicantId("app-123");
        sampleApplication.setAmount(new BigDecimal("10000.00"));
        sampleApplication.setTermMonths(24);

        savedApplication = new LoanApplication();
        savedApplication.setId(sampleId);
        savedApplication.setApplicantId("app-123");
        savedApplication.setAmount(new BigDecimal("10000.00"));
        savedApplication.setTermMonths(24);
    }

    @Test
    void submitApplication_HappyPath_SuccessfullySavesAndCreatesOutboxEvent() throws JsonProcessingException {
        // Arrange
        when(loanRepository.save(any(LoanApplication.class))).thenReturn(savedApplication);
        when(objectMapper.writeValueAsString(any(LoanApplication.class))).thenReturn("{\"id\":\"" + sampleId + "\"}");

        ContextPropagators propagators = mock(ContextPropagators.class);
        TextMapPropagator propagator = mock(TextMapPropagator.class);
        when(propagators.getTextMapPropagator()).thenReturn(propagator);

        doAnswer(invocation -> {
            Map<String, String> carrier = invocation.getArgument(1);
            carrier.put("traceparent", "00-test-trace-id");
            return null;
        }).when(propagator).inject(any(), any(), any());

        try (MockedStatic<GlobalOpenTelemetry> mockedOTel = mockStatic(GlobalOpenTelemetry.class)) {
            mockedOTel.when(GlobalOpenTelemetry::getPropagators).thenReturn(propagators);

            // Act
            LoanApplication result = loanService.submitApplication(sampleApplication);

            // Assert
            assertNotNull(result);
            assertEquals(sampleId, result.getId());

            verify(loanRepository, times(1)).save(sampleApplication);

            ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxRepository, times(1)).save(eventCaptor.capture());

            OutboxEvent capturedEvent = eventCaptor.getValue();
            assertEquals("LoanApplication", capturedEvent.getAggregateType());
            assertEquals(sampleId.toString(), capturedEvent.getAggregateId());
            assertEquals("ApplicationSubmitted", capturedEvent.getEventType());
            assertEquals("{\"id\":\"" + sampleId + "\"}", capturedEvent.getPayload());

            // Ensure traceparent is populated correctly
            assertEquals("00-test-trace-id", capturedEvent.getTraceparent());
        }
    }

    @Test
    void submitApplication_SerializationFails_ThrowsRuntimeException() throws JsonProcessingException {
        // Arrange
        when(loanRepository.save(any(LoanApplication.class))).thenReturn(savedApplication);
        when(objectMapper.writeValueAsString(any(LoanApplication.class)))
            .thenThrow(mock(JsonProcessingException.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            loanService.submitApplication(sampleApplication);
        });

        assertEquals("Failed to serialize loan application", exception.getMessage());
        assertInstanceOf(JsonProcessingException.class, exception.getCause());

        verify(loanRepository, times(1)).save(sampleApplication);
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void submitApplication_SavedAppHasNullId_ThrowsNullPointerException() throws JsonProcessingException {
        // Arrange
        LoanApplication appWithoutId = new LoanApplication();
        appWithoutId.setApplicantId("app-456");

        when(loanRepository.save(any(LoanApplication.class))).thenReturn(appWithoutId);
        when(objectMapper.writeValueAsString(any(LoanApplication.class))).thenReturn("{}");

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            loanService.submitApplication(sampleApplication);
        });

        verify(loanRepository, times(1)).save(sampleApplication);
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void submitApplication_LoanRepositoryThrowsException_PropagatesException() {
        // Arrange
        when(loanRepository.save(any(LoanApplication.class)))
            .thenThrow(new DataAccessException("Database error") {});

        // Act & Assert
        assertThrows(DataAccessException.class, () -> {
            loanService.submitApplication(sampleApplication);
        });

        verify(loanRepository, times(1)).save(sampleApplication);
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void submitApplication_OutboxRepositoryThrowsException_PropagatesException() throws JsonProcessingException {
        // Arrange
        when(loanRepository.save(any(LoanApplication.class))).thenReturn(savedApplication);
        when(objectMapper.writeValueAsString(any(LoanApplication.class))).thenReturn("{\"id\":\"" + sampleId + "\"}");
        when(outboxRepository.save(any(OutboxEvent.class)))
            .thenThrow(new DataAccessException("Database error") {});

        ContextPropagators propagators = mock(ContextPropagators.class);
        TextMapPropagator propagator = mock(TextMapPropagator.class);
        when(propagators.getTextMapPropagator()).thenReturn(propagator);

        try (MockedStatic<GlobalOpenTelemetry> mockedOTel = mockStatic(GlobalOpenTelemetry.class)) {
            mockedOTel.when(GlobalOpenTelemetry::getPropagators).thenReturn(propagators);

            // Act & Assert
            assertThrows(DataAccessException.class, () -> {
                loanService.submitApplication(sampleApplication);
            });

            verify(loanRepository, times(1)).save(sampleApplication);
            verify(outboxRepository, times(1)).save(any(OutboxEvent.class));
        }
    }
}
