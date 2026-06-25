package com.fiesta.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "loan_applications")
@Data
@NoArgsConstructor
public class LoanApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private String applicantId;
    private BigDecimal amount;
    private Integer termMonths;
    private String status;
    private Instant createdAt;
    
    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.status = "PENDING_REVIEW";
    }
}
