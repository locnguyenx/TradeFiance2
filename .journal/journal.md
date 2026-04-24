# Tech Journal - Trade Finance Integration Hardening

## 2026-04-22: API Integration Breakthrough

### Moqui View-Entity Resolution
- **Insight**: Moqui's `view-entity` engine can be brittle when resolving joined fields (e.g., `productEnumId` from `TradeInstrument`) within complex hierarchies, especially across packages (`trade` vs `trade.importlc`).
- **Solution**: Shifted to **Manual Groovy Joins** in `ImportLcServices.xml`. By performing sequential lookups and merging maps (`TI.getMap() + ILC.getMap()`), we eliminated all "field not found" and registry resolution errors. 
- **Rule of Thumb**: For high-stakes API services, prefer the predictability of Groovy scripts over the magic of XML-defined view-entities.

### REST Path Parameter Routing
- **Insight**: The standard `{parameter}` syntax for path parameters in `rest.xml` failed with 404 in this environment.
- **Solution**: Switched to the `<id name="parameterName">` tag. This is the more robust pattern for path parameter extraction in Moqui and resolved the routing issues immediately.

### Data Mapping Alignment
- **Insight**: The Frontend integration tests used `amount` while the backend used `baseEquivalentAmount`. `auto-parameters` silently ignored the mismatch.
- **Solution**: Explicitly mapped `amount` to `baseEquivalentAmount` in the `create` and `update` service actions. 

### XML Structural Fragility
- **Issue**: Accidental removal of `<actions>` or unclosed `<in-parameters>` tags led to services that loaded but didn't execute logic.
### Frontend Data Resilience
- **Issue**: `Runtime TypeError: can't access property "toLocaleString", lc.baseEquivalentAmount is undefined`.
- **Insight**: Even with typed interfaces, runtime data from the backend can be incomplete (null/undefined) due to legacy records or partial service execution.
- **Solution**: Implemented null-safe fallback `(lc.baseEquivalentAmount ?? 0).toLocaleString()` in `ImportLcDashboard.tsx`.
- **Testing**: Added a regression test in `ImportLcDashboard.test.tsx` using `mockResolvedValueOnce` with `null` fields. This ensures the UI remains stable even when the data layer is imperfect.
