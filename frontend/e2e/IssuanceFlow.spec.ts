import { test, expect } from '@playwright/test';
import { setupApiMocks } from './helpers/api-mock';

test.describe('Issuance Lifecycle E2E', () => {
  test.beforeEach(async ({ page }) => {
    await setupApiMocks(page);
  });

  test('completes a full LC issuance from dashboard to submission', async ({ page }) => {
    // 1. Navigate to Dashboard
    await page.goto('/import-lc'); // Hardcore dashboard path
    await expect(page.getByRole('heading', { name: 'Active Transaction Data Table' })).toBeVisible();

    // 2. Start New Issuance (via Sidebar)
    await page.getByRole('link', { name: 'New LC Issuance' }).click();
    await expect(page.getByRole('heading', { name: 'Step 1: Parties & Limits' })).toBeVisible();

    // 3. Fill Step 1: Parties
    const uniqueRef = `E2E-REF-${Date.now()}`;
    await page.locator('#transactionRef').fill(uniqueRef);
    await page.locator('#applicant').fill('Test Applicant');
    await page.getByTestId('next-button').click();

    // 4. Fill Step 2: Main LC Info (Financials, Terms, Narratives)
    await expect(page.getByRole('heading', { name: 'Step 2: Main LC Information' })).toBeVisible();
    await page.locator('#amount').fill('75000');
    await page.locator('#portOfLoading').fill('London');
    await page.locator('#goodsDescription').fill('Precision components');
    await page.getByTestId('next-button').click();
    
    // 5. Navigate Step 3: Margin & Charges
    await expect(page.getByRole('heading', { name: 'Step 3: Margin & Charges' })).toBeVisible();
    await page.getByTestId('next-button').click();

    // 6. Review & Submit (Step 4)
    await expect(page.getByRole('heading', { name: 'Step 4: Review & Submit' })).toBeVisible();
    await expect(page.getByText(`Reference: ${uniqueRef}`).first()).toBeVisible();
    await expect(page.getByText('Amount: 75,000').first()).toBeVisible();

    // 6. Submit for Approval
    await page.getByTestId('submit-button').click();

    // 7. Verify Success Message
    await expect(page.getByText('Successfully Submitted for Approval')).toBeVisible();
  });
});
