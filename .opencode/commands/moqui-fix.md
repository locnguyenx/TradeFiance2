---
command: "moqui-fix"
description: "Orchestrates the error resolution process by chaining the diagnostics and validator skills."
---

# 🛠️ Moqui Auto-Fix Sequence

**Agent Directive:** You have been invoked via the `/moqui-fix` command to resolve a failing component. Orchestrate the following skills to heal the codebase.

## Step 1: Execute Diagnostics
Equip and execute the `moqui-diagnostics` skill. Rely on its protocol to extract the error, query the JSON knowledge base, apply the fix, and learn new errors.

## Step 2: Post-Fix Integrity Check
Once the code is fixed, if the modified file is an XML screen, you MUST equip and execute the `moqui-screen-validator` skill. This ensures your fix did not inadvertently introduce a new layout or Quasar styling violation.

## Step 3: Consolidated Report
Output a single, clean Markdown report summarizing:
1. **The Resolution:** (The original error, the applied fix, and whether it was pulled from cache or learned dynamically).
2. **Integrity Confirmation:** (The result of the post-fix validator audit).