---
name: trade-finance-business
description: business rules and processing for trade finance
---

# Trade Finance Business Skill

This skill provides a set of business rules, patterns, other requirements and best practices for building trade finance solution

## When to use this skill
- When starting a new Moqui component or feature.
- When designing UI screens, especially dashboards and data grids.
- When configuring security and authorization rules.
- When troubleshooting service resolution or widget template errors.

## Business Requirements Document

The business requirement documents (BRD) are stored in dicrectory ./docs/brd

## Business Rules

### 1. LC Amendment (Shadow Record Model)
- **Concept**: Amendments do not modify the master LC directly during the "Pending" or "Submitted" phases. Instead, a "Shadow Record" (`LcAmendment`) is created with all amendable fields.
- **Application**: Only upon "Confirmation" (status `LcAmndConfirmed`) are the shadow fields copied back to the master `LetterOfCredit`.
- **Audit**: Every amendment creates a `LcHistory` entry for the master LC, documenting the change in values.

### 2. Drawing Lifecycle
- **Presentation**: Documents are received and registered as `LcDrawing` (Status: `LcDrReceived`).
- **Examination**: The bank examines documents against LC terms.
- **Compliance**:
    - **Compliant**: Status moves to `LcDrCompliant`.
    - **Discrepant**: Discrepancies are recorded (`LcDiscrepancy`). Status moves to `LcDrDiscrepant`.
- **Refusal**: If discrepant and not waived, a SWIFT MT734 Advice of Refusal MUST be generated.

## SWIFT MT messages
- **MT700**: Issuance of a Documentary Credit.
- **MT707**: Amendment of a Documentary Credit.
- **MT734**: Advice of Refusal.
- **MT756**: Advice of Reimbursement or Payment.

### General Validation Rules
- **SWIFT Character Set X**: All alphanumeric fields MUST only allow characters: `A-Z`, `a-z`, `0-9`, `/`, `-`, `?`, `:`, `(`, `)`, `.`, `,`, `'`, `+`, and `space`.
- **Date Format**: Standard ISO dates (YYYY-MM-DD), validated as per UCP 600 (e.g., Expiry > Issue).
