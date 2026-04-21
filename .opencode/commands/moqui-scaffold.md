---
command: "moqui-scaffold"
description: "Generates boilerplate Moqui XML screens (Parent, Find, and Detail) using native file-writing tools."
---

# 🏗️ Moqui Screen Scaffolding

> **Agent Directive:** You have been invoked to scaffold a new Moqui UI module. Do not use external scripts. Use your native file-writing tools to create the following three files in the requested directory. Replace `[Name]` with the user's requested entity name (PascalCase) and `[name]` with the camelCase version.

## Step 1: Create `[Name].xml` (The Parent Screen)
Write a standard Moqui parent screen. 
- Include transitions for `find[Name]` and `[Name]Detail`.
- Include a `<subscreens default-item="Find[Name]">` block mapping to the two child screens.
- Implement the safe subscreen detection block: wrap the tab panel in a `<section>` that checks `condition="[name]Id && !['Find[Name]', '[Name]'].contains(sri.screenUrlInfo.targetScreen?.getScreenName())"`.

## Step 2: Create `Find[Name].xml` (The List Screen)
Write the list screen.
- Include an `<entity-find>` in the `<actions>` block.
- Create a `<form-list>` with `header-dialog="true"`.
- Include a `<container-dialog>` for creating a new record.
- In the list row actions, map the ID to a passing variable (e.g., `<script>[name]IdToPass = [name]Id</script>`) to keep parent routing clean.

## Step 3: Create `[Name]Detail.xml` (The Detail Screen)
Write the detail editing screen.
- Require the `[name]Id` parameter.
- Fetch the record in `<actions>` using `<entity-find-one>`.
- Build a `<form-single>` to update the record.
- Include a back button referencing `lastScreenUrl`.

**Completion:** Once written, verify that the `default-item` in the parent exactly matches the list screen's filename. Report success to the user.