package com.fiesta.domain;

import com.fiesta.outbox.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class LoanControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private LoanApplicationRepository loanRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    public void setup() {
        loanRepository.deleteAll();
        outboxEventRepository.deleteAll();

        // Mock the JWT decoder to accept our dummy token
        Jwt jwt = Jwt.withTokenValue("dummy-token")
                .header("alg", "none")
                .claim("sub", "user1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(jwtDecoder.decode(anyString())).thenReturn(jwt);
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("dummy-token");
        return headers;
    }

    private String createBaseUrl() {
        return "http://localhost:" + port + "/api/loans";
    }

    @Test
    public void submitLoanApplication_Success() {
        LoanApplicationRequest request = new LoanApplicationRequest();
        request.setApplicantId("app-123");
        request.setAmount(new BigDecimal("5000.00"));
        request.setTermMonths(24);

        HttpEntity<LoanApplicationRequest> entity = new HttpEntity<>(request, getHeaders());

        ResponseEntity<LoanApplicationResponse> response = restTemplate.postForEntity(
                createBaseUrl(), entity, LoanApplicationResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        LoanApplicationResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isNotNull();
        assertThat(body.getApplicantId()).isEqualTo("app-123");
        assertThat(body.getAmount()).isEqualTo(new BigDecimal("5000.00"));
        assertThat(body.getTermMonths()).isEqualTo(24);
        assertThat(body.getStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(body.getCreatedAt()).isNotNull();

        // Verify it was saved in DB
        assertThat(loanRepository.findAll()).hasSize(1);

        // Verify outbox event was created
        assertThat(outboxEventRepository.findAll()).hasSize(1);
    }

    @Test
    public void submitLoanApplication_ValidationError_MissingApplicantId() {
        LoanApplicationRequest request = new LoanApplicationRequest();
        // Missing applicant ID
        request.setAmount(new BigDecimal("5000.00"));
        request.setTermMonths(24);

        HttpEntity<LoanApplicationRequest> entity = new HttpEntity<>(request, getHeaders());

        ResponseEntity<String> response = restTemplate.postForEntity(
                createBaseUrl(), entity, String.class);

        // Validation should fail
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Verify not saved
        assertThat(loanRepository.findAll()).isEmpty();
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    public void submitLoanApplication_ValidationError_NegativeAmount() {
        LoanApplicationRequest request = new LoanApplicationRequest();
        request.setApplicantId("app-123");
        request.setAmount(new BigDecimal("-100.00")); // Invalid
        request.setTermMonths(24);

        HttpEntity<LoanApplicationRequest> entity = new HttpEntity<>(request, getHeaders());

        ResponseEntity<String> response = restTemplate.postForEntity(
                createBaseUrl(), entity, String.class);

        // Validation should fail
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void submitLoanApplication_Unauthorized_NoToken() {
        LoanApplicationRequest request = new LoanApplicationRequest();
        request.setApplicantId("app-123");
        request.setAmount(new BigDecimal("5000.00"));
        request.setTermMonths(24);

        // No headers with bearer token
        HttpEntity<LoanApplicationRequest> entity = new HttpEntity<>(request);

        ResponseEntity<String> response = restTemplate.postForEntity(
                createBaseUrl(), entity, String.class);

        // We expect either 401 Unauthorized or 403 Forbidden based on Spring Security setup
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }
}
