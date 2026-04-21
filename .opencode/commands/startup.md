# Startup Command

## Description
Initializes the TradeFinance workflow system when starting a new session.

## Usage
```
/startup
```

## Actions
1. Creates `.opencode/workflow/` directory if not exists
2. Creates all workflow files (README, state, templates)
3. Creates custom commands if not exists
4. Displays current workflow state
5. Shows next steps

## Files Created
- `.opencode/workflow/README.md`
- `.opencode/workflow/state.md`
- `.opencode/workflow/QUICK_REFERENCE.md`
- `.opencode/workflow/phase1-analysis.md`
- `.opencode/workflow/phase2-discovery.md`
- `.opencode/workflow/phase3-design.md`
- `.opencode/workflow/phase4-implementation.md`
- `.opencode/workflow/phase5-validation.md`

## Example
```
> /startup
✓ Workflow directory initialized
✓ Workflow files created
✓ Custom commands ready

Current State: Ready for Phase 1: Analysis & Planning
Next: Use @plan analyze [requirement] with @trade-expert
```
