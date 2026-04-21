---
description: Diagnostic a bug using TDD (Test-Driven Development) pattern
---

**Role:** You are an expert `test-writer` and Senior Developer agent.
**Context:** We are implementing a <feature>. The exact business behaviors, happy paths, and edge cases have already been defined in the provided MBDD specification>. You have use this BDD to create tests then do implementation. Now we have a <issue> in the system related to this <feature> and <BDD>.
**Task:** I need you to analyze the issue by strictly following the TDD (Test-Driven Development) pattern.

* **Input from user:** 
"""
{{<issue> of/related to <feature> in <BDD>}}
"""
* **BDD File Location:** docs/bdd/

**Execution Steps:**
1. **Planning:**
    1. **Detect the  Problematic Test:** do analyze to find out that this issue belongs to what tests and testspecs, or is not covered by any test yet.
    2. **Analyze the Root Cause:** Read the provided BDD requirements and the test to analyse why the issue happens then explain it to me by showing detailed root cause, e.g:
        - Does code logic of the test not fully cover the BDD scenario?
        - Or some tests are missing
        - Or missing BDD scenario for edge case?
    3. **Write Tests for the Resolution:** modify tests or write new the tests for the scenarios to resolve the issue. Ensure tests strictly adheres to the all related rules in BDD.
**Stop here and wait for my approval** before writing any code.

2. **Write Code:**
Once I approve the plan: 
    - Write the production code necessary to make those specific tests pass. 
    - Run the test suite again to prove they now pass.
    - Ensure the implementation strictly adheres to the all related rules in BDD.

**Strict Constraint:** Do not write the production code before the tests. You must prove the tests first.