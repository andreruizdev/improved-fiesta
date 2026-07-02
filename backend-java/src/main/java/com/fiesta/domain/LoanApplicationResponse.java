package com.fiesta.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplicationResponse {
    private UUID id;
    private String applicantId;
    private BigDecimal amount;
    private Integer termMonths;
    private String status;
    private Instant createdAt;
}
