# Frontend & React Patterns

## 1. Data Resilience & Null Safety
* **Handling Incomplete Data**: Runtime data from the backend can be incomplete (null/undefined) due to legacy records or partial service execution, even with TypeScript typing.
* **Fallback Strategy**: Implement null-safe fallbacks in the UI (e.g., `(lc.baseEquivalentAmount ?? 0).toLocaleString()`) to prevent rendering crashes.
* **Resilience Testing**: Add regression tests (e.g., in `ImportLcDashboard.test.tsx`) using `mockResolvedValueOnce` with `null` fields to ensure UI stability when the data layer is imperfect.

## 2. Validation & Error Propagation
* **Surfacing Backend Business Logic**: Do not ignore backend validation. For example, if the backend strictly enforces a 10% drawing tolerance, handle the `400 Bad Request` or service error gracefully by surfacing the tolerance violation message to the user in the UI.
* **Global Error Handling**: Ensure backend validation errors (like real-time KYC status checks during lead-time data entry) are captured and displayed via a global error banner (e.g., in `IssuanceStepper.tsx`), preventing silent failures.

## 3. UI Consistency & Unit Testing
* **Exact String Matching**: High-density navigation shells and branding components must exactly match their test specs (e.g., `TRADEFINANCE` vs `TRADE FINANCE`) to maintain CI/CD stability.
* **React Key Props**: Pay attention to React key prop warnings in the console (e.g., in Product and Tariff managers) to ensure proper list rendering and DOM diffing.
