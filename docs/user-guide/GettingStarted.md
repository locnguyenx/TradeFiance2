# Getting Started Guide: Digital Trade Finance

This guide provides the necessary steps to set up, run, and access the Digital Trade Finance platform for development and local evaluation.

---

## 1. Prerequisites
Ensure the following are installed on your system:
- **OpenJDK 17+**: Required for Moqui Framework.
- **Node.js 18+ & npm**: Required for the React SPA.
- **Git**: For repository management.

---

## 2. Backend Setup (Moqui)
The backend is a headless service providing the `/rest/s1/trade` API.

### Environment Preparation
1. **Load Seed Data**: Provision standard roles, users, and trade templates.
   ```bash
   ./gradlew load
   ```
2. **Start the Server**:
   ```bash
   ./gradlew run
   ```
   - The backend will be available at `http://localhost:8080`.
   - Access Moqui Admin Tools at `http://localhost:8080/vroot`.

---

## 3. Frontend Setup (React SPA)
The frontend is a modern React application located in the `/frontend` directory.

### Installation & Execution
1. **Install Dependencies**:
   ```bash
   cd frontend
   npm install
   ```
2. **Start the Development Server**:
   ```bash
   npm run dev
   ```
   - The application will be available at `http://localhost:3000`.

---

## 4. Navigation & Layout
The platform utilizes a premium, unified navigation shell:
- **Workspace**: Access your module dashboards and the Global Checker Queue.
- **Trade Modules**: Context-sensitive menus for Import and Export workflows.
- **Master Data**: Administrative access to Parties, Facilities, and Tariffs.

---

## 5. Initial Access & Login
Use the following credentials to explore the different personas:

| persona | Username | Password |
|---------|----------|----------|
| **Maker** | `trade.maker` | `trade123` |
| **Checker** | `trade.checker` | `trade123` |
| **Backoffice** | `trade.backoffice` | `trade123` |
| **System Admin**| `trade.admin` | `trade123` |

---

## 5. Verification
The platform includes a comprehensive test suite covering backend logic, frontend components, and full E2E journeys.

### Run All Tests
1. **Backend (Spock)**: `./gradlew test` (Moqui runtime)
2. **Frontend (Jest)**: `cd frontend && npm test` (Component level)
3. **E2E (Playwright)**: `cd frontend && npx playwright test` (Full-stack)

For detailed technical patterns and test strategies, refer to the [Developer Guide](file:///Users/me/myprojects/moqui-trade/docs/user-guide/DeveloperGuide.md).
