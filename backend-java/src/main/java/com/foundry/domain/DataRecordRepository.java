package com.foundry.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface DataRecordRepository extends JpaRepository<DataRecord, UUID> {
}