---
name: session-close-protocol
description: Use when finishing a session. The Brain. Core protocols for writing, and maintaining the session's Memory
---

# Operational Protocols
This skill defines the core operational rules.

## 1. Date Verification (CRITICAL)
Before modifying ANY documentation file (Session Memory, Docs, Skills):
1.  
**Check System Date:**
 Run `date` or check system time tool.
2.  
**Update Metadata:**
 Always update `**Last Update:** [YYYY-MM-DD]` fields.
3.  
**NEVER ASSUME DATES.**


## 2. Session Closing Protocol
When the user says "finish session" or similar:
1.  
**Update `current-state.md`**
: Write everything we did so far to current-state.md, ensure to note the end goal, the approach we're taking, the steps we've done so far, and the current failure we're working on
2.  
**Update `progress.md`**
: Log completed milestones.
3.  
**Update `NOTES_NEXT_SESSION.md`**
: Write clear instructions for the "next you".

4.  
**Cleanup**
: Remove temp files or logs.
5. Do **Self-Improvement**

### Location
`.agents/session-memory/` (at Root of the project)

### Core Files
1.  
**`session-memory/core/current-state.md`**
2.
**`session-memory/core/progress.md`**
3.  
**`session-memory/NOTES_NEXT_SESSION.md`**

# 🚀 Self-Improvement Directive (The "Gardener")

You are responsible for maintaining and evolving your own Memory and Skills.
When you discover a new pattern, solution, finding, lesson learned or rule:
1.  
**Update the Journal (Knowledge Base):**
 If the finding is a pattern or framework rule, update or create the relevant `.journal/patterns-*.md` or `.journal/moqui-*.md` file. The `.journal/` directory is your Single Source of Truth for all domain knowledge.
2.  
**Identify the relevant Skill:**
 If the finding is a procedural workflow, update the corresponding `SKILL.md` in `.agents/skills/`.
3.  
**Refactor:**
 If a Journal file or Skill becomes too large, propose splitting it.
4.  
**Create:**
 Only create a NEW Skill folder if the knowledge is truly procedural and domain-distinct (e.g., Workflow Processing, Temporal, Testing, Banking).

**DO NOT create loose files for rules. Curate your `.agents/skills` folder.**