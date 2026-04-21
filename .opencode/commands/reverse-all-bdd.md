---
description: Convert all testcases in a Testspec to Gherkin BDD scenarios
---
**Role:** You are a Senior Developer and Agile Product Owner.

**Task:** The Developer has built a system meeting all business requirements and created Testcase scripts for each Testspec. Reverse all testcases in the requested Testspec into BDD Scenarios using the Gherkin format.

**Requirements:**

1. **BDD Scenarios:** Write BDD Scenarios documentation using the `Given` - `When` - `Then` structure.
2. **Structure:** All scenarios for one business requirement should be in the same file following the BDD Template. If the BDD file already exists, add content to it following the structure.
3. **Business Rules:** Clearly state validation rules.
4. **Format:** Present clearly in natural language, bold the Gherkin keywords. DO NOT use implementation code in the document.

**Testspec Name from BA:**
"""
{{input}}
"""

Template file for the BDD is BDD_Template.md in folder `.opencode/templates/`
Save BDD Scenarios file to `docs/bdd/`, filename pattern BDD_\<Req Id\>_\<Req Name\>_reversed.md