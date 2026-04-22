import { test, expect } from '@playwright/test';

test.describe('Issuance Lifecycle E2E', () => {
  test('completes a full LC issuance from dashboard to submission', async ({ page }) => {
    // 1. Navigate to Dashboard
    await page.goto('/import-lc'); // Hardcore dashboard path
    await expect(page.getByText('Active Transaction Data Table')).toBeVisible();

    // 2. Start New Issuance (via Sidebar)
    await page.getByRole('link', { name: 'New LC Issuance' }).click();
    await expect(page.getByText('Step 1: Parties & Limits')).toBeVisible();

    // 3. Fill Step 1: Parties
    const uniqueRef = `E2E-REF-${Date.now()}`;
    await page.locator('#transactionRef').fill(uniqueRef);
    await page.locator('#applicant').fill('Test Applicant');
    await page.getByTestId('next-button').click();

    // 4. Fill Step 2: Financials
    await expect(page.getByText('Step 2: Financials & Dates')).toBeVisible();
    await page.locator('#amount').fill('75000');
    await page.getByTestId('next-button').click();
    
    // 5. Navigate Steps 3-4
    await expect(page.getByText('Step 3: Terms & Shipping')).toBeVisible();
    await page.getByTestId('next-button').click();

    await expect(page.getByText('Step 4: Narratives')).toBeVisible();
    await page.getByTestId('next-button').click();

    // 6. Review & Submit (Step 5)
    await expect(page.getByText('Step 5: Review & Submit')).toBeVisible();
    await expect(page.getByText(`Reference: ${uniqueRef}`)).toBeVisible();
    await expect(page.getByText('Amount: 75,000')).toBeVisible();

    // 6. Submit for Approval
    await page.getByTestId('submit-button').click();

    // 7. Verify Success Message
    await expect(page.getByText('Successfully Submitted for Approval')).toBeVisible();
  });
});
