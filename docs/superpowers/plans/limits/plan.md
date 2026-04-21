# Limits Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement the Limits Management module providing limit configuration, multi-level assignment (product/customer/account), real-time limit enforcement, and approval workflow.

**Architecture:** Foundation module consumed by business modules for real-time transaction validation. Resolution order: Account → Customer → Product (most specific wins). Critical path optimized for < 10ms.

**Tech Stack:** Java 17, Spring Boot 3.2, Spring Data JPA, PostgreSQL, Flyway, React 18, TypeScript, Ant Design, Redux Toolkit

---

## File Structure

```
src/main/java/com/banking/limits/
├── domain/
│   ├── entity/
│   │   ├── LimitDefinition.java
│   │   ├── ProductLimit.java
│   │   ├── CustomerLimit.java
│   │   ├── AccountLimit.java
│   │   ├── ApprovalThreshold.java
│   │   ├── LimitUsage.java
│   │   └── ApprovalRequest.java
│   ├── enums/
│   │   ├── LimitType.java
│   │   ├── LimitStatus.java
│   │   ├── LimitCheckResult.java
│   │   └── ApprovalStatus.java
│   └── embeddable/
│       └── AuditFields.java
├── repository/
├── service/
├── controller/
├── dto/
├── mapper/
└── exception/

src/main/resources/db/migration/
├── V9__create_limits_schema.sql
└── V10__seed_limits.sql

frontend/src/
├── types/limit.types.ts
├── services/limitService.ts
├── store/slices/limitSlice.ts
└── pages/limits/
```

---

## Task 1: Scaffold + Enums + AuditFields
- Create `AuditFields`, `LimitType`, `LimitStatus`, `LimitCheckResult`, `ApprovalStatus` enums

## Task 2: LimitDefinition + CRUD
- Entity, repository, service, controller, DTOs, exceptions, tests
- BDD: S1.1-S1.5

## Task 3: ProductLimit + CustomerLimit + AccountLimit
- 3 entities, 3 repositories, services, controllers
- BDD: S2.1-S2.2, S3.1-S3.2, S4.1-S4.2

## Task 4: LimitCheckService (critical path)
- Resolution logic: Account → Customer → Product
- BDD: S3.2, S4.2, S5.1-S5.8

## Task 5: LimitUsage + UsageService
- Period-based usage tracking, daily reset job
- BDD: S5.4, S5.5

## Task 6: ApprovalThreshold + ApprovalRequest + ApprovalService
- BDD: S6.1-S6.5, S7.1-S7.5

## Task 7: ExceptionHandler + QueryService

## Task 8: Flyway migrations + seed

## Task 9: Integration tests

## Task 10: Frontend (types, service, Redux, pages)

## Task 11: Run all tests & verify
