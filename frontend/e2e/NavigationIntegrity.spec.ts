import { test, expect } from '@playwright/test';

test.describe('Navigation Integrity (BDD-IMP-FLOW-*, BDD-CMN-WF-01)', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/import-lc');
  });

  test('BDD-IMP-FLOW-02: New LC Issuance navigation from sidebar', async ({ page }) => {
    await page.getByRole('link', { name: 'New LC Issuance' }).click();
    await expect(page).toHaveURL(/\/issuance/);
    await expect(page.getByText('Step 1: Parties & Limits')).toBeVisible();
  });

  test('BDD-CMN-AUTH-02: Global Checker Queue (My Tasks) navigation', async ({ page }) => {
    await page.getByRole('link', { name: 'My Tasks' }).click();
    await expect(page).toHaveURL(/\/approvals/);
    await expect(page.getByText('Global Checker Queue')).toBeVisible();
  });

  test('BDD-IMP-FLOW-03: LC Amendments navigation from sidebar', async ({ page }) => {
    await page.getByRole('link', { name: 'LC Amendments' }).click();
    await expect(page).toHaveURL(/.*amendments/);
    await expect(page.getByText('Issue LC Amendment')).toBeVisible();
  });

  test('BDD-IMP-SET-01: Settlements navigation from sidebar', async ({ page }) => {
    await page.getByRole('link', { name: 'Settlements' }).click();
    await expect(page).toHaveURL(/.*settlement/);
    await expect(page.getByText('Initiate LC Settlement')).toBeVisible();
  });

  test('BDD-IMP-SG-01: Shipping Guarantees navigation from sidebar', async ({ page }) => {
    await page.getByRole('link', { name: 'Shipping Guarantees' }).click();
    await expect(page).toHaveURL(/.*shipping-guarantees/);
    await expect(page.getByText('Issue Shipping Guarantee')).toBeVisible();
  });

  test('BDD-CMN-AUTH-01: Authority Tiers navigation from sidebar', async ({ page }) => {
    await page.getByRole('link', { name: 'Authority Tiers' }).click();
    await expect(page).toHaveURL(/.*tiers/);
    await expect(page.getByText('User Authority Management')).toBeVisible();
  });

  test('BDD-CMN-MAS-04: System Audit Logs navigation from sidebar', async ({ page }) => {
    await page.getByRole('link', { name: 'System Audit Logs' }).click();
    await expect(page).toHaveURL(/.*logs/);
    await expect(page.getByText('System Audit Logs (Delta JSON)')).toBeVisible();
  });

  test('BDD-CMN-MAS-03: Party Directory navigation from sidebar', async ({ page }) => {
    await page.getByRole('link', { name: 'Party Directory' }).click();
    await expect(page).toHaveURL(/\/parties/);
    await expect(page.getByRole('heading', { name: 'Party Directory', exact: true })).toBeVisible();
  });

  test('BDD-CMN-WF-01: Operations Dashboard (Return from Navigation)', async ({ page }) => {
    await page.getByRole('link', { name: 'LC Amendments' }).click();
    await page.getByRole('link', { name: 'Operations Dashboard' }).click();
    await expect(page).toHaveURL(/\/import-lc/);
    await expect(page.getByText('Active Transaction Data Table')).toBeVisible();
  });
});
