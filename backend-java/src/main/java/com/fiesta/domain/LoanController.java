package com.fiesta.domain;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
@Validated
public class LoanController {
    private final LoanService loanService;
    private final DynamicLoanValidator dynamicLoanValidator;

    @PostMapping
    public ResponseEntity<LoanApplication> submit(@Valid @RequestBody LoanApplicationRequest request) {
        // Enforce dynamic business tier limits
        dynamicLoanValidator.validate(request);

        LoanApplication application = new LoanApplication();
        application.setApplicantId(request.getApplicantId());
        application.setAmount(request.getAmount());
        application.setTermMonths(request.getTermMonths());

        return ResponseEntity.ok(loanService.submitApplication(application));
    }
}
