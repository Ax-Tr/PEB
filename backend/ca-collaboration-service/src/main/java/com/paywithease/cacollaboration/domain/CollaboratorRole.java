package com.paywithease.cacollaboration.domain;

/** Scope of an invited external collaborator. AUDITOR is strictly read-only. */
public enum CollaboratorRole {
  ACCOUNTANT(false),
  CA(false),
  AUDITOR(true);

  private final boolean readOnly;

  CollaboratorRole(boolean readOnly) {
    this.readOnly = readOnly;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public static boolean isValid(String value) {
    for (CollaboratorRole r : values()) {
      if (r.name().equals(value)) {
        return true;
      }
    }
    return false;
  }
}
