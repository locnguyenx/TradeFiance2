# TDD Test Template

## Structure
```
Test: [Expected Behavior]
  Given: [Initial state]
  When: [Action]
  Then: [Assertion]
```

## Guidelines
1. **Test Name**: Descriptive of expected behavior
2. **Given**: Setup initial state and test data
3. **When**: Execute the action under test
4. **Then**: Assert expected outcomes
5. **One Assertion Per Test**: Focus on single behavior
6. **Test Data Builders**: Use builders for complex objects

## Coverage Requirement
**100% Coverage**: Every BDD scenario must have corresponding TDD tests.
- Every scenario → TDD tests
- Every edge case → TDD tests
- Every boundary condition → TDD tests

## Red-Green-Refactor Cycle
1. **Red**: Write a failing test
2. **Green**: Write minimal code to pass test
3. **Refactor**: Improve code while keeping tests passing

## Example
```
Test: Should approve LC within credit limit
  Given: Customer with credit limit of $1,000,000
  When: LC application for $500,000 is validated
  Then: Application should be approved
  And: Remaining credit limit should be $500,000
```
