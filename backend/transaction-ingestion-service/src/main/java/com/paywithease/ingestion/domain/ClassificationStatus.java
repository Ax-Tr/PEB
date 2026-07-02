package com.paywithease.ingestion.domain;

/** Classification lifecycle. Low-confidence suggestions stay SUGGESTED until a user confirms. */
public enum ClassificationStatus {
  UNCLASSIFIED,
  SUGGESTED,
  CONFIRMED
}
