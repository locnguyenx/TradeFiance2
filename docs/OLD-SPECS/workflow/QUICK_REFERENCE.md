# Workflow Quick Reference

## Current Mode: BUILD
**Status**: You are in Build Mode with full file access.

## Phase 1: Analysis & Planning
**Mode**: Plan (Tab)
**Command**: `@plan analyze [req] with @trade-expert`
**Output**: Session memory only
**Save**: `/save-workflow phase1-analysis "[content]"`

## Phase 2: Code Discovery
**Mode**: Plan (Tab)
**Command**: `@explore find [code] with @tech-designer`
**Output**: Session memory only
**Save**: `/save-workflow phase2-discovery "[content]"`

## Phase 3: Design
**Mode**: Plan (Tab)
**Command**: `@tech-designer design [comp] with @reasoning`
**Output**: Session memory only
**Save**: `/save-workflow phase3-design "[content]"`

## Phase 4: Implementation (CURRENT)
**Mode**: Build (Tab) - **YOU ARE HERE**
**Command**: Implement based on design
**Output**: Files created, tests run
**Save**: Auto-saved to phase4-implementation.md
**Action**: Read `phase3-design.md` and implement the design

## Phase 5: Validation
**Mode**: Plan (Tab)
**Command**: `@trade-expert validate [impl] with @explore`
**Output**: Session memory only
**Save**: `/save-workflow phase5-validation "[content]"`

## Mode Switching
- **Plan Mode**: Tab key (read-only, analysis only)
- **Build Mode**: Tab key (write-enabled, implementation)

## Quick Commands
- `/init-workflow` - Initialize workflow directory
- `/save-workflow [phase] [content]` - Save workflow output
- `/read-workflow [phase]` - Read workflow output

## Templates
- `templates/bdd-tdd-methodology.md` - BDD/TDD methodology overview
- `templates/bdd-template.md` - BDD feature template
- `templates/tdd-template.md` - TDD test template
- `templates/phase-prompts.md` - Revised phase prompts for BDD/TDD

## Current Build Mode Actions
1. Read `phase3-design.md` to understand the design
2. Implement the design (create entities, services, screens)
3. Run tests
4. Save implementation details to `phase4-implementation.md`
5. Switch to Plan Mode (Tab) for Phase 5 validation
