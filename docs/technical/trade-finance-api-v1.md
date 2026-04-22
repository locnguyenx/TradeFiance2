# Trade Finance REST API v1 - Technical Guide

**Base URL**: `/rest/s1/trade`

## 1. KPIs & Metrics
- **Endpoint**: `GET /kpis`
- **Output**:
```json
{
  "pendingDrafts": 5,
  "expiringSoon": 2,
  "discrepantDocs": 1
}
```

## 2. Import Letter of Credit
- **List Documents**: `GET /import-lc`
- **Create Document**: `POST /create-lc`
  - **Payload**:
    ```json
    {
      "transactionRef": "TF-IMP-100",
      "baseEquivalentAmount": 50000.0,
      "expiryDate": "2026-12-31"
    }
    ```

## 3. Authorization Actions
- **Authorize Instrument**: `POST /authorize`
  - **Payload**: `{ "instrumentId": "10001" }`
  - **Logic**: Enforces 4-Eyes principle (Maker/Checker matrix).

## 4. Error Responses
| Code | Meaning |
|------|---------|
| 200  | Success |
| 400  | Validation/Limit Failure |
| 403  | Self-Authorization Forbidden |
| 500  | Internal Server Error |
