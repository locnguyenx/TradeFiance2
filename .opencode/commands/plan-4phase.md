---
command: "plan-4phase"
description: "Start 4-phase business requirement analysis workflow using specialized agents"
---
# Plan Mode-4phases Command

## Overview
This command executes a 4-phase planning workflow for requirement inputted from the Product Manager, using specialized agents. Each phase produces output files that serve as input for subsequent phases.

**Input requirement from Product Manager:**
"""
{{input}}
"""

## Workflow Structure
workflow with agent used:
Phase 1: Business Understanding (@reasoning)
    ↓
Phase 2: Current State Analysis (@explore + @reasoning)
    ↓
Phase 3: Future State Definition (@plan + @reasoning)
    ↓
Phase 4: Technical Discovery (@tech-designer + @plan)

---

## Templates Location
- `01-business-requirements`: `.opencode/plans/templates/01-business-requirements.md`
- `02-current-state`: `.opencode/plans/templates/02-current-state.md`
- `03-future-state`: `.opencode/plans/templates/03-future-state.md`
- `04-technical-discovery`: `.opencode/plans/templates/04-technical-discovery.md`

## Feature Location (Input/Output Files)
**Feature Location:** `.opencode/plans/[feature-name]/`
All input/output files are stored in Feature Location

---

## Phase 1: Business Understanding

**Agent**: @reasoning (MiMo V2 Flash Free)

**Purpose**: Capture "what" and "why" - NOT "how"

**Input**: User describes the business requirement

**Output**: `01-business-requirements.md`

**Template**: `01-business-requirements`

**Agent Command**:
```
@reasoning Analyze the business requirement: "[DESCRIBE REQUIREMENT]" and save output to .opencode/plans/[feature-name]/01-business-requirements.md
```

---

## Phase 2: Current State Analysis

**Agents**: 
- @explore (MiniMax M2.5 Free) - Finding code
- @reasoning (MiMo) - Analysis

**Purpose**: Understand what exists today without assumptions

**Input**: `01-business-requirements.md`

**Output**: `02-current-state.md`

**Template**: `02-current-state`

**Agent Commands**:
```
@explore Find all entities, services, and screens related to [KEYWORDS] in TradeFinance component
@reasoning Analyze findings against .opencode/plans/[feature-name]/01-business-requirements.md and save to 02-current-state.md
```

---

## Phase 3: Future State Definition

**Agents**:
- @plan (Nemotron 3 Super Free) - Process modeling
- @reasoning (MiMo) - Risk analysis

**Purpose**: Define target state clearly

**Input**: 
- `01-business-requirements.md`
- `02-current-state.md`

**Output**: `03-future-state.md`

**Template**: `03-future-state`

**Agent Commands**:
```
@plan Design the future state process flow based on requirements in 01-business-requirements.md and current state in 02-current-state.md
@reasoning Assess risks and save to 03-future-state.md
```

---

## Phase 4: Technical Discovery

**Agents**:
- @tech-designer (MiniMax M2.5 Free) - Technical design
- @plan (Nemotron) - Implementation planning

**Purpose**: Translate to technical terms (NOT implementation)

**Input**:
- `01-business-requirements.md`
- `02-current-state.md`
- `03-future-state.md`

**Output**: `04-technical-discovery.md`

**Template**: `04-technical-discovery`

**Agent Commands**:
```
@tech_designer Design the technical solution based on future state in 03-future-state.md
@plan Create implementation roadmap and save to 04-technical-discovery.md
```
---

## Usage

### Step 1: Create Feature Directory
```bash
mkdir -p .opencode/plans/[feature-name]
```

### Step 2: Copy Templates
```bash
cp .opencode/plans/templates/*.md .opencode/plans/[feature-name]/
```

### Step 3: Update Feature Name
Replace `[FEATURE NAME]` in all template files with actual feature name

### Step 4: Execute Phase Commands
Run each phase command sequentially, waiting for completion before proceeding

---

## Agent Reference

| Agent | Model | Purpose |
|-------|-------|---------|
| @reasoning | MiMo V2 Flash Free | Analysis, reasoning |
| @explore | Built-in | Code finding |
| @plan | Built-in | Planning |
| @tech-designer | MiniMax M2.5 Free | Technical design |

---

## Constraints

- **Plan Mode**: NO code edits during planning phases
- **Validation**: Each phase ends with user validation before proceeding
- **Persistence**: All outputs must be saved to plan files for subsequent phases

---

## After Planning
After Phase 4 complete, proceed to following stages:
1. Create BDD from expected future state and technical discovery: 
"@general Create BDD for [feature] using template at `.opencode/templates/BDD_Template.md`, then store BDD file in feature dir"
2. Implementation:
After BDD is created and reviewed by user. Request use to change to build mode and Execute TDD: `/tdd`
