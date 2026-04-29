---
name: moqui-diagnostics
description: Used in diagnostics Moqui error stack traces, rendering errors, and validation failures.
---

## Execution Steps

### 1. Context Retrieval
- **Extract error:** Identify the core exception string (e.g., "SAXParseException", "formInstance was null").
- **Read Knowledge:** 
  1. Read `.journal/moqui-errors.json` for immediate matches.
  2. Read `.journal/moqui-ui-patterns.md`, `moqui-entity-patterns.md`, or `moqui-service-patterns.md` for architectural context, resolution, best practices.

### 2. Match & Apply
- If a match is found in the JSON or Markdown patterns:
  - Open the failing file.
  - Apply the exact fix documented.
  - Verify resolution and skip to Step 4.

### 3. Deep Diagnosis & Learning (Fallback)
- **If match fails:**
  1. **UI/XML:** Read `.journal/moqui_syntax_ref.md`. If still unknown, check `framework/xsd/xml-screen-3.xsd`.
  2. **Logic/Groovy:** Analyze the script against Moqui context objects (`ec`).
- **Resolve & Update:** After fixing a *new* error, you MUST update `.journal/moqui-errors.json` with a concise key-value pair describing the error and fix.

### 4. Reporting
Output a Markdown report:
1. **Error Analyzed:** Concise summary of the issue.
2. **Action Taken:** File(s) modified and the logic of the fix.
3. **Learning Status:** State if the fix was from cache or learned dynamically.
