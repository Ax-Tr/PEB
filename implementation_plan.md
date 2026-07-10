# End-to-End Feature Verification Plan

The onboarding flow is complete. Now we need to systematically verify every feature in the application (Customers, Vendors, Invoices, Payroll, Dashboard, Settings). 

## User Review Required

> [!IMPORTANT]
> The remaining microservices (`customer-service`, `vendor-service`, `product-service`, `employee-payroll-service`) are currently **not running**. We must start them to test these features.
>
> Furthermore, the React Native web frontend (`npm run web`) automatically claimed port **8084**, which is the default port for `vendor-service`. This will cause a conflict. 

## Proposed Changes

1. **Fix Port Conflicts**:
   - Change `vendor-service` default port from `8084` to `8087`.
   - Update `api-gateway` routing to point `vendor-service` to `http://localhost:8087`.

2. **Start Remaining Services**:
   - Start `customer-service` (Port 8083)
   - Start `vendor-service` (Port 8087)
   - Start `product-service` (Port 8085)
   - Start `employee-payroll-service` (Port 8086)
   - *(Note: Identity, Tenant, Analytics, and API Gateway are already running successfully).*

3. **Systematic Feature Testing**:
   Once all services are up, we will test each UI feature one by one:
   - **Dashboard**: Verify PnL and quick actions load.
   - **Customers**: Create a new customer and view the list.
   - **Vendors**: Create a new vendor and view the list.
   - **Products**: Add a product/service.
   - **Invoices**: Create an invoice for the customer.
   - **Payroll**: Add an employee.
   
## Verification Plan
After starting the services, I will guide you through testing each feature in the browser. If any backend errors occur, I will monitor the background service logs to debug and fix them immediately.
