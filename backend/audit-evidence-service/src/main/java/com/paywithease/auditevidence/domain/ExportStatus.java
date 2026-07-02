package com.paywithease.auditevidence.domain;

/** Lifecycle of an auditor export job. */
public enum ExportStatus {
  REQUESTED,
  PROCESSING,
  COMPLETED,
  FAILED
}
