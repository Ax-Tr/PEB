package com.paywithease.product.infrastructure;

import com.paywithease.product.domain.HsnSac;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HsnSacRepository extends JpaRepository<HsnSac, String> {

  /**
   * Lookup by code prefix or free-text description. Returns up to 20 matches, code-first so exact
   * prefix hits surface ahead of description-only matches.
   */
  @Query(
      "SELECT h FROM HsnSac h "
          + "WHERE h.code LIKE CONCAT(:q, '%') "
          + "OR LOWER(h.description) LIKE LOWER(CONCAT('%', :q, '%')) "
          + "ORDER BY h.code")
  List<HsnSac> search(@Param("q") String q, Limit limit);
}
