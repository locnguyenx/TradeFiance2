---
command: "apply-tdd"
description: "Execute TDD (Red-Green-Refactor) based on a provided BDD specification."
---
# Apply-TDD (Red-Green-Refactor) workflow

## Key Definitions
**Role:** You are an expert `test-writer` and Senior Developer agent.
**Context:** We are implementing a feature. The exact business behaviors, happy paths, and edge cases have already been defined in the provided BDD specification.
**Task:** I need you to implement the feature described in the provided BDD by strictly following the TDD (Test-Driven Development) Red-Green-Refactor workflow.

* **Target BDD File/Content:** 
"""
{{provided BDD}}
"""
* **BDD File Location:** docs/bdd/

## Execution Steps:
### 1. **Analyze & Plan (Plan Mode):** 

Read the provided BDD requirements. Outline the exact Unit Tests you plan to write to cover these scenarios, business rules & validation. In case Unit Tests for this requirement already exist, revise them.

**BDD-to-Test Mapping Protocol (BTMP):**
To ensure 100% verification quality, I propose the following 3-step protocol for test preparation:

* **Step 1: Scenario Decomposition (Assertion Mapping)**
Map every Gherkin Then and And step to a specific code-level assertion. Example:
    BDD: Then system should place a formal Provision Hold...
    -> Assertion: assert prov.cbsHoldReference != null && prov.provisionStatusId == 'LcPrvHeld'

* **Step 2: Negative/Constraint Testing (Rule Enforcement)**
For every business rule that says "is NOT allowed" or "MUST NOT", create a negative test case. Example:
    Requirement: Provision amount is not allowed to be manually adjusted.
    -> Test Case: Attempt to update provisionAmount via update#LcProvision and assert that the service returns an error or ignores the update.

* **Step 3: Atomic assertions**
If one Gherkin Then have multiple expectation, map each of them into 1 assertions. Example:
    BDD: Then system automatically calculates charge and provision amount
    -> Assertion: 
        assert lc.chargeAmount == 10
        assert lc.provAmount == 500

* **Step 4: Traceability Documentation**
Use structured comments or annotations in tests to link back to BDD scenario IDs.
Example:
```groovy
// @Scenario(BDD-R8.11-SC5)
def "verify contingent accounting on issuance"() { ... }
```

* **🧠 Breaking the Verification Paradox (Excellence Rule)**
The "Verification Paradox" occurs when a test passes but the core requirement is missing. To prevent this:
1. **Side-Effect Verification**: If a process causes a status change, you MUST verify the side-effects of that status (e.g., GL entries, Audit logs, Fund release). Never assert status alone.
2. **State Integrity**: Refresh entity values (`refresh()`) before assertions to ensure you are testing the database state, not the in-memory mock.

**Stop here and wait for my approval** before writing any code.

### 2. **RED Phase (Write Tests First):** 
Once I approve the plan, write the Unit Tests for the scenarios. Run the test suite to prove that these new tests fail (since the actual feature is not built yet).

### 3. **GREEN Phase (Write Code):** 
Write the minimal production code necessary to make those specific tests pass. Run the test suite again to prove they now pass.

### 4. **REFACTOR Phase:** 
Clean up the production code and test code. Ensure the implementation strictly adheres to the all related rules in BDD.

**Strict Constraint:** 
Do not write the production code before the tests. You must prove the tests fail first.
