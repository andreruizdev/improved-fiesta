# Project Foundry-Stream

A modern, enterprise event-driven pipeline rebuilt from the ground up to follow zero-trust infrastructure paradigms, strict schema boundaries, and professional operations engineering.

## Overview
This architecture, named Project Foundry-Stream, demonstrates cloud-native fundamentals required for high-availability enterprise platforms. It moves away from legacy polling mechanisms and leverages Log-Based Change Data Capture (CDC) via Debezium to instantly stream database mutations into Kafka topics.

## Pipeline Architecture
1. **Foundational Cloud Topology:** Provisioned via Terraform ensuring robust remote state management and decoupled configurations. Features a private VNet for an isolated Managed Kubernetes cluster (AKS).
2. **Transactional Event Ingestion:** A Java/Spring Boot 3 service that implements the Transactional Outbox Pattern to guarantee atomicity and avoid data inconsistencies.
3. **Change Data Capture Stream Delivery:** Debezium connects to PostgreSQL WAL and instantly pushes log mutations to Kafka outbox topics.
4. **Idempotent Asynchronous Processing:** High-throughput Python ingestion loop built around strict data validation schemas, enforcing idempotent processing through Atomic Check-And-Set semantics via Redis.
5. **Declarative CI/CD Automation:** Pipeline deployment defined via a centralized Jenkinsfile managing source analysis and artifact creation.

## Deployment Instructions

1. **Infrastructure**: Navigate to `infrastructure` and run `terraform init` and `terraform apply`.
2. **Kubernetes Services**: Standard yaml deployments to manage Kafka, Redis, and Postgres environments (not tracked directly within this folder but executed post-setup).
3. **Connector Provisioning**: Execute `infrastructure/setup_cdc.sh` against your Debezium instance to spin up the Outbox Connector.