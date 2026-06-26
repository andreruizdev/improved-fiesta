package com.foundry.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxPayload, UUID> {
}