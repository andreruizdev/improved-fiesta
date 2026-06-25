# Architecture Runbook
## Components
1. **Java Backend (`backend-java`)**: Event Producer using Spring Boot 3, Java 21, Transactional Outbox pattern.
2. **Infrastructure (`platform-infra`)**: Kafka, Postgres, Debezium CDC.
3. **Python Inference Service (`inference-python`)**: FastAPI Consumer, Idempotent via Redis, XGBoost ML with SHAP.
