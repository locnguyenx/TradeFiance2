import { test, expect } from '@playwright/test';
import { loginAsAdmin } from './helpers/auth-helper';

test.describe('Admin Configuration (True E2E)', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/');
  });

  test('should allow navigating and viewing product configuration', async ({ page }) => {
    await page.goto('/admin/product');
    await expect(page.getByRole('heading', { name: 'Products' })).toBeVisible({ timeout: 15000 });
    // Verify real product label from MasterData.xml
    await expect(page.getByText('Standard Import LC').first()).toBeVisible();
  });

  test('should allow navigating and viewing tariff configuration', async ({ page }) => {
    await page.goto('/tariffs');
    await expect(page.getByRole('heading', { name: 'Tariff Matrix' })).toBeVisible({ timeout: 15000 });
  });

  test('should allow navigating and viewing party directory with KYC', async ({ page }) => {
    await page.goto('/parties');
    await expect(page.getByRole('heading', { name: 'Counterparties' })).toBeVisible();
    // Verify real party from MasterData.xml
    await expect(page.getByText('Acme Corporation Ltd').first()).toBeVisible({ timeout: 10000 });
  });
});
