package com.paywithease.employee.api;

import com.paywithease.employee.application.EmployeeService;
import com.paywithease.employee.domain.Employee;
import com.paywithease.employee.domain.SalaryStructure;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Employee master &amp; salary structure API. Salary runs/payslips are a later sprint. */
@RestController
@RequestMapping("/api/v1/employees")
@Tag(name = "employees", description = "Employee master and salary structure")
public class EmployeeController {

  private final EmployeeService service;

  public EmployeeController(EmployeeService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create an employee in the current tenant")
  public EmployeeDtos.EmployeeResponse create(
      @Valid @RequestBody EmployeeDtos.CreateEmployee body) {
    Employee e =
        service.create(
            body.name(),
            body.mobile(),
            body.email(),
            body.pan(),
            body.designation(),
            body.dateOfJoining());
    return toResponse(e);
  }

  @GetMapping
  @Operation(summary = "List employees in the current tenant")
  public List<EmployeeDtos.EmployeeResponse> list() {
    return service.list().stream().map(this::toResponse).toList();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get an employee")
  public EmployeeDtos.EmployeeResponse get(@PathVariable String id) {
    return toResponse(service.get(id));
  }

  @PutMapping("/{id}/salary-structure")
  @Operation(summary = "Create or replace an employee's salary structure")
  public EmployeeDtos.SalaryStructureResponse setSalaryStructure(
      @PathVariable String id, @Valid @RequestBody EmployeeDtos.SalaryStructureRequest body) {
    SalaryStructure s =
        service.setSalaryStructure(
            id,
            body.grossSalaryMinor(),
            body.basicMinor(),
            body.hraMinor(),
            body.pfApplicable(),
            body.esiApplicable(),
            body.ptApplicable(),
            body.effectiveFrom());
    return toSalaryStructure(s);
  }

  @GetMapping("/{id}/salary-structure")
  @Operation(summary = "Get an employee's salary structure")
  public EmployeeDtos.SalaryStructureResponse getSalaryStructure(@PathVariable String id) {
    return toSalaryStructure(service.getSalaryStructure(id));
  }

  private EmployeeDtos.EmployeeResponse toResponse(Employee e) {
    return new EmployeeDtos.EmployeeResponse(
        e.getId(),
        e.getName(),
        e.getMobile(),
        e.getEmail(),
        e.getPan(),
        e.getDesignation(),
        e.getDateOfJoining(),
        e.getStatus());
  }

  private EmployeeDtos.SalaryStructureResponse toSalaryStructure(SalaryStructure s) {
    return new EmployeeDtos.SalaryStructureResponse(
        s.getEmployeeId(),
        s.getGrossSalaryMinor(),
        s.getBasicMinor(),
        s.getHraMinor(),
        s.isPfApplicable(),
        s.isEsiApplicable(),
        s.isPtApplicable(),
        s.getEffectiveFrom());
  }
}
