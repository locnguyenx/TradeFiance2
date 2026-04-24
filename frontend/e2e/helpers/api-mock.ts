import { test, expect } from '@playwright/test';

// ABOUTME: Centralized API mocking for Playwright E2E tests.
// ABOUTME: Ensures deterministic behavior for Dual Checker and Admin flows.

export async function setupApiMocks(page: any) {
  page.on('console', (msg: any) => console.log(`BROWSER [${msg.type()}]: ${msg.text()}`));
  page.on('pageerror', (err: any) => console.log(`BROWSER ERROR: ${err.message}`));

  await page.route('**/api/v1/exposure-data', async (route: any) => {
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

  await page.route('**/api/v1/parties*', async (route: any) => {
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

  await page.route('**/api/v1/product-catalogs', async (route: any) => {
    console.log('Mocking product-catalogs');
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        catalogList: [
          { productCatalogId: 'IMPORT_LC_STD', productName: 'Import Letter of Credit', isActive: 'Y' }
        ]
      })
    });
  });

  await page.route('**/api/v1/fee-configs', async (route: any) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        configList: [
          { feeConfigId: 'FEE_001', feeTypeEnumId: 'ISSUANCE_FEE', isActive: 'Y', calculationMethodEnumId: 'PERCENT' }
        ]
      })
    });
  });
  
  await page.route('**/api/v1/import-lcs', async (route: any) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        lcList: [
          { instrumentId: 'LC_100', transactionRef: 'REF100', applicantName: 'Beta Corp Ltd', amount: 50000, businessStateId: 'LC_ISSUED' }
        ]
      })
    });
  });

  await page.route('**/api/v1/kpis', async (route: any) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ pendingDrafts: 5, expiringSoon: 2, discrepantDocs: 1 })
    });
  });
}
