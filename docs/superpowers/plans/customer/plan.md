# Customer Management Module - Implementation Plan

**Created:** 2026-03-19  
**Status:** Draft  
**Module:** Customer Management  
**Worktree:** `.worktrees/foundation` (branch: `feature/foundation`)

---

## Overview

Implement the Customer Management module for the Corporate & SME Banking System following the Modular Monolith architecture. This plan covers 17 tasks organized into 4 phases.

## Approach

**Backend-first:** All services and repositories are implemented and tested before frontend work begins. Each layer is verified before moving to the next.

**Testing strategy:** Unit tests for service layer, integration tests for repositories. Frontend component tests using React Testing Library.

---

## Task 1: Project Setup

**Estimated steps:** 8

- [ ] 1.1 Create Gradle build files (`build.gradle`, `settings.gradle`) with Spring Boot 3.2, Java 17
- [ ] 1.2 Create `gradle.properties` with version catalog
- [ ] 1.3 Create `config.yaml` with module configuration
- [ ] 1.4 Create package structure under `src/main/java/com/banking/customer/`
- [ ] 1.5 Create `application.yml` with Spring configuration
- [ ] 1.6 Create `CustomerModuleApplication.java` main class
- [ ] 1.7 Create `application-test.yml` for testing
- [ ] 1.8 Create database schema file `V1__create_customer_schema.sql`

**Verification:** `./gradlew build` completes successfully

---

## Task 2: Domain Enums

**Estimated steps:** 5

- [ ] 2.1 Create `CustomerType.java` enum (CORPORATE, SME, INDIVIDUAL)
- [ ] 2.2 Create `CustomerStatus.java` enum (PENDING, ACTIVE, SUSPENDED, CLOSED)
- [ ] 2.3 Create `KYelevel.java` enum (STANDARD, ENHANCED, EXEMPT)
- [ ] 2.4 Create `KYCStatus.java` enum (NOT_STARTED, IN_PROGRESS, PENDING_REVIEW, APPROVED, REJECTED, EXPIRED)
- [ ] 2.5 Create `RelationshipType.java` enum (OWNER, DIRECTOR, AUTHORIZED_SIGNATORY, BENEFICIAL_OWNER)

**Verification:** All enums compile with no errors

---

## Task 3: Embeddable Types

**Estimated steps:** 5

- [ ] 3.1 Create `Address.java` embeddable (street, city, state, postalCode, country)
- [ ] 3.2 Create `PhoneNumber.java` embeddable (countryCode, number, type)
- [ ] 3.3 Create `AuditFields.java` embeddable (createdAt, createdBy, updatedAt, updatedBy)
- [ ] 3.4 Create `Money.java` embeddable (amount, currency) with banker's rounding
- [ ] 3.5 Create `PercentageRate.java` embeddable (value, basis) with precise decimal handling

**Verification:** All embeddables compile; unit tests for Money arithmetic

---

## Task 4: Customer Entity

**Estimated steps:** 6

- [ ] 4.1 Create `Customer.java` base entity with id, customerNumber, type, status, name, taxId, addresses, phones, emails, auditFields
- [ ] 4.2 Create `CorporateCustomer.java` entity extending Customer (registrationNumber, industry, annualRevenue, employeeCount, website)
- [ ] 4.3 Create `SMECustomer.java` entity extending Customer (registrationNumber, industry, businessType, annualTurnover, yearsInOperation)
- [ ] 4.4 Create `IndividualCustomer.java` entity (dateOfBirth, placeOfBirth, nationality, employerId, employmentStatus)
- [ ] 4.5 Add JPA lifecycle callbacks for audit fields
- [ ] 4.6 Create `CustomerRepository.java` with findByCustomerNumber, findByTaxId, findByStatus

**Verification:** Schema matches SQL migration; repository tests pass

---

## Task 5: EmploymentRelationship Entity

**Estimated steps:** 5

- [ ] 5.1 Create `EmploymentRelationship.java` entity (id, employee, employer, employeeNumber, department, position, startDate, endDate, status, createdAt)
- [ ] 5.2 Create `EmploymentStatus.java` enum (ACTIVE, TERMINATED, ON_LEAVE)
- [ ] 5.3 Create `BulkUploadRecord.java` entity for batch employee imports (id, employer, uploadedBy, totalRecords, processedRecords, status, createdAt)
- [ ] 5.4 Create `BulkUploadRecordRepository.java`
- [ ] 5.5 Create `EmploymentRelationshipRepository.java` with findByEmployeeAndStatus, findByEmployerAndStatus, findByEmployerAndEmployee

**Verification:** Repository tests pass; bulk upload entity audited

---

## Task 6: CustomerAuthorization Entity

**Estimated steps:** 4

- [ ] 6.1 Create `CustomerAuthorization.java` entity (id, customer, authorizedPerson, relationshipType, documents, isPrimary, effectiveDate, expirationDate, status)
- [ ] 6.2 Create `AuthorizationDocument.java` entity (id, authorization, documentType, documentReference, uploadedAt)
- [ ] 6.3 Create `DocumentType.java` enum (PASSPORT, ID_CARD, BOARD_RESOLUTION, POWER_OF_ATTORNEY, SPECIMEN_SIGNATURE)
- [ ] 6.4 Create `CustomerAuthorizationRepository.java` with findByCustomerAndStatus, findByAuthorizedPerson

**Verification:** Entity relationships mapped correctly; repository tests pass

---

## Task 7: KYC Entities

**Estimated steps:** 5

- [ ] 7.1 Create `KYCCheck.java` entity (id, customer, kycLevel, status, assignedOfficer, riskScore, dueDate, completedAt, nextReviewDate)
- [ ] 7.2 Create `KYCDocument.java` entity (id, kycCheck, documentType, documentReference, verificationStatus, verifiedBy, verifiedAt, expiryDate)
- [ ] 7.3 Create `KYCDocumentType.java` enum (CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_ASSOCIATION, PROOF_OF_ADDRESS, TAX_CERTIFICATE, PASSPORT, UTILITY_BILL, BANK_STATEMENT)
- [ ] 7.4 Create `SanctionsScreeningResult.java` entity (id, kycCheck, screenedAt, result, matchedNames, isCleared, clearedBy, clearedAt)
- [ ] 7.5 Create `KYCCheckRepository.java` with findByCustomerAndStatus, findByDueDateBefore

**Verification:** KYC entities audited and versioned; repository tests pass

---

## Task 8: Customer Service

**Estimated steps:** 7

- [ ] 8.1 Create `CustomerService.java` with createCorporate, createSME, createIndividual, updateCustomer, searchCustomers
- [ ] 8.2 Create `CustomerMapper.java` to convert between entities and DTOs
- [ ] 8.3 Implement customer number generation (format: CUST-YYYYMMDD-XXXXXX)
- [ ] 8.4 Add validation for customer creation (uniqueness of customerNumber, taxId)
- [ ] 8.5 Create `DuplicateCustomerException.java` custom exception
- [ ] 8.6 Implement search with pagination and filtering
- [ ] 8.7 Write unit tests for CustomerService (80% coverage target)

**Verification:** All unit tests pass; no integration tests yet

---

## Task 9: Employment Relationship Service

**Estimated steps:** 6

- [ ] 9.1 Create `EmploymentRelationshipService.java` with createEmployment, terminateEmployment, bulkUploadEmployees
- [ ] 9.2 Implement CSV parsing for bulk upload
- [ ] 9.3 Create `BulkUploadResult.java` DTO with success/failure details
- [ ] 9.4 Implement validation for employee-employer relationships
- [ ] 9.5 Write unit tests for EmploymentRelationshipService
- [ ] 9.6 Create `EmploymentRelationshipServiceIntegrationTest.java`

**Verification:** Unit tests pass; integration test with test database passes

---

## Task 10: KYC Service

**Estimated steps:** 6

- [ ] 10.1 Create `KYCService.java` with initiateKYC, submitDocuments, submitForReview, approveKYC, rejectKYC, scheduleReview
- [ ] 10.2 Create `KYCOfficerAssignmentService.java` for officer assignment logic
- [ ] 10.3 Implement sanctions screening integration stub
- [ ] 10.4 Create `KYCEventPublisher.java` for downstream notifications
- [ ] 10.5 Write unit tests for KYCService
- [ ] 10.6 Create `KYCServiceIntegrationTest.java`

**Verification:** Unit tests pass; integration test passes

---

## Task 11: Customer Authorization Service

**Estimated steps:** 5

- [ ] 11.1 Create `AuthorizationService.java` with createAuthorization, updateAuthorization, revokeAuthorization, getActiveAuthorizations
- [ ] 11.2 Implement document management stubs
- [ ] 11.3 Create expiration notification logic
- [ ] 11.4 Write unit tests for AuthorizationService
- [ ] 11.5 Create `AuthorizationServiceIntegrationTest.java`

**Verification:** Unit tests pass; integration test passes

---

## Task 12: REST API Controllers

**Estimated steps:** 7

- [ ] 12.1 Create `CustomerController.java` with POST/GET/PUT endpoints
- [ ] 12.2 Create `EmploymentController.java` with POST/bulk POST endpoints
- [ ] 12.3 Create `KYCController.java` with POST/GET/PUT endpoints
- [ ] 12.4 Create `AuthorizationController.java` with POST/GET/PUT endpoints
- [ ] 12.5 Add request validation with Jakarta Bean Validation
- [ ] 12.6 Add global exception handler `GlobalExceptionHandler.java`
- [ ] 12.7 Write integration tests for controllers using MockMvc

**Verification:** All controller tests pass; API contracts validated

---

## Phase 3: Frontend Implementation

---

## Task 13: Frontend Project Setup

**Estimated steps:** 6

- [ ] 13.1 Create `frontend/` directory with Vite + React 18 + TypeScript setup
- [ ] 13.2 Install dependencies: Ant Design, Redux Toolkit, React Query, React Router
- [ ] 13.3 Create Redux store with customer slice
- [ ] 13.4 Create API service layer with axios client
- [ ] 13.5 Create TypeScript types matching backend DTOs
- [ ] 13.6 Create layout components (AppLayout, Sidebar, Header)

**Verification:** Frontend builds without errors; TypeScript compiles clean

---

## Task 14: Customer Management Pages

**Estimated steps:** 7

- [ ] 14.1 Create `CustomerListPage.tsx` with search, filter, pagination
- [ ] 14.2 Create `CustomerDetailPage.tsx` with tabbed interface
- [ ] 14.3 Create `CorporateCustomerForm.tsx` for creation/editing
- [ ] 14.4 Create `SMECustomerForm.tsx` for creation/editing
- [ ] 14.5 Create `IndividualCustomerForm.tsx` for creation/editing
- [ ] 14.6 Create customer Redux slice with async thunks
- [ ] 14.7 Write component tests with React Testing Library

**Verification:** All pages render; component tests pass

---

## Task 15: Employment Management Pages

**Estimated steps:** 5

- [ ] 15.1 Create `EmploymentListPage.tsx` for employer's employees
- [ ] 15.2 Create `BulkUploadPage.tsx` with file upload and progress
- [ ] 15.3 Create `BulkUploadResults.tsx` for success/failure display
- [ ] 15.4 Create employment Redux slice with async thunks
- [ ] 15.5 Write component tests

**Verification:** All pages render; component tests pass

---

## Task 16: KYC Pages

**Estimated steps:** 6

- [ ] 16.1 Create `KYCStatusPage.tsx` showing current KYC status
- [ ] 16.2 Create `KYCDocumentUploadPage.tsx` with multi-document upload
- [ ] 16.3 Create `KYCReviewPage.tsx` for officers to review submissions
- [ ] 16.4 Create KYC Redux slice with async thunks
- [ ] 16.5 Add sanctions screening result display
- [ ] 16.6 Write component tests

**Verification:** All pages render; component tests pass

---

## Task 17: Authorization Pages

**Estimated steps:** 5

- [ ] 17.1 Create `AuthorizationListPage.tsx` showing active authorizations
- [ ] 17.2 Create `AuthorizationFormPage.tsx` for adding/editing authorizations
- [ ] 17.3 Create `DocumentUploadComponent.tsx` for supporting documents
- [ ] 17.4 Create authorization Redux slice with async thunks
- [ ] 17.5 Write component tests

**Verification:** All pages render; component tests pass

---

## Definition of Done

1. All 17 tasks completed with all steps checked off
2. Backend: `./gradlew build` passes with all tests green
3. Frontend: `npm run build` passes with no TypeScript errors
4. E2E tests: Core customer workflows pass
5. Code coverage: Service layer ≥80%, Controllers ≥70%
6. No linting errors (backend or frontend)
7. All code committed to `feature/foundation` branch

---

## Notes

- **Database:** PostgreSQL with Flyway migrations
- **ID Strategy:** Auto-increment IDs for all entities (BIGINT)
- **Events:** Spring ApplicationEvents for internal events, Kafka for external
- **Error codes:** Use `CUST-XXX` prefix for customer module errors
