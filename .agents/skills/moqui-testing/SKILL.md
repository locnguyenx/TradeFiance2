---
name: moqui-testing
description: Use when executing Moqui Spock tests, parsing the generated HTML/XML reports to diagnose failures. Use this whenever you build or run tests.
---

## BUILD & TEST COMMANDS

### Full Build
```bash
./gradlew build
```

### Clean & Reload Data (when entity/data files change)
```bash
./gradlew cleanDb loadSave
```

### Full Clean Rebuild
```bash
./gradlew cleanAll loadSave
```

### Run Tests (ALWAYS use reloadSave first)
```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home ./gradlew reloadSave :runtime:component:TradeFinance:test
```

### Run Single Test Spec
```bash
./gradlew reloadSave :runtime:component:TradeFinance:test --tests moqui.trade.finance.TradeFinanceScreensSpec
```

### Run Specific Test Method
```bash
./gradlew reloadSave :runtime:component:TradeFinance:test --tests "moqui.trade.finance.TradeFinanceServicesSpec#testCreateTrade"
```

### Run All Tests for a Specific Feature Area
```bash
./gradlew reloadSave :runtime:component:TradeFinance:test --tests "*LcProvision*"
```

### Continuous Test Watching (during development)
```bash
./gradlew reloadSave --continuous :runtime:component:TradeFinance:test
```

### Generate Test Reports
```bash
./gradlew reloadSave :runtime:component:TradeFinance:test --info
```

### Run Only Unit Tests (excluding integration tests)
```bash
./gradlew reloadSave :runtime:component:TradeFinance:test --tests "*Spec" --exclude-test "*Integration*"
```

> [!CAUTION]
> Do NOT combine `reloadSave` with `load` or `loadSave` in the same command. `reloadSave` restores from the saved snapshot, which would undo the fresh load.

## Execution Steps

> [!IMPORTANT]
> **NEVER** run `java -jar moqui.war` for data loading

All data loading and test execution must use `./gradlew` only.

### 1. Environment Verification
- **Read the Law:** Open and read `.agents/knowledge/moqui-testing.md` to refresh your memory on the exact `./gradlew` execution mandates (specifically the `reloadSave` rule).
- **Verify Path:** Ensure you are targeting the correct component for testing (e.g., `TradeFinance`).

### 2. Execution
- Check if there's a running java process related to this moqui application. If YES then kill it.
- **Selective running**: if previous testsuite failed then only run the failed testspecs again.
- Use your bash/terminal tool to execute the test command.
- *Note:* If the user specifies they changed entity data recently, execute `cleanDb loadSave` first.

### 3. Parse the Results
Do not rely solely on the terminal output. If a test fails, use your file-reading tools to inspect the actual test reports:
- **XML Output:** Read the detailed failure traces in `runtime/component/[ComponentName]/build/test-results/test/`.
- **Log Check:** Check `runtime/log/moqui.log` for hidden entity or SQL errors that caused the failure.

### 4. Report & Auto-Diagnose
1. Output a final Markdown report to the user:
- State whether the suite passed or failed.
- If it failed, extract the specific assertion failure from the XML report.
- If the failure matches a known issue in the `moqui-testing.md` knowledge base (like a Type Coercion Trap or a Stale UI Cache), point it out and offer to fix the code.
2. Update the test coverage report in the component's `docs/tcd` directory.

### 5. Verification Integrity Audit (Mandatory)
Even if tests pass (GREEN), perform a manual text-scan of the Spec file to identify the **Verification Paradox**:
- **Surface-Only Assertions**: Flag tests that only check `statusId` but ignore financial side-effects (e.g., `AcctgTrans`).
- **Mock-Heavy logic**: Flag tests where the "When" block mocks the very logic that the "Then" block claims to verify.
- **Atomic Check**: Ensure every `Then` clause in the BDD has a corresponding `assert` in the code.
- **Negative Coverage**: Verify that business rules with "restricted" or "mandatory" keywords have a corresponding `RED` test case.