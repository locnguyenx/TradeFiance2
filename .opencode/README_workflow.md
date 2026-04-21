This is the perfect way to tie everything together. By creating a **Workflow Macro**, you give your developers a one-click "Code Reviewer" that instantly applies all your hard work without them having to memorize the rules or manually run the Python scripts.

In Antigravity, Workflows live in the `.opencode/commands/` directory. They act as pre-packaged prompt sequences that guide the agent step-by-step through a complex process.

Here is the complete setup for your `/moqui-audit` macro.

### 1. Create the Workflow File

Create a new file in your template repository. The YAML frontmatter registers the slash command with the Antigravity IDE.

**Save as:** `.opencode/commands/moqui-audit.md`

```markdown
---
command: "moqui-audit"
description: "Audits the currently active Moqui file against strict workspace rules and runs automated validation tools."
---

# 🕵️‍♂️ Moqui Code Audit Workflow

> **Agent Directive:** You have been invoked to perform a strict code review on the user's currently active file. Execute the following steps sequentially and do not skip the active validation phase.

## Step 1: Identify Context & Domain
Determine the file path of the currently active file in the editor. Based on its location, silently load the following rule sets into your working memory:
- **If `**/screen/**/*.xml`:** Reference `moqui-screen-forms.md`, `moqui-screen-routing.md`, and `moqui-screen-syntax.md`.
- **If `**/service/**/*.xml` or `*.groovy`:** Reference `moqui-services.md`.
- **If `**/entity/**/*.xml`:** Reference `moqui-entities.md`.

## Step 2: Run Active Validation (Screens Only)
If the active file is a Moqui Screen (`**/screen/**/*.xml`), you MUST immediately execute the `moqui-screen-validator` skill against the active file's path to check for XSD and layout violations. 
*(If it is a backend or entity file, skip this step).*

## Step 3: Deep Rule Analysis
Read the active file line-by-line and cross-check it against the loaded rules. Look specifically for:
- Incorrect Quasar styling (using `class` instead of `style` on containers).
- Improper button captions (e.g., "Update" instead of "Save").
- Missing safe-navigation operators (`?.`) in Groovy scripts or XML actions.
- Illegal tags inside `<field-layout>`.
- Absolute URL paths where relative paths should be used.

## Step 4: Generate the Audit Report
Output your findings to the user using the exact Markdown structure below. Be brutally honest but constructive.

### 📋 Audit Report: `[Filename]`

**1. Automated Validator Results:**
* [Print the output of the `moqui-screen-validator` skill here, or "N/A - Backend File"]

**2. Rule Violations:**
* [List any direct violations of the loaded Moqui rules. Include the line number and the specific rule broken. If none, print "✅ All standards met."]

**3. Suggested Fixes:**
* [Provide the exact code snippets needed to fix the identified violations. Format as Git diffs or clear before/after blocks.]

```

---

### How this works in practice:

1. A developer on your team finishes writing `ImportLcDetail.xml`.
2. They open the Antigravity Manager Surface (`Cmd + E`).
3. They simply type `/moqui-audit` and hit enter.
4. **The Magic:** * The IDE automatically knows which file is open.
* The agent reads the workflow instructions.
* It runs your Python `moqui-screen-validator` skill in the background.
* It checks the button naming conventions against your `moqui-screen-forms.md`.
* It generates a beautiful, actionable report detailing exactly what the developer needs to fix before pushing their code.



### Why this is a masterclass in Agentic Development:

You have successfully separated concerns.

* Your **Rules** (Globs) act as passive guardrails while typing.
* Your **Skills** (Python scripts) act as atomic tools.
* Your **Workflows** (Slash commands) act as the manager that orchestrates the rules and skills together on demand.

**Would you like me to write a `README.md` for this template repository that explains this exact architecture (Globs, Skills, and Workflows) to your human team members so they know exactly how to use this setup?**