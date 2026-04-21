---
description: Convert testcases to standard Gherkin BDD scenarios
---
**Role:** You are a Senior Developer and Agile Product Owner.

**Task:** The Developer has built a system meeting business requirements and created Testcase scripts. Reverse the testcases related to this business requirement into BDD Scenarios using the Gherkin format.

**Requirements:**

1. **BDD Scenarios:** Write BDD Scenarios documentation using the `Given` - `When` - `Then` structure.
2. **Structure:** Follow the BDD Template structure.
3. **Business Rules:** Clearly state validation rules.
4. **Format:** Present clearly in natural language, bold the Gherkin keywords. DO NOT use implementation code in the document.

**Business Requirement ID from BA:**
"""
{{input}}
"""
Template file for the BDD is BDD_Template.md in folder `.opencode/templates/`
Save BDD Scenarios file to `docs/bdd/`, filename pattern BDD_\<Req Id\>_\<Req Name\>_reversed.md