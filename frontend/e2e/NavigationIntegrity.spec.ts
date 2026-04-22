import { test, expect } from '@playwright/test';

test.describe('Navigation Integrity', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/import-lc');
  });

  test('should navigate to Issuance page from sidebar', async ({ page }) => {
    await page.getByRole('link', { name: 'New LC Issuance' }).click();
    await expect(page).toHaveURL(/\/issuance/);
    await expect(page.getByText('Step 1: Parties & Limits')).toBeVisible();
  });

  test('should navigate to My Tasks page from sidebar', async ({ page }) => {
    await page.getByRole('link', { name: 'My Tasks' }).click();
    await expect(page).toHaveURL(/\/approvals/);
    await expect(page.getByText('Global Checker Queue')).toBeVisible();
  });

  test('should navigate to Party Directory from sidebar', async ({ page }) => {
    await page.getByRole('link', { name: 'Party Directory' }).click();
    await expect(page).toHaveURL(/\/parties/);
    await expect(page.getByText('Party Directory & KYC Management')).toBeVisible();
  });

  test('should return to Dashboard from sidebar', async ({ page }) => {
    await page.getByRole('link', { name: 'New LC Issuance' }).click();
    await page.getByRole('link', { name: 'Operations Dashboard' }).click();
    await expect(page).toHaveURL(/\/import-lc/);
    await expect(page.getByText('Active Transaction Data Table')).toBeVisible();
  });
});
