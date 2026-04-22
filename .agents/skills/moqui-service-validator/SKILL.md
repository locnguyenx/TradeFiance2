---
name: moqui-service-validator
description: Used in service development, diagnostics service issue.
---

## Execution Steps
1. **XSD Validation:** Validate XML tag against xsd file `service-definition-3.xsd`, `xml-actions-3.xsd`, `service-eca-3.xsd` in `/framework/xsd/` dir. If the tag is absent, flag it as an illegal hallucination.
2. **Knowledge Retrieval:** Read the knowledge at `.agents/knowledge/moqui-service-patterns.md` and `.agents/knowledge/moqui-common-patterns.md` to check for architectureal context, existing resolutions
3. **Self-Correction:** If you validated a new resolution, update file `moqui_patterns.md`
4. **Report:** Output a Markdown summary of violations fixed and/or new tags learned.