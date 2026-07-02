package com.paywithease.employee.infrastructure;

import com.paywithease.employee.domain.SalaryRunLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaryRunLineRepository extends JpaRepository<SalaryRunLine, String> {
  List<SalaryRunLine> findBySalaryRunId(String salaryRunId);
}
