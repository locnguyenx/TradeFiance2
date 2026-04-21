# Plan Mode Templates

This directory contains templates for the 4-phase business requirement analysis workflow.

## Structure

```
.opencode/plans/
├── templates/           # Template files (copy for each feature)
│   ├── 01-business-requirements.md
│   ├── 02-current-state.md
│   ├── 03-future-state.md
│   └── 04-technical-discovery.md
└── [feature-name]/      # Feature-specific plans
    ├── 01-business-requirements.md
    ├── 02-current-state.md
    ├── 03-future-state.md
    └── 04-technical-discovery.md
```

## How to Use

### 1. Create a new feature plan directory
```bash
mkdir -p .opencode/plans/[feature-name]
```

### 2. Copy templates
```bash
cp .opencode/plans/templates/*.md .opencode/plans/[feature-name]/
```

### 3. Update feature name in each template
Replace `[FEATURE NAME]` with the actual feature name.

### 4. Run phase commands

**Phase 1 - Business Understanding:**
```
@reasoning Analyze the business requirement: "[DESCRIBE REQUIREMENT]" and save output to .opencode/plans/[feature-name]/01-business-requirements.md using the business requirements template.
```

**Phase 2 - Current State Analysis:**
```
@explore Find all entities, services, and screens related to [KEYWORDS] in TradeFinance component
@reasoning Analyze the findings against .opencode/plans/[feature-name]/01-business-requirements.md and save to .opencode/plans/[feature-name]/02-current-state.md
```

**Phase 3 - Future State Definition:**
```
@plan Design the future state process flow based on requirements and current state
@reasoning Assess risks and save to .opencode/plans/[feature-name]/03-future-state.md
```

**Phase 4 - Technical Discovery:**
```
@tech_designer Design the technical solution based on future state
@plan Create implementation roadmap and save to .opencode/plans/[feature-name]/04-technical-discovery.md
```

## Agent Reference

| Agent | Model | Purpose |
|-------|-------|---------|
| @explore | Built-in | Code search, navigation |
| @plan | Built-in | Planning, analysis |
| @reasoning | MiMo V2 Flash Free | Analysis, reasoning |
| @tech-designer | MiniMax M2.5 Free | Technical design |
| @trade-expert | Custom | Trade finance domain |

## Example: Import LC Provision

```
mkdir -p .opencode/plans/import-lc-provision
cp .opencode/plans/templates/*.md .opencode/plans/import-lc-provision/
```

Then run the phase commands as shown above.
