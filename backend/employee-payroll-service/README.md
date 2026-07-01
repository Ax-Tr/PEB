# employee-payroll-service

Employee master and salary structure for the PEB platform.

- **Port:** 8086
- **Database:** `employee_db` (Flyway-managed, service-owned)
- **Base package:** `com.paywithease.employee`

## Scope (current sprint)

- **Employee master** — create / get / list employees. Sensitive fields (mobile, email, PAN)
  are encrypted at rest via `EncryptedStringConverter` (AES-GCM).
- **Salary structure** — one current structure per employee (gross / basic / HRA in integer
  paise, plus PF/ESI/PT applicability flags). Upsert + read.

All monetary amounts are integer **paise** stored in `bigint` columns with a `_minor` suffix.
All operations are tenant-scoped via `TenantContext.requireTenantId()`.

## Out of scope (later sprint)

Salary runs, payslip generation, and statutory calculations (PF/ESI/PT/TDS computation) are a
later sprint and are intentionally **not** implemented here.

## API

Base path: `/api/v1/employees`

| Method | Path                        | Description                          |
|--------|-----------------------------|--------------------------------------|
| POST   | `/`                         | Create an employee (201)             |
| GET    | `/`                         | List employees in the current tenant |
| GET    | `/{id}`                     | Get an employee                      |
| PUT    | `/{id}/salary-structure`    | Create or replace a salary structure |
| GET    | `/{id}/salary-structure`    | Get a salary structure               |

## Events

Emits `EMPLOYEE_CREATED` to the transactional outbox on employee creation.
