---
paths:
  - "runtime/component/TradeFinance/screen/**/*.xml"
---

# Moqui Screen Constraints & Standards

## 1. UI & Button Standards
- **Record Actions:** Use `Create`, `Save` (NEVER use "Save Changes", "Update", or "Apply"), and `Delete` (styled `text-negative`).
- **Placement:** Primary "Save" and workflow action buttons MUST be placed in the top header via `<field-ref>`.
- **Idiomatic Disabling:** Do not manually style buttons as disabled. Add a `condition` to the transition (e.g., `<transition name="..." condition="!isReadOnly">`).
- **Form Patterns:** - `<form-list>` MUST use `header-dialog="true"` and `<field-ref>` inside `<form-list-column>`.
  - Pop-up dialogs MUST use `<container-dialog header-dialog="true">`.

## 2. Routing & Navigation
- **Pathing (CRITICAL):** Use RELATIVE paths (e.g., `../../Lc/MainLC`). AVOID double-slash absolute paths (`//ImportLc/...`) for internal module routing.
- **URL Type:** Always use `url-type="screen"` for linking to other screens.
- **Context Preservation:** Target screens must always use the `lastScreenUrl` variable for their close/back link: `<link url="${lastScreenUrl ?: '.'}" .../>`. The Back button style must be `q-btn-flat text-grey-7`.
- **Subscreen Panels:** Use `<subscreens-panel type="popup"/>` for main module navigation, and `<subscreens-panel type="tab"/>` for internal record views. Always define a `default-item`.

## 3. XML Syntax & Layout Strictness
- **XML Prolog**: Every screen file MUST start with `<?xml version="1.0" encoding="UTF-8"?>`.

- **Encapsulation (CRITICAL):** Inside `<field-layout>`, you CANNOT use widgets like `<container>`, `<display-entity>`, `<label>`, or `<link>` directly. They MUST be defined inside a `<field>` tag first, then referenced using `<field-ref name="..."/>`.

- **Quasar Styling:** `<container>` does not support Quasar `class` attributes. All Quasar classes (e.g., `q-card shadow-2`) MUST be wrapped in the `style` attribute.

- **Conditional Rendering:** Do not use `condition="..."` on `<link>` or `<container>`. Wrap the widget inside a `<section name="..." condition="...">`.

- **Defensive Groovy:** Do not let "null" appear in the UI. Use Groovy's Elvis operator (`?:`) and safe-navigation (`?.`) in `<actions>` blocks (e.g., `desc = statusItem?.description ?: 'Not Set'`).

## 4. Layout Constraints (Nesting)
- **Field Layout:** Inside `<field-layout>`, only `<field-col-row>`, `<field-col>`, and `<field-group>` are permitted as direct children.
- **Widget References:** All widgets (links, containers) MUST be defined in a `<field>` and referenced via `<field-ref>`.

## 5. Complex Implementations (The Cookbook)
- For complex UI architecture—including **Safe Subscreen Detection** (preventing tab leakage), **Idiomatic Status Chips**, and **Parent Context Clearing**—you MUST consult the approved code snippets in `.agents/knowledge/moqui-ui-patterns.md`.
- For questions about valid Moqui tags, consult `.agents/knowledge/moqui_syntax_ref.md`.

## 6. XML Validation (MANDATORY)
**ALWAYS validate XML screens BEFORE testing.**

- Use `xmllint --noout <file-path>` to validate screen XML files
- Common errors that break screens:
  - Duplicate field definitions
  - Unclosed or extra closing tags
  - Invalid tag nesting
- See `.agents/knowledge/moqui-testing.md` section 12 for more details.