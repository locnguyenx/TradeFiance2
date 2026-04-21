# Skill: doc-sync
**Description**: Audits the active codebase against both the Business Requirements Documents (BRDs) and Technical Design Specs (TDS) to ensure total alignment. Use this skill whenever you complete a feature or modify core logic.

## Execution Steps
Do not execute a script. Use your native file-reading tools to perform this strict dual-layer audit:

### 1. Identify the Target
Identify the currently active `.xml` screen/entity or `.groovy` service file you just modified.

### 2. Locate the Specs
Use your file-system tools to read the documentation directory:
- **Business Layer:** Find the corresponding BRD in `./docs/brd/` (defines the workflows, UI expectations, and business rules).
- **Technical Layer:** Find the corresponding TDS in `./docs/brd/tds/` (defines the entity models, service signatures, parameter maps, and precise Moqui component architecture).

### 3. The Dual-Layer Comparison
Cross-reference the active code against the loaded documents:
- **BRD Audit:** Does the screen flow, user permissions, and visual layout (Forms/Lists) fulfill the business requirements?
- **TDS Audit:** Do the `<in-parameters>`, `<entity-find>` queries, Groovy variable names, and database fields exactly match the technical design spec? 

### 4. Report and Resolve
Output a Markdown report with two distinct sections: **Business Alignment** and **Technical Alignment**. 
- List any exact discrepancies (e.g., "The code uses `statusId`, but the TDS specifies `lifecycleStatusId`").
- If the code deviates from the documentation, explicitly ask the user for the resolution: *"Should I rewrite the code to match the specs, or should I update the BRD/TDS to reflect our new code?"*