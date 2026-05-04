package com.example.llm_gateway.auth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageLogRepository extends JpaRepository<UsageLog, Long> {}
