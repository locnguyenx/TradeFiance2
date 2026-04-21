# TradeFinance Workflow Directory

This directory contains workflow outputs for TradeFinance development.

## Current Mode: BUILD
**Status**: You are in Build Mode with full file access.

## Files
- `phase1-analysis.md` - Business analysis and planning
- `phase2-discovery.md` - Code discovery findings
- `phase3-design.md` - Technical design specifications
- `phase4-implementation.md` - Implementation details
- `phase5-validation.md` - Validation results

## Templates Directory
- `templates/bdd-tdd-methodology.md` - BDD/TDD methodology overview
- `templates/bdd-template.md` - BDD feature template
- `templates/tdd-template.md` - TDD test template
- `templates/phase-prompts.md` - Revised phase prompts for BDD/TDD

## Workflow Procedure
1. **Phase 1-3**: Start in Plan Mode (Tab) → Complete analysis/discovery/design → Save outputs
2. **Phase 4**: Switch to Build Mode (Tab) → Implement based on design → Auto-save
3. **Phase 5**: Switch back to Plan Mode (Tab) → Validate → Save outputs

## Current Mode Instructions (Build Mode)
- **You can**: Write files, edit files, run bash commands
- **You should**: Implement Phase 4 (Implementation) based on design
- **Next step**: Read `phase3-design.md` and implement the design

## Quick Commands
- `/init-workflow` - Initialize workflow directory
- `/save-workflow [phase] [content]` - Save workflow output
- `/read-workflow [phase]` - Read workflow output

## Mode Switching
- **Plan Mode**: Tab key (read-only, analysis only)
- **Build Mode**: Tab key (write-enabled, implementation)
