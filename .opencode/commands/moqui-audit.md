---
command: "moqui-audit"
description: "Executes a comprehensive code and documentation review by chaining the validator and doc-sync skills."
---

# 🕵️‍♂️ Comprehensive Moqui Audit

**Agent Directive:** You have been invoked via the `/moqui-audit` command. Your job is to orchestrate a full review of the active file by executing your specific capability skills in sequence.

## Step 1: Structural & Syntax Audit
If the active file is an XML screen, equip and execute the `moqui-screen-validator` skill. Allow it to check the XSD boundaries, verify encapsulation, and update the syntax cache if necessary.

## Step 2: Documentation Alignment
Equip and execute the `doc-sync` skill. Cross-reference the active file against the Business Requirements (BRD) and Technical Design Specs (TDS) in `./docs/brd/`.

## Step 3: Consolidated Report
Output a single, clean Markdown report summarizing the results of both skills:
1. **Validation Results:** (Syntax, strict-XML checks, and any newly learned tags).
2. **Documentation Status:** (BRD & TDS alignment, noting any discrepancies).