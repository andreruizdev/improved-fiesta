package com.fiesta.domain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class HealthController {
    @GetMapping("/healthz")
    public String healthz() { return "OK"; }
    @GetMapping("/ready")
    public String ready() { return "OK"; }
}
