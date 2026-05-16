import { test, expect } from '@playwright/test';
import { loginAsAdmin } from './helpers/auth-helper';

test.describe('Trade Inbox & SWIFT Correlation (True E2E)', () => {
  const testPrefix = `E2E${Date.now()}`;
  const lcRef = `${testPrefix}-LC`;

  test.beforeEach(async ({ page }) => {
    page.on('console', msg => {
      if (msg.type() === 'log' || msg.type() === 'error' || msg.type() === 'warn') {
        console.log(`BROWSER: ${msg.text()}`);
      }
    });
    await loginAsAdmin(page);
  });

  test('MT 730 Acknowledge Flow', async ({ page, request }) => {
    const credentials = Buffer.from('trade.admin:trade123').toString('base64');
    const authHeader = { 'Authorization': `Basic ${credentials}` };

    // 1. Create a baseline LC via REST API for correlation
    console.log(`Creating LC with ref: ${lcRef}`);
    const lcResponse = await request.post('http://localhost:8080/rest/s1/trade/import-lc', {
      headers: authHeader,
      data: {
        instrumentRef: lcRef,
        lcAmount: 50000,
        lcCurrencyUomId: 'USD',
        productCatalogId: 'STD_IMP_LC',
        instrumentParties: [
          { roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001' },
          { roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002' },
          { roleEnumId: 'TP_ADVISING_BANK', partyId: 'ADVISING_BANK_001' }
        ],
        businessStateId: 'LC_ISSUED' // Seed as already issued to test 730 delivery
      }
    });
    if (!lcResponse.ok()) {
      console.error(`LC Creation failed: ${lcResponse.status()} ${await lcResponse.text()}`);
    }
    expect(lcResponse.ok()).toBeTruthy();
    const lcData = await lcResponse.json();
    const instrumentId = lcData.instrumentId;

    // 2. Ingest MT 730 SWIFT Message matching the LC
    const mt730 = `{1:F01VIETBANK1XXXX0000000000}{2:I730OVERSEAS2XXXXN}{4:
:20:${testPrefix}-730
:21:${lcRef}
:25:001-234567-USD
:30:260516
:32D:260516USD50000,
:71B:0,
:72:/BNF/LC ADVISED OK
-}`;

    console.log(`Ingesting MT 730 for LC ${lcRef}`);
    const ingestResponse = await request.post('http://localhost:8080/rest/s1/trade/inbound-swift/ingest', {
      headers: authHeader,
      data: {
        rawContent: mt730,
        sourceChannel: 'E2E_TEST'
      }
    });
    expect(ingestResponse.ok()).toBeTruthy();

    // Give time for async correlation
    console.log('Waiting for async correlation...');
    await page.waitForTimeout(5000);

    // 3. Navigate to Trade Inbox
    await page.goto('/import-lc/inbox');
    await page.waitForLoadState('networkidle');

    // 4. Verify MT 730 appears in the list
    console.log(`Waiting for row with text: ${testPrefix}-730`);
    const inboxRow = page.locator('tr', { hasText: `${testPrefix}-730` });
    
    // Debug: print all row text if not found
    try {
      await expect(inboxRow).toBeVisible({ timeout: 10000 });
    } catch (e) {
      const rows = await page.locator('tr').allTextContents();
      console.log('All rows found:', rows);
      throw e;
    }

    await expect(inboxRow.locator('.type-badge')).toHaveText('730');
    
    // 5. Verify correlation with LC
    await expect(inboxRow.locator('.correlation-info')).toContainText(lcRef);

    // 6. Verify Auto-Processed Status (since MT 730 is auto-triggered)
    await expect(inboxRow.locator('.status-pill')).toHaveText('PROCESSED');
    await expect(inboxRow.locator('.text-green-500')).toBeVisible(); 

    // 8. Verify LC state is updated to Advised (with polling/reload)
    console.log(`Verifying LC ${instrumentId} is advised`);
    await expect(async () => {
      await page.goto(`/import-lc/details?id=${instrumentId}`);
      const advisedRow = page.locator('.detail-row').filter({ has: page.locator('.row-label', { hasText: /^Advised$/ }) });
      await expect(advisedRow).toContainText('Yes', { timeout: 2000 });
    }).toPass({ timeout: 30000, intervals: [5000] });
  });
});
