# 🚀 Moqui + Google Antigravity Template (Native Agent Architecture)

Welcome to the standardized Moqui development template. This repository is pre-configured with **Google Antigravity** agentic workflows. It ensures that both human developers and AI agents strictly adhere to our team's Moqui UI, routing, and backend standards.



## 🧠 The "Zero-Bloat, Zero-Code" AI Architecture

This project uses a highly optimized agentic architecture. Instead of overwhelming the AI with a massive rulebook or relying on fragile Python scripts, we use **Glob-targeted Rules** and **Instruction-Only Skills**. The agent relies entirely on its native semantic reasoning and built-in file system tools.

### Directory Structure

```text
.opencode/
├── rules/                 # Passive guardrails (Glob-targeted)
│   ├── session_protocol.md (Always On - Git & Session standards)
│   ├── moqui-screens.md   (Glob: runtime/component/TradeFinance/screen/**/*.xml)
│   ├── moqui-services.md  (Glob: runtime/component/TradeFinance/service/**/*.xml)
│   ├── moqui-entities.md  (Glob: runtime/component/TradeFinance/entity/**/*.xml)
│   └── trade-finance.md   # TradeFinance-specific rules
├── skills/                # Active Standard Operating Procedures (SOPs)
│   ├── moqui-diagnostics/ (Instructional error-matching & learning)
│   ├── moqui-scaffold/    (Native XML boilerplate generation)
│   ├── moqui-screen-validator/ (Semantic XML linting)
│   └── doc-sync/          (BRD/TDS alignment audit)
├── knowledge/             # Synchronized reference data
│   ├── moqui-errors.json  (Automated diagnostics cache)
│   ├── moqui-ui-patterns.md (Categorized UI architectural lessons)
│   ├── moqui-entity-patterns.md (Categorized Entity architectural lessons)
│   ├── moqui-service-patterns.md (Categorized Service architectural lessons)
│   ├── moqui-other-patterns.md (Testing & Integration architectural lessons)
│   ├── moqui-testing.md   (Mandates & traps for Spock/UI tests)
│   └── moqui_syntax_ref.md (Expanded XML tag reference)
```

### 1. Rules (Passive Guardrails)
- **Zero-Touch Enforcement:** `global.md` strictly forbids modifications outside `runtime/component/TradeFinance/`.
- **Session Standards:** `session_protocol.md` (consolidated) manages Git branching and session closing.
- **Contextual Globs:** Rules for screens, services, and entities are injected only when relevant.

### 2. Skills (Active SOPs)
Instructions telling the agent *how* to use its native abilities. No external scripts are required.
- **`moqui-diagnostics`**: Now uses the synchronized `moqui-errors.json` and categorized `moqui-*-patterns.md` for self-healing.
- **`doc-sync`**: Performs dual-layer audits (Business vs Technical).

---

## 🛠️ Quick Start Guide

**For Developers & AI Agents:**

1. Clone this repository and open it in **Google Antigravity**.
2. The IDE will automatically detect the `.opencode/` folder and register the Globs and Skills.
3. Open the Manager Surface (`Cmd + E` on Mac, `Ctrl + E` on Windows).
4. **To build a new UI:** Type *"Use the `moqui-scaffold` skill to generate an Equipment tracker module in `runtime/component/my-app/screen/MyApp`."*
5. **To validate your work:** Type *"Run the `moqui-screen-validator` skill on my active file."*
6. **If you encounter a build error:** Paste the stack trace into the chat and say, *"Use `moqui-diagnostics` to find the fix for this."*

## 🛡️ Key Standards Enforced Automatically

* **Strict XML:** No unclosed tags, proper `<![CDATA[ ... ]]>` usage, and no raw widgets directly inside `<field-layout>`.
* **Quasar Styling:** Forcing the use of `style="q-card..."` instead of `class="q-card..."` on containers.
* **Relative Routing:** Blocking absolute paths (`//...`) to prevent "out of application" routing errors.
* **Defensive Groovy:** Eliminating `NullPointerException` risks by enforcing the Elvis (`?:`) and safe-navigation (`?.`) operators.

---
*Note: Do not delete or modify the YAML frontmatter in the `.agents/rules/` files, as the Antigravity IDE relies on them for dynamic token management.*