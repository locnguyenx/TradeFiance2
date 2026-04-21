# BDD Feature Template

## Structure
```
Feature: [Domain] [Business Rule]
  As a [role]
  I want [capability]
  So that [benefit]

  Scenario: [Specific Case]
    Given [initial context]
    When [event occurs]
    Then [expected outcome]
    And [additional outcome]
```

## Guidelines
1. **Feature Title**: Clear, descriptive, business-focused
2. **User Story**: As [role], I want [capability], so that [benefit]
3. **Scenarios**: Specific examples of behavior
4. **Given**: Initial context and preconditions
5. **When**: Action or event that occurs
6. **Then**: Expected outcomes and assertions
7. **And**: Additional outcomes or conditions

## Coverage Requirement
**100% Coverage**: Every business rule must have at least one BDD scenario.
- Positive cases
- Negative cases
- Edge cases
- Boundary conditions

## Example
```
Feature: Import LC Credit Limit Validation
  As a Trade Finance Officer
  I want to validate credit limits for Import LC applications
  So that we don't exceed customer credit exposure

  Scenario: LC amount within credit limit
    Given customer has credit limit of $1,000,000
    When Import LC application for $500,000 is submitted
    Then LC application should be approved
    And credit limit should be reduced by $500,000
```
