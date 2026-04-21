# Project Rules

## Overview
This project is to build a TradeFinance system

## Architecture Principles
- **Architecture**: Moqui Framework
- **Project Root:** `../../`
- **Working Component (TradeFinance component):**: `<Project Root>runtime/component/TradeFinance/`

## SCOPE LOCKDOWN (CRITICAL)
You are allowed to edit files only in the `TradeFinance` component.
**FORBIDDEN**: You are STRICTLY FORBIDDEN from modifying any files outside of the `TradeFinance` component, by any file manipulation command/tool, regardless of mode

**Exception**:
- Use gradle tasks for database operations

**Git Boundary**: All commits must target only the TradeFinance component directory.

---
## Coding Compliance (Mandatory)
- **Pre-Flight Check:** Before performing any entity, screen, service creation or modification, you MUST:
    1. Read the relevant section of moqui rules.
    2. Check `.agents/knowledge/` for related patterns and lessons learned.
    3. Confirm that the proposed code change aligns with the architecture, related rules, patterns.

---

## REFERENCES

### Rules Files
- Session and memory management: @.agents/rules/session_protocol.md
- TradeFinance-specific rules: @.agents/rules/trade-finance.md
- Service rules: @.agents/rules/moqui-services.md
- Screen rules: @.agents/rules/moqui-screens.md
- Entity rules: @.agents/rules/moqui-entities.md
- Test rules: @.agents/rules/moqui-tests.md
- Troubleshooting guide: @.agents/rules/moqui-troubleshooting.md

### Knowledge Base (Reference Data)
- Testing patterns and traps: @.agents/knowledge/moqui-testing.md
- Entity architectural patterns: @.agents/knowledge/moqui-entity-patterns.md
- Service patterns: @.agents/knowledge/moqui-service-patterns.md
- UI architectural patterns: @.agents/knowledge/moqui-ui-patterns.md
- Testing & Integration patterns: @.agents/knowledge/moqui-other-patterns.md
- XML tag reference: @.agents/knowledge/moqui_syntax_ref.md
- Error diagnostics cache: @.agents/knowledge/moqui-errors.json

---

## GIT WORKFLOW

### Pre-commit Checks
- Run `./gradlew test` for affected components
- Validate XML files with xmllint
- Check for TODO/FIXME comments that should be addressed
- Verify no debug logging statements remain in production code