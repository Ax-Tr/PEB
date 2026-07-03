package com.paywithease.commitment.infrastructure;

import com.paywithease.commitment.domain.Commitment;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommitmentRepository extends JpaRepository<Commitment, String> {

  Optional<Commitment> findByTenantIdAndId(String tenantId, String id);

  boolean existsByTenantIdAndSourceTypeAndSourceRef(
      String tenantId, String sourceType, String sourceRef);

  @Query(
      "select c from Commitment c where c.tenantId = :tenantId "
          + "and (:status is null or c.status = :status) "
          + "and (:counterpartyType is null or c.counterpartyType = :counterpartyType) "
          + "order by c.dueDate asc, c.createdAt desc")
  List<Commitment> list(
      @Param("tenantId") String tenantId,
      @Param("status") String status,
      @Param("counterpartyType") String counterpartyType);

  @Query(
      "select c from Commitment c where c.tenantId = :tenantId "
          + "and c.status not in ('PAID','CANCELLED') "
          + "and c.dueDate between :from and :to order by c.dueDate asc")
  List<Commitment> dueSoon(
      @Param("tenantId") String tenantId, @Param("from") LocalDate from, @Param("to") LocalDate to);

  @Query(
      "select c from Commitment c where c.tenantId = :tenantId "
          + "and c.status not in ('PAID','CANCELLED') and c.dueDate < :today order by c.dueDate asc")
  List<Commitment> overdue(@Param("tenantId") String tenantId, @Param("today") LocalDate today);
}
