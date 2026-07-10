package com.paywithease.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A system role (OWNER, CASHIER, ...). Authorities are derived from role name + permissions. */
@Entity
@Table(name = "roles")
public class Role {

  @Id
  @Column(columnDefinition = "char(26)")
  private String id;

  @Column(nullable = false, unique = true)
  private String name;

  @Column(nullable = false)
  private String description;

  @Column(name = "system_role", nullable = false)
  private boolean systemRole;

  protected Role() {}

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public boolean isSystemRole() {
    return systemRole;
  }
}
