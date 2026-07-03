package com.paywithease.analytics.infrastructure;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FactCommitmentRepository extends JpaRepository<FactCommitment, String> {

  Optional<FactCommitment> findByTenantIdAndCommitmentId(String tenantId, String commitmentId);

  @Query(
      "select coalesce(sum(f.amountMinor), 0) from FactCommitment f where f.tenantId = :tenantId")
  long sumPromisedMinor(@Param("tenantId") String tenantId);

  @Query("select coalesce(sum(f.paidMinor), 0) from FactCommitment f where f.tenantId = :tenantId")
  long sumPaidMinor(@Param("tenantId") String tenantId);

  @Query(
      "select coalesce(sum(f.outstandingMinor), 0) from FactCommitment f "
          + "where f.tenantId = :tenantId and f.status not in ('PAID','CANCELLED')")
  long sumOpenOutstandingMinor(@Param("tenantId") String tenantId);

  long countByTenantIdAndStatus(String tenantId, String status);

  @Query(
      "select count(f) from FactCommitment f where f.tenantId = :tenantId "
          + "and f.status not in ('PAID','CANCELLED')")
  long countOpen(@Param("tenantId") String tenantId);

  @Query(
      "select f from FactCommitment f where f.tenantId = :tenantId "
          + "and f.status not in ('PAID','CANCELLED') and f.dueDate = :today order by f.updatedAt desc")
  List<FactCommitment> dueToday(
      @Param("tenantId") String tenantId, @Param("today") LocalDate today);

  @Query(
      "select f from FactCommitment f where f.tenantId = :tenantId "
          + "and f.status not in ('PAID','CANCELLED') and f.dueDate < :today order by f.dueDate asc")
  List<FactCommitment> overdue(@Param("tenantId") String tenantId, @Param("today") LocalDate today);

  @Query(
      "select f from FactCommitment f where f.tenantId = :tenantId "
          + "and f.status not in ('PAID','CANCELLED') and f.dueDate between :from and :to "
          + "order by f.dueDate asc")
  List<FactCommitment> dueBetween(
      @Param("tenantId") String tenantId, @Param("from") LocalDate from, @Param("to") LocalDate to);

  List<FactCommitment> findTop20ByTenantIdAndStatusOrderByDueDateAsc(
      String tenantId, String status);
}
