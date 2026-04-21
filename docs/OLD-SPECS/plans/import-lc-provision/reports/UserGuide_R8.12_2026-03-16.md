# User Guide: R8.12 LC Provision Collection

**Version:** 1.0.0
**Date:** 2026-03-16
**Feature:** Import LC Multi-Account Provision Collection

---

## Overview

The LC Provision Collection feature allows banks to collect provision (collateral) from multiple customer accounts in different currencies for Import Letters of Credit. This feature streamlines the provision collection process by supporting multi-currency collections with real-time exchange rate conversion.

---

## Accessing the Feature

1. Navigate to **Trade Finance** → **Import LC**
2. Select or create an LC
3. Click on the **Financials** tab
4. Scroll to the **Provision Collection** section

---

## User Interface

### Provision Collection Section

The Provision Collection section in the Financials tab displays:

- **Target Provision**: The total provision amount required for the LC
- **Collected**: The current total collected amount
- **Status**: Current status of the collection (Draft, Complete, Collected, Released, Failed)

### Collection Status Indicators

| Status | Color | Description |
|--------|-------|-------------|
| Draft | Grey | Collection created, entries being added |
| Complete | Yellow | Total matches target, ready to collect |
| Collected | Green | Funds successfully held in CBS |
| Released | Blue | Funds released back to accounts |
| Failed | Red | Collection failed |

---

## Workflow

### Step 1: Create Provision Collection

1. Go to the Financials tab of an LC
2. Click **"Collect Provision"** button
3. The system creates a new collection with status "Draft"
4. Target provision amount is populated from LC settings

### Step 2: Add Collection Entries

1. Click **"Add Entry"** button in the Collection Entries section
2. Select an account from the dropdown:
   - `ACC_EUR_001` - EUR account
   - `ACC_GBP_001` - GBP account
   - `ACC_USD_001` - USD account
3. Enter the amount in the selected currency
4. Click **"Add"**

The system automatically:
- Fetches the exchange rate from CBS
- Converts the amount to USD
- Updates the running total

### Step 3: Validate Collection

Click **"Validate"** button to check if the collected amount matches the target:

- **Complete**: Total matches target (±0.01 USD tolerance)
- **Exceeds**: Total is more than target
- **Incomplete**: Total is less than target

### Step 4: Collect Funds

Once validation shows "Complete":

1. Click **"Collect Funds"** button
2. The system executes CBS hold requests for all accounts
3. If any hold fails, all holds are rolled back
4. Collection status changes to "Collected"

### Step 5: Release Funds (Optional)

When the LC is closed, expired, or revoked:

1. Click **"Release Funds"** button
2. The system releases all CBS holds
3. Collection status changes to "Released"

---

## Supported Currencies

| Currency | Code | Exchange Rate (USD) |
|----------|------|-------------------|
| US Dollar | USD | 1.00 |
| Euro | EUR | 1.09 |
| British Pound | GBP | 1.27 |

*Note: Exchange rates are fetched from CBS in real-time.*

---

## Business Rules

1. **Multi-Account Support**: Provisions can be collected from multiple accounts in different currencies
2. **Tolerance**: Collections within ±0.01 USD of target are accepted as complete
3. **Atomic Collection**: All CBS holds must succeed, or all are rolled back (no partial collections)
4. **Account Eligibility**: Only accounts owned by the LC applicant can be used

---

## Troubleshooting

### Issue: "An active collection already exists"

**Solution**: Use the existing collection or wait for it to be completed/released before creating a new one.

### Issue: Collection status stays "Incomplete"

**Solution**: Add more entries to reach the target provision amount, or adjust existing entry amounts.

### Issue: "Collect Funds" button is disabled

**Solution**: Click "Validate" first to verify the total matches the target. The button is only enabled when status is "Complete".

### Issue: CBS hold failures

**Solution**: 
- Ensure accounts have sufficient balances
- Check CBS connectivity
- Try again or use different accounts

---

## Related Screens

- **LC Financials**: `ImportLc/Lc/Financials`
- **LC Detail**: `ImportLc/Lc/MainLC`
- **Find LC**: `ImportLc/Lc/FindLc`

---

## Appendix: Service Operations

| Operation | Service | Description |
|-----------|---------|-------------|
| Create Collection | `create#LcProvisionCollection` | Initialize a new provision collection |
| Add Entry | `add#CollectionEntry` | Add an account entry with currency conversion |
| Validate | `validate#CollectionTotal` | Validate total against target |
| Collect Funds | `collect#ProvisionFunds` | Execute CBS holds |
| Release Funds | `release#ProvisionCollection` | Release CBS holds |

---

**Document Version:** 1.0
**Last Updated:** 2026-03-16
