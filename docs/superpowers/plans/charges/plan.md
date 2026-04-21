# Charges Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement the Charges Management module providing fee definitions, tiered pricing, waivers, and interest accrual/application.

**Architecture:** Foundation module consumed by business modules for fee calculation. Resolution: Customer override → Product charge → default rule → waiver applied last.

**Tech Stack:** Java 17, Spring Boot 3.2, Spring Data JPA, PostgreSQL, Flyway, React 18, TypeScript, Ant Design, Redux Toolkit

---

## File Structure

```
src/main/java/com/banking/charges/
├── domain/
│   ├── entity/
│   │   ├── ChargeDefinition.java
│   │   ├── ChargeRule.java
│   │   ├── ChargeTier.java
│   │   ├── ProductCharge.java
│   │   ├── CustomerChargeOverride.java
│   │   ├── FeeWaiver.java
│   │   ├── InterestRate.java
│   │   ├── InterestTier.java
│   │   └── InterestAccrual.java
│   ├── enums/
│   │   ├── ChargeType.java
│   │   ├── CalculationMethod.java
│   │   ├── WaiverScope.java
│   │   ├── ChargeStatus.java
│   │   └── InterestSchedule.java
│   └── embeddable/
│       └── AuditFields.java
├── repository/
├── service/
├── controller/
├── dto/
├── mapper/
└── exception/

src/main/resources/db/migration/
├── V11__create_charges_schema.sql
└── V12__seed_charges.sql

frontend/src/
├── types/charge.types.ts
├── services/chargeService.ts
├── store/slices/chargeSlice.ts
└── pages/charges/
```

---

## Task 1: Scaffold + Enums + AuditFields
- Create `AuditFields`, `ChargeType`, `CalculationMethod`, `WaiverScope`, `ChargeStatus`, `InterestSchedule`

## Task 2: ChargeDefinition + CRUD
- Entity, repository, service, controller, DTOs, exceptions, tests
- BDD: S1.1-S1.5

## Task 3: ChargeRule + ChargeTier (calculation config)
- Flat, Percentage, Tiered Volume methods
- BDD: S2.1-S2.10

## Task 4: ProductCharge + CustomerChargeOverride
- BDD: S3.1-S3.2, S4.1-S4.2

## Task 5: FeeWaiver
- BDD: S5.1-S5.8

## Task 6: ChargeCalculationService (critical path)
- Fee calculation with waiver application
- BDD: S6.1-S6.4

## Task 7: InterestRate + InterestTier + InterestAccrual
- Fixed/tiered rates, daily accrual, periodic application
- BDD: S5a.1-S5a.10

## Task 8: ExceptionHandler

## Task 9: Flyway migrations + seed

## Task 10: Integration tests

## Task 11: Frontend (types, service, Redux, pages)

## Task 12: Run all tests & verify
