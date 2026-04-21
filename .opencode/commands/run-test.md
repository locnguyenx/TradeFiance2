---
description: Orchestrates the Moqui testing lifecycle, including optional database resets and executing the test runner skill.
---

# 🧪 Moqui Test Orchestrator

**Agent Directive:** You have been invoked via the `/run-test` command. Your job is to orchestrate the testing sequence for the active component by executing the `moqui-test-runner` skill.

## Step 1: Clarify Intent
Before running the tests, analyze the user's prompt:
- Did they ask for a "fresh load", "fresh data" or mention changing entity data? If so, you MUST execute `cleanDb loadSave` via terminal before proceeding.
- If not, proceed directly to the standard testing sequence.

## Step 2: Execute the Runner
Equip and execute the `moqui-test-runner` skill. Rely entirely on its internal protocol to read the `moqui-testing.md` knowledge base, execute the strict `reloadSave` Gradle command, and parse the XML test reports.

## Step 3: Consolidated Report
Output a clean, highly readable Markdown report summarizing:
1. **Environment State:** (Did you run a fresh load or just a `reloadSave` snapshot?)
2. **Results:** Pass/Fail metrics.
3. **Diagnostics:** If the test failed, provide the exact assertion failure from the XML report and cross-reference it with the traps listed in the testing knowledge base.