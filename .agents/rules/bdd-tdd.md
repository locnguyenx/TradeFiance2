---
glob: runtime/component/TradeFinance/**
---
# BDD/TDD Rules for TradeFinance

## Coverage Requirement
- **100% test coverage** for all business rules
- BDD scenarios for every business rule
- TDD tests for every scenario

## Phase Integration
- **Phase 1**: Define BDD scenarios with @trade-expert
- **Phase 2**: Discover existing tests with @explore
- **Phase 3**: Design test framework with @tech-designer
- **Phase 4**: Implement tests in Build Mode
- **Phase 5**: Validate coverage with @trade-expert

## BDD Structure
- Given-When-Then format
- Positive, negative, and edge cases

## TDD Structure
- Test files in `src/test/groovy/[domain]/`
- Red-Green-Refactor cycle
- One assertion per test
