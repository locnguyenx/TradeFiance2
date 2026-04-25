import { test, expect } from '@playwright/test';

// ABOUTME: Centralized API mocking for Playwright E2E tests.
// ABOUTME: Ensures deterministic behavior for Dual Checker and Admin flows.

export async function setupApiMocks(page: any) {
  page.on('console', (msg: any) => console.log(`BROWSER [${msg.type()}]: ${msg.text()}`));
  page.on('pageerror', (err: any) => console.log(`BROWSER ERROR: ${err.message}`));

  // Catch-all for other /rest/s1/trade requests to prevent leaks to real backend
  // Note: Playwright handles routes in REVERSE order of registration.
  // We register this FIRST so that specific routes registered later take priority.
  await page.route(/.*\/rest\/s1\/trade\/.*/, async (route: any) => {
    console.log(`UNMATCHED REST REQUEST (Fallback): ${route.request().url()}`);
    await route.fulfill({
      status: 404,
      contentType: 'application/json',
      body: JSON.stringify({ error: 'Endpoint not mocked in E2E' })
    });
  });

  await page.route(/.*\/rest\/s1\/trade\/exposure-data/, async (route: any) => {
    console.log('Mocking exposure-data');
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        totalLimit: 10000000,
        totalExposure: 6500000,
        utilizationPercent: 65,
        facilityBreakdown: [
          { facilityId: 'FAC_001', facilityName: 'Import LC Facility', limit: 8000000, exposure: 5000000 }
        ]
      })
    });
  });

  await page.route(/.*\/rest\/s1\/trade\/parties/, async (route: any) => {
    console.log('Mocking parties');
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        partyList: [
          {
            partyId: 'CORP_BETA',
            partyName: 'Beta Corp Ltd',
            roleTypeId: 'APPLICANT',
            kycStatusEnumId: 'KYC_PASSED',
            sanctionsStatusEnumId: 'SANCTIONS_CLEAN'
          }
        ]
      })
    });
  });

  await page.route(/.*\/rest\/s1\/trade\/product-catalog/, async (route: any) => {
    console.log('Mocking product-catalog');
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        productList: [
          { productCatalogId: 'IMPORT_LC_STD', productName: 'Import Letter of Credit', isActive: 'Y' }
        ]
      })
    });
  });

  await page.route(/.*\/rest\/s1\/trade\/fee-configurations/, async (route: any) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        feeList: [
          { feeConfigId: 'FEE_001', feeTypeEnumId: 'ISSUANCE_FEE', isActive: 'Y', calculationMethodEnumId: 'PERCENT' }
        ]
      })
    });
  });
  
  await page.route(/.*\/rest\/s1\/trade\/import-lc($|\?|\/)/, async (route: any) => {
    const url = route.request().url();
    const method = route.request().method();

    if (method === 'POST') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ instrumentId: 'LC_NEW_999', status: 'SUCCESS' })
      });
      return;
    }

    if (url.match(/\/import-lc\/[^\/\?]+($|\?)/)) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ instrumentId: 'LC_100', transactionRef: 'REF100', applicantName: 'Beta Corp Ltd', amount: 50000, businessStateId: 'LC_ISSUED' })
      });
    } else {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          lcList: [
            { instrumentId: 'LC_100', transactionRef: 'REF100', applicantName: 'Beta Corp Ltd', amount: 50000, businessStateId: 'LC_ISSUED' }
          ],
          lcListCount: 1
        })
      });
    }
  });

  await page.route(/.*\/rest\/s1\/trade\/kpis/, async (route: any) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ pendingDrafts: 5, expiringSoon: 2, discrepantDocs: 1 })
    });
  });

  await page.route(/.*\/rest\/s1\/trade\/approvals/, async (route: any) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        approvalsList: [
          { instrumentId: 'AP-1', transactionRef: 'TX-REF-1', module: 'Import LC', action: 'Issuance', priorityEnumId: 'NORMAL', timeInQueue: '2h', lifecycleStatusId: 'INST_PENDING_APPROVAL' }
        ]
      })
    });
  });

  await page.route(/.*\/rest\/s1\/trade\/audit-logs/, async (route: any) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        auditLogList: [
          { auditLogId: 'LOG_1', timestamp: new Date().toISOString(), changedByUserId: 'SYSTEM', actionName: 'STARTUP', deltaJson: '{}' }
        ]
      })
    });
  });

  await page.route(/.*\/rest\/s1\/trade\/authority-profiles/, async (route: any) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        profileList: [
          { authorityProfileId: 'TIER_1', description: 'Tier 1 Analyst', maxAmount: 100000, isActive: 'Y' }
        ]
      })
    });
  });
}
