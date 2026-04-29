import { test, expect } from '@playwright/test';
import { loginAsAdmin } from './helpers/auth-helper';

test.describe('Dashboard Functions (True E2E)', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/import-lc');
  });

  test('should display real KPI metrics from live backend', async ({ page }) => {
    // Verify Dashboard context
    await page.waitForLoadState('networkidle');
    // If system shows unavailable, wait a bit and reload once
    if (await page.getByText('System Temporarily Unavailable').isVisible()) {
        await page.waitForTimeout(2000);
        await page.reload();
        await page.waitForLoadState('networkidle');
    }
    await expect(page.getByText('Active Instrument Data Table')).toBeVisible({ timeout: 20000 });

    // Assert on real KPI elements individually
    await expect(page.getByText('Drafts Awaiting My Submission')).toBeVisible();
    await expect(page.getByText('LCs Expiring within 7 Days')).toBeVisible();
    await expect(page.getByText('Discrepant Presentations Awaiting Waiver')).toBeVisible();
  });
});
