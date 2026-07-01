package com.paywithease.common.audit;

import org.springframework.data.jpa.repository.JpaRepository;

/** Append-only: only {@code save} and reads are used; no delete is ever invoked. */
public interface AuditRepository extends JpaRepository<AuditEvent, String> {}
