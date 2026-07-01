package com.paywithease.employee.infrastructure;

import com.paywithease.employee.domain.SalaryStructure;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, String> {

  Optional<SalaryStructure> findByEmployeeId(String employeeId);
}
