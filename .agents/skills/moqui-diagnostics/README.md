# Troubleshooting guides

This is a perfect evolution of your agentic architecture. Troubleshooting guides are incredibly valuable, but they are the ultimate token-wasters if handled incorrectly.

Think about it: why should the agent load 2,000 words on how to fix a `SAXParseException` or a "Field not found" error if the code it just wrote is perfectly fine and compiling successfully?

To get the exact same self-healing behavior without the token bloat, you have two distinct paths in Antigravity. Which one you choose depends on how big your list of troubleshooting instructions currently is.

---

## Path 1: The "Model Decision" Rule (For ~10 to 20 Errors)

If your troubleshooting list is relatively short (under 1,000 words), you don't need to build a search engine. You just need to tell Antigravity to **hide the rules until an error occurs**.

We use the `model_decision` activation mode. The IDE will only show the agent a tiny 1-sentence description. If the agent encounters a bug, it will actively choose to "read" the rest of the file to find the solution.

**Save as:** `.agents/rules/moqui-troubleshooting.md`

```markdown
---
mode: model_decision
description: "Contains solutions for Moqui framework errors, XML parsing crashes, and Quasar rendering issues. Pull this into context ONLY if your code fails or the user reports a bug."
---

# Moqui Troubleshooting & Known Errors

## XML Syntax Errors
- **Symptom:** "Content is not allowed in prolog"
  - **Fix:** Remove any whitespaces or hidden characters before the `<?xml ... ?>` tag at the very top of the file.
- **Symptom:** "Doing nothing for element X"
  - **Fix:** You used an XML tag in a context where the Moqui renderer doesn't support it (e.g., `<row>` inside a widget that only supports `container`). Switch to `<container style="row">`.

## Field & Form Errors
- **Symptom:** "could not find field with name X referred to in a field-ref"
  - **Fix:** The field is implicitly defined via `auto-parameters` but used in a `<field-layout>`. Explicitly define the field (e.g., `<field name="lcId"><default-field><hidden/></default-field></field>`) *before* the layout block.
- **Symptom:** Empty Form Lists
  - **Fix:** Verify that `<field-ref>` is used inside `<form-list-column>`. Ensure the `list` attribute in `<form-list>` exactly matches the list name produced in `<actions>`.

## UI & State Issues
- **Symptom:** Persistent Tabs remaining on the screen after changing to a popup.
  - **Fix:** This is a Quasar browser cache issue. Instruct the user to perform a hard refresh (Cmd+Shift+R).

```

**Why this works:** The agent spends exactly **30 tokens** carrying around the description. When you say, *"Hey, I just got a 'could not find field' error,"* the agent's logic triggers: *I need the troubleshooting rule.* It pulls it in, finds the exact fix, and corrects the XML.

---

## Path 2: The "Diagnostic Search" Skill (For 20+ Errors)

If your agent has been learning for weeks and your troubleshooting list is massive (e.g., 50+ different errors, stack traces, and deep Groovy bugs), putting it in a Rule file will eventually cause Token Bloat when the agent tries to load it.

Instead, you convert the knowledge into a **JSON Dictionary** and create a tiny Python skill to search it.

### Step 1: The Knowledge Base (Not read by the agent directly)

Create a file to hold your massive list of errors.
**Save as:** `.agents/knowledge/moqui-errors.json`

```json
{
  "prolog": "Remove whitespace before <?xml?>.",
  "could not find field": "Explicitly define the field before the <field-layout> block.",
  "formInstance was null": "You used a raw widget like <container> inside <field-layout>. Wrap it in a <field> or <field-col>."
}

```

### Step 2: The Diagnostic Skill

**Save as:** `.opencode/skills/moqui-diagnostics/SKILL.md`

```markdown
# Skill: moqui-diagnostics
**Description**: Searches the internal Moqui error database. Use this WHENEVER you encounter a stack trace, rendering error, or XSD violation.
## Usage
- **Command**: `python search_errors.py --query "<error_snippet>"`

```

**Save as:** `.opencode/skills/moqui-diagnostics/search_errors.py`

```python
import argparse
import json
import os

def search_errors(query):
    # Load the JSON dictionary
    file_path = os.path.join(os.path.dirname(__file__), '../../knowledge/moqui-errors.json')
    try:
        with open(file_path, 'r') as f:
            errors = json.load(f)
    except Exception:
        return {"status": "error", "message": "Knowledge base not found."}

    # Simple text matching
    results = []
    for key, solution in errors.items():
        if key.lower() in query.lower() or query.lower() in key.lower():
            results.append({"trigger": key, "solution": solution})
    
    if results:
        return {"status": "success", "solutions": results}
    return {"status": "miss", "message": "No known solution found. Debug using standard Moqui docs."}

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--query", required=True)
    args = parser.parse_args()
    print(json.dumps(search_errors(args.query)))

```

**Why this is the ultimate setup:** 1. The agent gets an error: *"Template Error: formInstance was null"*.
2. The agent runs: `python search_errors.py --query "formInstance was null"`.
3. The script returns exactly **1 sentence** of help.
4. The agent fixes the bug instantly without loading 5,000 words of unrelated errors into its context window.

---

## The Verdict

* If your lessons learned are mostly **"Best Practices and Gotchas,"** use **Path 1** (Model Decision Rule).
* If your lessons learned are mostly **"Specific Stack Traces and Error Strings,"** use **Path 2** (Diagnostic Search Skill).

**Would you like me to take your raw list of troubleshooting instructions right now and format them into the JSON Dictionary for Path 2, so you can immediately start using the Diagnostic Skill?**