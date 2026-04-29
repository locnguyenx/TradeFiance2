import { test, expect } from '@playwright/test';
import { loginAsAdmin } from './helpers/auth-helper';

test.describe('Navigation Integrity (True E2E)', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/import-lc');
  });

  test('BDD-IMP-FLOW-02: New LC Issuance navigation from sidebar', async ({ page }) => {
    await page.getByRole('link', { name: 'New LC Issuance' }).click();
    await expect(page).toHaveURL(/\/issuance/);
    await expect(page.getByRole('heading', { name: 'Step 1: Parties & Limits' })).toBeVisible();
  });

  test('BDD-CMN-AUTH-02: Global Checker Queue (My Tasks) navigation', async ({ page }) => {
    await page.getByRole('link', { name: 'My Tasks' }).click();
    await expect(page).toHaveURL(/\/approvals/);
    await expect(page.getByRole('heading', { name: 'Authorization Queue' }).or(page.getByText('Global Checker Queue'))).toBeVisible();
  });

  test('BDD-IMP-FLOW-03: LC Amendments navigation from sidebar', async ({ page }) => {
    await page.getByRole('link', { name: 'LC Amendments' }).click();
    await expect(page).toHaveURL(/.*amendments/);
    await expect(page.getByText(/Issue LC Amendment|LC Amendment Request/)).toBeVisible();
  });

  test('BDD-IMP-SET-01: Settlements navigation from sidebar', async ({ page }) => {
    await page.getByRole('link', { name: 'Settlements' }).click();
    await expect(page).toHaveURL(/.*settlement/);
    await expect(page.getByRole('heading', { name: 'Initiate LC Settlement' })).toBeVisible();
  });

  test('BDD-IMP-SG-01: Shipping Guarantees navigation from sidebar', async ({ page }) => {
    await page.getByRole('link', { name: 'Shipping Guarantees' }).click();
    await expect(page).toHaveURL(/.*shipping-guarantees/);
    await expect(page.getByRole('heading', { name: 'Issue Shipping Guarantee' })).toBeVisible();
  });

  test('BDD-CMN-AUTH-01: User Authority Tiers navigation', async ({ page }) => {
    await page.getByRole('link', { name: 'User Authority Tiers' }).click();
    await expect(page).toHaveURL(/\/admin\/tiers/);
    await expect(page.getByRole('heading', { name: 'User Authority Management' })).toBeVisible();
  });

  test('BDD-CMN-MAS-04: Audit Logs navigation from sidebar', async ({ page }) => {
    await page.getByRole('link', { name: 'Audit Logs' }).click();
    await expect(page).toHaveURL(/.*logs/);
    await expect(page.getByRole('heading', { name: 'Global Transaction Log' })).toBeVisible();
  });

  test('BDD-CMN-MAS-03: Party Directory navigation from sidebar', async ({ page }) => {
    await page.getByRole('link', { name: 'Party & KYC Directory' }).click();
    await expect(page).toHaveURL(/\/parties/);
    await expect(page.getByRole('heading', { name: 'Counterparties' })).toBeVisible();
  });

  test('BDD-CMN-EXP-01: Credit Facilities navigation from sidebar', async ({ page }) => {
    await page.getByRole('link', { name: 'Credit Facilities (Limits)' }).first().click();
    await expect(page).toHaveURL(/\/facilities/);
    await expect(page.getByRole('heading', { name: 'Credit Facility Dashboard' })).toBeVisible({ timeout: 15000 });
  });

  test('BDD-CMN-TRF-01: Tariff & Fee Mapping navigation from sidebar', async ({ page }) => {
    await page.getByRole('link', { name: 'Tariff & Fee Configuration' }).click();
    await expect(page).toHaveURL(/\/tariffs/);
    await expect(page.getByRole('heading', { name: 'Tariff Matrix' })).toBeVisible();
  });

  test('BDD-CMN-PRD-01: Product Config navigation from sidebar', async ({ page }) => {
    await page.getByRole('link', { name: 'Product Configuration' }).click();
    await expect(page).toHaveURL(/.*product/);
    await expect(page.getByRole('heading', { name: 'Products' })).toBeVisible();
  });

  test('BDD-CMN-WF-01: Operations Dashboard (Return from Navigation)', async ({ page }) => {
    await page.getByRole('link', { name: 'LC Amendments' }).click();
    await page.getByRole('link', { name: 'Import LC Dashboard' }).click();
    await expect(page).toHaveURL(/\/import-lc/);
    await expect(page.getByRole('heading', { name: 'Active Instrument Data Table' })).toBeVisible();
  });

  test('REQ-NAV-01.3: Transaction Dashboard visibility and content', async ({ page }) => {
    await page.getByRole('link', { name: 'Transaction Dashboard' }).click();
    await expect(page).toHaveURL(/\/transactions/);
    await expect(page.getByRole('heading', { name: 'Transaction Dashboard' })).toBeVisible();
    // Verify that at least the header or a table row appears without 404 error
    await expect(page.getByText('Unified operational view')).toBeVisible();
  });
});
