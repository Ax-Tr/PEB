package com.paywithease.employee.infrastructure;

import com.paywithease.employee.domain.Employee;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, String> {

  List<Employee> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}
