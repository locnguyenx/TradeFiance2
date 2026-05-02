import { test, expect } from '@playwright/test';
import { loginAsAdmin } from './helpers/auth-helper';

test.describe('Party & KYC Management', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/parties');
  });

  test('FR-TP-10/11: Create and Edit Party Flow', async ({ page }) => {
    // 1. Verify Page Header
    await expect(page.getByRole('heading', { name: 'Counterparties' })).toBeVisible();

    // 2. Open New Party Modal
    await page.getByRole('button', { name: '+ New Party' }).click();
    await expect(page.getByRole('heading', { name: 'Register New Counterparty' })).toBeVisible();

    // 3. Fill and Submit New Party (Commercial)
    const partyId = `TEST_CORP_${Date.now()}`;
    await page.locator('input[name="partyId"]').fill(partyId);
    await page.locator('input[name="partyName"]').fill('Test Corporation Ltd');
    await page.locator('select[name="partyTypeEnumId"]').selectOption('PARTY_COMMERCIAL');
    await page.locator('textarea[name="registeredAddress"]').fill('123 Test Street, Singapore');
    await page.locator('input[name="accountNumber"]').fill('SG123456789');
    
    await page.getByRole('button', { name: 'Register Counterparty' }).click();

    // 4. Verify Creation in List
    await expect(page.getByText(partyId)).toBeVisible();
    await page.getByText(partyId).click();
    await expect(page.getByRole('heading', { name: 'Test Corporation Ltd' })).toBeVisible();

    // 5. Edit Party Profile
    await page.getByRole('button', { name: 'Edit Profile' }).click();
    await expect(page.getByRole('heading', { name: 'Edit Counterparty' })).toBeVisible();
    
    await page.locator('input[name="partyName"]').fill('Test Corporation Ltd Updated');
    await page.locator('select[name="kycStatus"]').selectOption('Pending');
    
    await page.getByRole('button', { name: 'Update Profile' }).click();

    // 6. Verify Update
    await expect(page.getByRole('heading', { name: 'Test Corporation Ltd Updated' })).toBeVisible();
    await expect(page.getByText('PENDING')).toBeVisible();
  });

  test('FR-TP-10: Create Bank Party with SWIFT BIC', async ({ page }) => {
    await page.getByRole('button', { name: '+ New Party' }).click();
    
    const bankId = `TEST_BANK_${Date.now()}`;
    const swiftBic = 'TESTSG22XXX';
    
    await page.locator('input[name="partyId"]').fill(bankId);
    await page.locator('input[name="partyName"]').fill('Test Global Bank');
    await page.locator('select[name="partyTypeEnumId"]').selectOption('PARTY_BANK');
    
    // Verify bank-specific fields appear
    await expect(page.getByText('Bank Specific Details')).toBeVisible();
    await page.locator('input[name="swiftBic"]').fill(swiftBic);
    await page.locator('input[name="hasActiveRMA"]').check();
    
    await page.getByRole('button', { name: 'Register Counterparty' }).click();
    
    // Verify in detail view
    await page.getByText(bankId).click();
    await expect(page.getByRole('heading', { name: 'Test Global Bank' })).toBeVisible();
  });
});
