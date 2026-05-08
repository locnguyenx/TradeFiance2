import { test, expect } from '@playwright/test';
import { loginAsAdmin } from './helpers/auth-helper';

test.describe('Authorizations Lifecycle (True E2E)', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/import-lc');
  });

  test('checker should see pending transactions in the queue', async ({ page }) => {
    // 1. Navigate to Authorizations Queue
    await page.getByRole('link', { name: 'My Tasks' }).click();
    await expect(page.getByRole('heading', { name: 'Authorization Queue' }).or(page.getByText('Global Checker Queue'))).toBeVisible({ timeout: 15000 });

    // 2. Verify that at least one pending record exists (e.g. LC240002 from master data)
    // In True E2E, we look for real instrument or reference patterns
    const table = page.getByRole('table');
    await expect(table).toBeVisible();
    
    // Check for any pending transaction row
    await expect(page.getByRole('button', { name: 'Authorize' }).first()).toBeVisible();
    
    // 3. Optional: Verify table structure
    await expect(page.getByRole('columnheader', { name: 'Instrument Ref' })).toBeVisible();
  });
});
