import { test, expect } from '@playwright/test';
import { setupApiMocks } from './helpers/api-mock';

test.describe('Admin Configuration (Phase FE-3)', () => {
  test.beforeEach(async ({ page }) => {
    await setupApiMocks(page);
  });
  test('should allow navigating and viewing product matrix', async ({ page }) => {
    await page.goto('/admin/product');
    await expect(page.getByText('Product Configuration Matrix')).toBeVisible();
    await expect(page.getByText('Import Letter of Credit')).toBeVisible();
  });

  test('should allow navigating and viewing tariff configuration', async ({ page }) => {
    await page.goto('/tariffs');
    await expect(page.getByText('Tariff & Fee Configuration')).toBeVisible();
    await expect(page.getByText('ISSUANCE_FEE')).toBeVisible();
  });

  test('should allow navigating and viewing party directory with KYC', async ({ page }) => {
    await page.goto('/parties');
    await expect(page.getByText('Counterparties')).toBeVisible();
    // Verify detail pane state
    await expect(page.getByText(/select a party to view compliance details/i)).toBeVisible();
  });

  test('should show exposure alerts in facilities dashboard', async ({ page }) => {
    await page.goto('/facilities');
    await expect(page.getByText('Exposure & Credit Facilities')).toBeVisible();
    await expect(page.getByText('Total Outstanding Exposure')).toBeVisible();
  });
});
