---
paths:
  - "runtime/component/TradeFinance/service/**/*.xml"
---

# Moqui Backend Constraints

## 1. Service Structure
Always follow the standard Moqui directory layout for your service:
- `service/`: Service definitions (XML). **Crucial**: The directory structure inside `service/` must match the service package name.

## 2. Service Definitions (`.xml`)
* **Security:** Assume all services require authentication (`authenticate="true"`) unless building a public webhook.
* **Permissions:** Enforce authorization using `<sec-require>`.
* **Validation:** Strongly type `<in-parameters>` and use built-in validation attributes (`allow-html="safe"`) before writing Groovy logic.

### Service Naming
- **Services**: Services are referred to by `verb#noun` or `verb#package.noun`. Ensure the YAML/XML definition matches the intended verbal action.

### XML Syntax & Structure Rules
Strict adherence to XML standards is required for Moqui services to render correctly:

- **XML Prolog**: Every service file MUST start with `<?xml version="1.0" encoding="UTF-8"?>`.
- **XSD Validation**: All XML files must reference the correct XSD schema.
    - Example: `xsi:noNamespaceSchemaLocation="http://moqui.org/xsd/service-definition-3.xsd"`
    - Note: Local XSDs are stored in `./framework/xsd/` for IDE reference.

### Groovy Scripting Context
* **CDATA Blocks**: Use `<![CDATA[ ... ]]>` for any multi-line scripts or complex logic within XML tags to prevent character parsing errors.
* **Global Context:** Always use the global `ec` (ExecutionContext) object.
* **Database Operations:** Use the Entity Facade (`ec.entity.find()`).
* **Transactions:** Moqui automatically wraps synchronous service calls in transactions. Do not manually call `ec.transaction.begin()` unless writing a custom batch job. Rollbacks occur naturally via `ec.message.addError()`.
* **Scope:** Define local variables strictly (`def` or explicit types) to prevent memory leaks in the shared context map.

### 3. Validation and Error Handling

* **Validation:** Use `<script><![CDATA[ ... ]]></script>` blocks for complex validation logic, utilizing `ec.message.addError()` for field-level feedback.
* **Error Handling (CRITICAL):** The XML `<message error="true">` tag does NOT trigger a transaction failure. You MUST use `<script>ec.message.addError("...")</script>` to properly fail a service and roll back the transaction.

### 4. Service comments (MANDATORY)
* Each service must have meaningful comments to explain the purpose of this service, usage with sample input & output, edge case... 