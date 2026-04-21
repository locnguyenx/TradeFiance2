---
description: generate a strict traceability report between the Testcases and the BDD specifications
---

**Role:** You are an expert `output-evaluator` and QA automation auditor.

**Context:** We have just completed the TDD (Red-Green-Refactor) phase based on our BDD specifications. I need you to perform an independent, rigorous traceability audit to ensure that our test suite comprehensively covers every business requirement without falling into "happy path bias" or generating tautological tests.

**Inputs:**
1. **Target BDD File:** BDD file inputted by Product Manager
2. **Target Test Directory:** test directory of current working component

**Your Task:**
Execute a strict code-to-requirement traceability audit and generate a "BDD Test Coverage & Quality Report". Do not write any new production code.


**The "Stateless Audit" Rule (CRITICAL)**
- **Do not rely on chat history, memory, or recent actions to determine progress.**
- You MUST perform a fresh, raw text search of the actual BDD files and test files to do audit

**Execution Steps:**
1. **Scenario and Business Rule Validation Extraction:** Read the BDD file and extract every single `Given-When-Then` scenario, separating them into "Happy Paths" and "Edge Cases", and every Business Rule/Validation. Ensure you include all Scenarios and usiness Rule/Validation, and maintain the exact numbering from the file.

2. **Traceability Mapping:** Scan the provided Test Directory. For each BDD scenario, find the exact Unit/Integration test function that implements it. 

3. **Artifact Paradox Analysis (Critical):** Do not just check if a test exists. Analyze the logic of test code fully comply all business logic / condition / action in Given-When-Then part of BDD. MUST Analyze the *quality* of the test assertions. Actively flag:
   * **Tautological Tests:** Tests that assert `true == true` or heavily mock the core logic so it never actually tests the requirement.
   * **Missing Assertions:** Tests that execute the code but fail to assert the specific `Then` condition from the BDD. The `Then` condition may contains multi assertions and need to be implemented all.
   * **Paradoxical Success:** Tests that pass because they only check the "Surface State" (e.g. status) while ignoring the "Deep State" (e.g. GL entries, CBS holds, audit logs).
   * **Missing Negative Verification:** Scenarios with "restricted", "not allowed", or "mandatory" keywords that lack a corresponding failure test.

4. **Generate Report:** Output a structured Markdown report containing:
   * **Coverage Matrix:** A clear table mapping [BDD Scenario Name] -> [Test Function Name] -> [Test Spec Name] -> [Status: Covered / Missing / Flawed] -> [Details in case of Flawed].
   * **Missing Scenarios / Business Rule / Validation:** A list of BDD scenarios that have zero corresponding tests.
   * **Test Quality Warnings:** Specific warnings for any tests that look polished but fail to enforce the business logic properly.
   * **Test Execution Summary** show table of Status | Count | Percentage



* "Citations Mandatory" rule:

For every Covered status, the auditor must cite the specific line number for EVERY assertion required by the Then clause.

**BDD file from Product Manager:**
"""
{{input}}
"""
**Report Location:** `docs/tcd`