---
name: moqui-screen-validator
description: Used in screen design, diagnostics screen issue. 
---

## Execution Steps
1. **Knowledge Retrieval:** Read the syntax dictionary at `.agents/knowledge/moqui_syntax_ref.md`.
2. **XML Validation (MANDATORY):** Run `xmllint --noout <file-path>` to validate XML syntax BEFORE any other checks.
   - Common errors that break screens: duplicate field definitions, unclosed/extra closing tags
   - This MUST be done for EVERY screen file modification
3. **Entity Field Verification (MANDATORY):** When using entity-options or references:
   - Always verify the entity exists in `entity/TradeFinanceEntities.xml`
   - Verify the field exists in the entity definition before using it
   - Common mistakes: Using fields that don't exist, wrong field names
4. **XSD Boundary Audit:** Scan the target XML file. Ensure no HTML (`<div>`) or Quasar (`<q-btn>`) tags exist directly in the XML tree.
5. **Encapsulation Audit:** Verify any client-side code is wrapped perfectly in `<text type="html"><![CDATA[ ... ]]></text>`.
6. **Deep Verification:** If a Moqui tag is missing from your `moqui_syntax_ref.md` cache:
   - Read `framework/xsd/xml-screen-3.xsd`. If the tag is absent, flag it as an illegal hallucination.
   - Check `runtime/template/screen-macro/` for attribute verification.
7. **Self-Correction:** If you validated a new tag via Step 4, append a new row to the Markdown table in `.agents/knowledge/moqui_syntax_ref.md` detailing the tag, parent, and attributes.
8. **Report:** Output a Markdown summary of violations fixed and/or new tags learned.