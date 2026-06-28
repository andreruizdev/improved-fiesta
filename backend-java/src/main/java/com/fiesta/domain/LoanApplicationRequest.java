package com.fiesta.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanApplicationRequest {
    @NotBlank(message = "Applicant ID is required")
    private String applicantId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Term months is required")
    @Min(value = 1, message = "Term must be at least 1 month")
    private Integer termMonths;
}
