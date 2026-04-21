# Business Requirement Document: Fix Module Boundary Violations

**Date:** 2026-03-23  
**Status:** Draft  
**Module:** Cross-cutting concern (all modules)  

## 1. Executive Summary

This document addresses architectural violations where modules are incorrectly depending on internal implementation details of other modules instead of using their published APIs. Specifically, the Account module has been found to directly import and use CustomerRepository instead of going through the CustomerQueryService interface, creating tight coupling that violates the modular monolith architecture principles.

## 2. Business Goals

1. **Maintain Architectural Integrity:** Ensure all modules adhere to the defined dependency rules where foundation modules only expose their API and DTO packages
2. **Reduce Coupling:** Eliminate tight coupling between modules to allow independent evolution
3. **Improve Maintainability:** Prevent cascading changes when internal implementations of modules change
4. **Enforce Module Boundaries:** Ensure that JPA relationships and direct entity references only occur within the same module

## 3. Problem Statement

During recent development, it was discovered that the AccountInquiryService.java file in the Account module was directly importing and using `com.banking.customer.repository.CustomerRepository` instead of the proper API interface `com.banking.customer.api.CustomerQueryService`. This violates the architectural principle that:

> Each module must expose services in `<module>.api` package
> Data transfer objects go in `<module>.dto` package  
> Other modules must ONLY import from `<module>.api` or `<module>.dto`
> Never import `<module>.domain.entity`, `<module>.repository`, or internal implementation packages

Similar violations were found in:
- DemoDataLoader.java: Direct imports of product domain entities and repositories
- LoanAccount.java: Direct import of customer domain entity

## 4. Stakeholders

- **Primary:** Backend developers working on account, customer, product, limits, charges, and master-data modules
- **Secondary:** Frontend developers who consume the APIs
- **Tertiary:** DevOps and QA teams who benefit from more stable interfaces

## 5. Functional Requirements

### 5.1 Account Module Corrections
1. AccountInquiryService must use CustomerQueryService instead of CustomerRepository
2. DemoDataLoader must use ProductQueryService instead of direct ProductRepository/ProductVersionRepository
3. DemoDataLoader must use CustomerQueryService for customer lookups (already correct)
4. LoanAccount must not have direct dependencies on Customer domain entities

### 5.2 Architectural Compliance
1. All modules must only depend on the API and DTO packages of other modules
2. No module should have direct dependencies on another module's:
   - domain.entity package
   - repository package  
   - service package (internal implementations)
   - config package
3. Cross-module references must use Long IDs or String identifiers + service lookup

## 6. Non-Functional Requirements

### 6.1 Performance
- No degradation in performance due to the refactoring
- Service lookup overhead should be minimal

### 6.2 Security
- No security implications from this change

### 6.3 Reliability
- Improved reliability through loose coupling
- Reduced risk of cascading failures

### 6.4 Maintainability
- Easier to modify internal implementations without affecting other modules
- Clearer module boundaries reduce cognitive load

## 7. User Stories

### 7.1 As a backend developer, I want to modify the internal implementation of the Customer module without breaking the Account module, so that we can evolve modules independently.

### 7.2 As an architect, I want to enforce clean module boundaries so that the system maintains its modular monolith benefits.

### 7.3 As a QA engineer, I want reduced coupling between modules so that changes in one module have minimal impact on others.

## 8. Constraints and Assumptions

### 8.1 Constraints
- Must maintain backward compatibility where possible
- Cannot break existing functionality
- Must follow existing code patterns and conventions

### 8.2 Assumptions
- All required API interfaces already exist and are properly implemented
- The CustomerQueryService, ProductQueryService, etc. have all necessary methods
- DemoDataLoader is only used for development/demo purposes and can be refactored freely

## 9. Acceptance Criteria

1. All improper cross-module dependencies are removed
2. Modules only interact through published API interfaces
3. All existing functionality continues to work as expected
4. No new architectural violations are introduced
5. Unit and integration tests pass after refactoring

## 10. Dependencies

- None - this is a refactoring effort to correct existing code