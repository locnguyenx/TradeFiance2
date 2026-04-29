# Testing & E2E Patterns

## 1. Playwright E2E "Stuck" State (Networking & Proxies)
* **DNS Dual-Stack Ambiguity**: In Node 17+ on macOS, `localhost` resolves to both `::1` (IPv6) and `127.0.0.1` (IPv4). Next.js might bind to IPv4 while Playwright's `webServer` verifies the other, causing a silent polling hang (`ECONNREFUSED` / timeout).
  * **Fix**: Explicitly use `127.0.0.1:3000` for both `baseURL` and `webServer.url`.
* **Zombie Turbopack Proxies**: `next dev --turbo` spawns child processes that can survive as orphans when Playwright terminates the webServer, holding port 3000.
  * **Fix**: Use `npm run build && npm start` for E2E verification. The single production process is stable and SIGTERM-compliant.
* **Path-Based Readiness Gates**: Polling the root `/` can succeed (200 OK) before the Next.js hydration engine or data fetching is ready.
  * **Fix**: Set the `webServer.url` to a functional business route (e.g. `/import-lc`) to ensure the server is 100% interactive.

## 2. Playwright Strict Mode
* **Master-Detail UIs**: Playwright enforces strict mode. Using text locators in a master-detail UI often fails when multiple elements match the same text.
* **Resolution**: Use highly specific locators (Role/Heading) or `.first()` to avoid strict mode violations.

## 3. API Mocking Strategy (Jest)
* **Mock Shape Alignment**: Frontend components (like dashboards) expect deep, complete objects for metrics and KPIs. Providing partial or shallow mocks in `api-mock.ts` leads to runtime crashes in components like `FacilityDashboard`.
* **Mocking Specificity**: Shallow mocks are insufficient. Add `jest.mock('../api/tradeApi')` with specific mocked values for each test case (loading, error, data) to ensure coverage for edge cases like system unavailability.
