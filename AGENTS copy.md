# AGENTS.md - TradeFinance Agent Guidelines

## Architecture Principles

Follow these patterns:
- **Architecture**: Moqui Framework
- **Testing**: TDD London School - mock collaborators, test behaviors not implementations

### Documentation Compliance (Mandatory)
- **Source of Truth:** except explicit confirmation, the followings documents are the ultimate authority for this project
    - brd: `runtime/component/TradeFinance/docs/brd/`
    - tsd: `runtime/component/TradeFinance/docs/tsd/`
- **Pre-Flight Check:** Before performing any entity, screen, service creation or modification, you MUST:
    1. Read the relevant section of moqui rules.
    2. Check `.agents/knowledge/` for related patterns and lessons learned.
    3. Confirm that the proposed code change aligns with the architecture, related rules, patterns.

## 1. SCOPE LOCKDOWN (CRITICAL)

**Working Component**: `runtime/component/TradeFinance/`

**FORBIDDEN**: You are STRICTLY FORBIDDEN from modifying any files outside of `runtime/component/TradeFinance/`, by any file manipulation command/tool, regardless of mode

**Exception**: 
- Only `.opencode/` directory at workspace root is editable for rules/knowledge updates.
- Use gradle tasks for database operations

**Git Boundary**: All commits must target only the TradeFinance component directory.

---

## 2. CODE STYLE - XML Files

- **Filenames**: PascalCase ONLY (e.g., `CreateTrade.xml`, `VendorServices.xml`)
- **XML Prolog**: Required at top of every file: `<?xml version="1.0" encoding="UTF-8"?>`
- **XSD Validation**: Reference schemas in `framework/xsd/`
- **CDATA**: Use `<![CDATA[ ... ]]>` for Groovy scripts inside XML tags
- **No Raw Widgets in field-layout**: Widgets must be in `<field>` then referenced via `<field-ref>`
- **Indentation**: Use 2 spaces for XML indentation
- **Attribute Order**: Consistent attribute ordering: name, type, required, default-value, description, etc.
- **Self-Closing Tags**: Use self-closing tags for empty elements (e.g., `<field name="statusId" type="id"/>`)
- **Comments**: Use XML comments `<!-- comment -->` for documentation, not Groovy comments

---

## 3. CODE STYLE - Services

- **Authentication**: Assume `authenticate="true"` unless building public webhook
- **Error Handling**: Use `<script>ec.message.addError("...")</script>` - NOT `<message error="true">`
- **Naming**: `verb#noun` pattern (e.g., `create#Trade`, `transition#Status`)
- **Package**: Directory structure in `service/` must match service package name
- **Transactions**: Do NOT manually call `ec.transaction.begin()` unless writing custom batch jobs
- **Parameter Validation**: Use built-in validation attributes before custom Groovy logic
- **Return Values**: Always return a map with meaningful data, never null
- **Logging**: Use `ec.logger.debug()` for debugging, `ec.logger.info()` for operational info
- **Security**: Always check permissions with `<sec-require>` for sensitive operations
- **Service Comments**: Each service must have meaningful comments explaining purpose, usage, sample input/output, and edge cases

---

## 4. CODE STYLE - Entities

- **Primary Key**: Use exactly one `<field is-pk="true">` as single-column surrogate key
- **Data Types**: Use Moqui semantic types (`id`, `text-short`, `date`) - NEVER raw SQL types like `VARCHAR`
- **Audit Stamps**: Do NOT manually add `lastUpdatedStamp` or `createdTxStamp` - framework injects automatically
- **Caching**: Enable `cache="true"` ONLY for static config data (Enumerations, StatusItems)
- **View Entities**: Use `<view-entity>` for SQL-free joins, never write raw SQL in services
- **Field Naming**: Use clear, descriptive names in lowercase with underscores (e.g., `trade_id`, `status_date`)
- **Entity Comments**: Each Entity must have meaningful comments explaining purpose, related business requirements, and system processing
- **Indexing**: Add `<index>` elements for frequently queried non-primary key fields
- **Enum Fields**: Use `type="id"` with related enumeration for status/type fields

---

## 5. CODE STYLE - Screens

- **Paths**: Use RELATIVE paths (e.g., `../../Lc/MainLC`) - AVOID double-slash absolute paths
- **URL Type**: Use `url-type="screen"` for links
- **Back Navigation**: Use `lastScreenUrl` variable: `<link url="${lastScreenUrl ?: '.'}"/>`
- **Buttons**: Use `Create`, `Save` (NEVER "Update"), `Delete` (text-negative style)
- **Form Patterns**: `<form-list>` must use `header-dialog="true"`
- **Quasar Styling**: Use `style="class..."` on `<container>` (NOT `class` attribute)
- **Conditional Rendering**: Wrap widgets in `<section condition="...">` - do NOT use `condition` on `<link>` or `<container>`
- **Field Layout**: Inside `<field-layout>`, only `<field-col-row>`, `<field-col>`, and `<field-group>` are permitted as direct children
- **Widget References**: All widgets (links, containers) MUST be defined in a `<field>` and referenced via `<field-ref>`
- **Screen Comments**: Each screen must have comments explaining its purpose and usage
- **Validation**: Use built-in validation attributes and `<valid-when>` for field-level validation
- **Subscreens**: Use `<subscreens-panel>` with appropriate type (popup/tab) and always define a `default-item`

---

## 6. NAMING CONVENTIONS

| Type | Convention | Example |
|------|------------|---------|
| Component Name, XML Filenames | PascalCase | `TradeFinance` |
| appRoot URL | kebab-case | `trade-finance` |
| Service Names | verb#noun | `create#Trade` |
| Entity Names | PascalCase | `TradeFinance` |
| Field Names | snake_case | `trade_id`, `status_date` |
| Service Parameters | camelCase | `tradeId`, `statusId` |
| Variables (Groovy) | camelCase | `tradeId`, `statusDate` |
| Constants (Groovy) | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Test Classes | *Spec | `TradeFinanceServicesSpec` |
| Test Methods | descriptive sentences | `void "should create trade when valid data provided"()` |

---

## 7. DEFENSIVE PROGRAMMING

- **Null Safety**: Always use Elvis operator (`?:`) and safe-navigation (`?.`) in Groovy
- **No "null" in UI**: Use `desc = statusItem?.description ?: 'Not Set'`
- **Conditional Rendering**: Wrap widgets in `<section condition="...">` - do NOT use `condition` on `<link>` or `<container>`
- **Default Values**: Provide sensible defaults for optional parameters
- **Boundary Checks**: Validate collection sizes and indices before access
- **Exception Handling**: Use try/catch for external service calls, never let exceptions propagate
- **Resource Management**: Close resources in finally blocks or use Groovy's withCloseable
- **String Comparison**: Use `equals()` method, never `==` for string comparison
- **Number Parsing**: Always handle NumberFormatException when parsing strings to numbers

---

## 8. KEEP CONSISTENCCY

### Screen and Service Creation/Modification
Checking with Entity Definition, Service Definition when using them for name, fields/attributes

### Entity Data Loading Setup
Checking with Entity Definition to make sure that:
- the entity name, fieldname is correct
- mandatory fields must be loaded

---

## 9. TESTING RULES

- **Framework**: Spock (Groovy)
- **reloadSave Rule**: Every test run MUST be preceded by `reloadSave` to prevent data pollution
- **Type Coercion**: Assert with `result.success == true || result.success == "true"`
- **Resilient Assertions**: Avoid exact size checks, use thresholds (`>= 5`) instead of `size() == 5`
- **Sequence Collisions**: Use dynamic ID logic for auto-increment tables in suite runs
- **Test Naming**: Use descriptive test names that explain the behavior being tested
- **Given/When/Then**: Structure tests using Spock's given/when/then blocks
- **Mocking**: Mock collaborators, don't test implementations
- **Test Data**: Create minimal necessary test data in setup() method
- **Assertion Messages**: Include descriptive messages in assertions for easier debugging
- **Test Coverage**: Aim for testing behaviors, not just code coverage
- **Test Isolation**: Each test should be independent and not rely on state from other tests
- **Test Location**: Place tests in `src/test/groovy` following the same package structure as source

**REMINDER**: 
- MUST use build & test command in `moqui-test-runner` skill
- Always check `runtime/component/TradeFinance/build/test-results/test/` for test failure details.

---

## 10. EXTERNAL FILE LOADING

CRITICAL: When you encounter a file reference (e.g., @rules/general.md), use your Read tool to load it on a need-to-know basis. They're relevant to the SPECIFIC task at hand.

Instructions:

- Do NOT preemptively load all references - use lazy loading based on actual need
- When loaded, treat content as mandatory instructions that override defaults
- Follow references recursively when needed

---

## 11. REFERENCES

### Rules Files
- Session and memory management: @.agents/rules/session_protocol.md
- TradeFinance-specific rules: @.agents/rules/trade-finance.md
- Service rules: @.agents/rules/moqui-services.md
- Screen rules: @.agents/rules/moqui-screens.md
- Entity rules: @.agents/rules/moqui-entities.md
- Test rules: @.agents/rules/moqui-tests.md
- Troubleshooting guide: @.agents/rules/moqui-troubleshooting.md

### Knowledge Base (Reference Data)
- Testing patterns and traps: @.agents/knowledge/moqui-testing.md
- Entity architectural patterns: @.agents/knowledge/moqui-entity-patterns.md
- Service patterns: @.agents/knowledge/moqui-service-patterns.md
- UI architectural patterns: @.agents/knowledge/moqui-ui-patterns.md
- Testing & Integration patterns: @.agents/knowledge/moqui-other-patterns.md
- XML tag reference: @.agents/knowledge/moqui_syntax_ref.md
- Error diagnostics cache: @.agents/knowledge/moqui-errors.json

---

## 12. GIT WORKFLOW

### Branching Strategy
- **main**: Production-ready code only
- **develop**: Integration branch for features
- **feature/***: Feature branches (e.g., feature/lc-enhancements)
- **bugfix/***: Bug fix branches (e.g., bugfix/trade-validation)
- **release/***: Release preparation branches

### Commit Messages
- Use conventional commits format: `<type>(<scope>): <subject>`
- Types: feat, fix, docs, style, refactor, perf, test, chore
- Scope: Optional, indicates component area (e.g., trade, lc, provision)
- Subject: Imperative mood, max 50 characters
- Body: Optional, detailed explanation
- Footer: Optional, breaking changes or issue references

### Pull Request Process
1. Ensure code passes all tests locally
2. Update documentation if needed
3. Create PR with descriptive title and summary
4. Request review from at least one team member
5. Address all review comments
6. Ensure CI passes before merging
7. Delete branch after merge

### Pre-commit Checks
- Run `./gradlew test` for affected components
- Validate XML files with xmllint
- Check for TODO/FIXME comments that should be addressed
- Verify no debug logging statements remain in production code