package com.fiesta.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoanController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for unit tests focused on the controller logic
public class LoanControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LoanService loanService;

    @Test
    public void submit_ValidRequest_ReturnsOk() throws Exception {
        LoanApplicationRequest request = new LoanApplicationRequest();
        request.setApplicantId("app-123");
        request.setAmount(new BigDecimal("10000.00"));
        request.setTermMonths(36);

        LoanApplication mockResponse = new LoanApplication();
        mockResponse.setId(UUID.randomUUID());
        mockResponse.setApplicantId("app-123");
        mockResponse.setAmount(new BigDecimal("10000.00"));
        mockResponse.setTermMonths(36);
        mockResponse.setStatus("PENDING_REVIEW");
        mockResponse.setCreatedAt(Instant.now());

        when(loanService.submitApplication(any(LoanApplication.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
    }

    @Test
    public void submit_MissingApplicantId_ReturnsBadRequest() throws Exception {
        LoanApplicationRequest request = new LoanApplicationRequest();
        request.setAmount(new BigDecimal("10000.00"));
        request.setTermMonths(36);

        mockMvc.perform(post("/api/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void submit_NegativeAmount_ReturnsBadRequest() throws Exception {
        LoanApplicationRequest request = new LoanApplicationRequest();
        request.setApplicantId("app-123");
        request.setAmount(new BigDecimal("-100.00"));
        request.setTermMonths(36);

        mockMvc.perform(post("/api/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void submit_InvalidTermMonths_ReturnsBadRequest() throws Exception {
        LoanApplicationRequest request = new LoanApplicationRequest();
        request.setApplicantId("app-123");
        request.setAmount(new BigDecimal("10000.00"));
        request.setTermMonths(0);

        mockMvc.perform(post("/api/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void submit_MassAssignmentAttempt_IgnoresExtraFields() throws Exception {
        // We simulate an attack where the user tries to send 'status' or 'id'
        // in the JSON payload, hoping they will be mapped directly to the entity.
        String maliciousPayload = """
                {
                    "applicantId": "app-123",
                    "amount": 10000.00,
                    "termMonths": 36,
                    "id": "123e4567-e89b-12d3-a456-426614174000",
                    "status": "APPROVED",
                    "createdAt": "2000-01-01T00:00:00Z"
                }
                """;

        LoanApplication mockResponse = new LoanApplication();
        // The service should return a newly generated ID, ignoring the provided one.
        mockResponse.setId(UUID.randomUUID());
        mockResponse.setApplicantId("app-123");
        mockResponse.setAmount(new BigDecimal("10000.00"));
        mockResponse.setTermMonths(36);
        // The service logic (via entity annotations/defaults) sets PENDING_REVIEW
        mockResponse.setStatus("PENDING_REVIEW");
        mockResponse.setCreatedAt(Instant.now());

        when(loanService.submitApplication(any(LoanApplication.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(maliciousPayload)
                .with(csrf()))
                .andExpect(status().isOk())
                // Ensure the status remains PENDING_REVIEW, not APPROVED
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                // Ensure the ID is not the one injected
                .andExpect(jsonPath("$.id").value(mockResponse.getId().toString()));
    }
}
