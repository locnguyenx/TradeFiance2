# Quick Start Guide - OpenCode Agent System

This guide provides quick reference for using the OpenCode agent system in TradeFinance development.

## Getting Started

### Default Agent: Plan
- OpenCode starts in **Plan** mode (analysis)
- Press **Tab** to switch to **Build** mode
- Press **Tab** again to switch back

### Agent Invocation
- Use **@agent-name** to invoke subagents
- Example: `@trade-expert explain LC validation`

## Quick Reference

| Task | Agent | Command |
|------|-------|---------|
| Analyze requirements | `plan` | Start session |
| Get domain rules | `trade-expert` | `@trade-expert explain Import LC` |
| Find code | `explore` (built-in) | `@explore find TradeFinance entity` |
| Design solution | `tech-designer` | `@tech-designer design LC entity` |
| Complex planning | `plan` (built-in) | `@plan design workflow` |
| Analysis | `reasoning` | `@reasoning analyze gaps` |
| Implement | `build` | Tab to switch |

## Common Workflows

### Workflow 1: New Feature
```
[plan] Analyze requirements
  → @trade-expert domain rules
  → @explore find existing code
  → @tech-designer design patterns
[build] Implement feature
```

### Workflow 2: Bug Fix
```
[plan] Analyze issue
  → @explore locate bug
  → @reasoning analyze cause
[build] Fix bug
```

### Workflow 3: Planning
```
[plan] Define scope
  → @plan create roadmap
  → @trade-expert validate rules
  → @tech-designer design approach
```

## Agent Models

| Agent | Model | Best For |
|-------|-------|----------|
| plan (built-in) | Nemotron 3 Super | Long analysis, planning |
| build (built-in) | MiMo V2 Flash | Coding |
| trade-expert | MiMo V2 Flash | Trade finance domain |
| reasoning | MiMo V2 Flash | Analysis |
| tech-designer | MiniMax M2.5 | Moqui technical design |
| explore (configured) | MiniMax M2.5 | Code search |
| general (built-in) | Inherited | General tasks |

## Trade Finance Commands

### Import LC
```bash
@trade-expert explain Import LC validation rules
@trade-expert list UC1-UC7 use cases
```

### Code Search
```bash
@explore find TradeFinance entities
@explore find create#LetterOfCredit service
```

### Design
```bash
@tech-designer design Import LC entity
@tech-designer create LC screen XML
```

### Planning
```bash
@plan design LC approval workflow
@reasoning analyze LC vs Guarantee differences
```

## Tips

1. **Always start with plan** - Analyze before implementing
2. **Use domain experts** - @trade-expert for business rules
3. **Find code first** - @explore before changes
4. **Design second** - @tech-designer for patterns
5. **Implement last** - Switch to build for coding

## Model Selection Summary

- **Nemotron**: Primary agents (plan, build)
- **MiMo**: Custom agents (trade-expert, reasoning)
- **MiniMax**: Technical design (tech-designer)

See AGENTS.md for complete documentation.
