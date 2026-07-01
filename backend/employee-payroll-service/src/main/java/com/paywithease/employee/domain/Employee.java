package com.paywithease.employee.domain;

import com.paywithease.common.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;

/** An employee of a tenant. Sensitive identifiers (mobile, email, PAN) are encrypted at rest. */
@Entity
@Table(name = "employees")
public class Employee {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(nullable = false)
  private String name;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "mobile_enc")
  private String mobile;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "email_enc")
  private String email;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "pan_enc")
  private String pan;

  private String designation;

  @Column(name = "date_of_joining")
  private LocalDate dateOfJoining;

  @Column(nullable = false)
  private String status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected Employee() {}

  public Employee(String id, String tenantId, String name, Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.name = name;
    this.status = "ACTIVE";
    this.createdAt = now;
    this.updatedAt = now;
  }

  public void updateName(String name, Instant now) {
    if (name != null) this.name = name;
    this.updatedAt = now;
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getName() {
    return name;
  }

  public String getMobile() {
    return mobile;
  }

  public void setMobile(String mobile) {
    this.mobile = mobile;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPan() {
    return pan;
  }

  public void setPan(String pan) {
    this.pan = pan;
  }

  public String getDesignation() {
    return designation;
  }

  public void setDesignation(String designation) {
    this.designation = designation;
  }

  public LocalDate getDateOfJoining() {
    return dateOfJoining;
  }

  public void setDateOfJoining(LocalDate dateOfJoining) {
    this.dateOfJoining = dateOfJoining;
  }

  public String getStatus() {
    return status;
  }
}
