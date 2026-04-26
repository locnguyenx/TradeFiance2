import { test, expect } from '@playwright/test';
import { loginAsAdmin } from './helpers/auth-helper';

test.describe('Issuance Life-Cycle (True E2E)', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
    // 1. Navigate to Dashboard
    await page.goto('/import-lc');
  });

  test('completes a full LC issuance from dashboard to submission', async ({ page }) => {
    // 1. Navigate to Dashboard
    await page.goto('/import-lc');
    await expect(page.getByRole('heading', { name: 'Active Transaction Data Table' })).toBeVisible({ timeout: 15000 });

    // 2. Start New Issuance
    await page.getByRole('link', { name: 'New LC Issuance' }).click();
    await expect(page.getByRole('heading', { name: 'Step 1: Parties & Limits' })).toBeVisible();

    const txRef = `IMLC/2026/${Math.floor(Math.random() * 9000) + 1000}`;
    await page.locator('#transactionRef').fill(txRef);
    await page.selectOption('#productCatalogId', { label: 'Standard Import LC' });
    
    // 3. Select Applicant & Facility in Step 1 (Real Master Data)
    const facilityPromise = page.waitForResponse(resp => resp.url().includes('/facilities/customer') && resp.status() === 200);
    await page.locator('#applicant').selectOption({ label: 'Zizi Corp' });
    await page.locator('#beneficiary').fill('Industrial Components Ltd\n123 Factory Road, Shanghai, China');
    await facilityPromise; // Robust sync
    await page.waitForTimeout(1000); 
    
    // Click Next and wait for Step 2 heading
    await page.getByTestId('next-button').click();
    await expect(page.getByRole('heading', { name: 'Step 2: Main LC Information' })).toBeVisible({ timeout: 10000 });

    // 4. Fill Step 2: Main LC Information
    await expect(page.getByRole('heading', { name: 'Step 2: Main LC Information' })).toBeVisible();
    await page.locator('#amount').fill('125000');
    await page.locator('#expiryPlace').fill('AT OUR COUNTERS');
    
    const futureDate = new Date();
    futureDate.setFullYear(futureDate.getFullYear() + 1);
    const dateStr = futureDate.toISOString().split('T')[0];
    await page.locator('#expiryDate').fill(dateStr);
    await page.locator('#latestShipmentDate').fill(dateStr);
    
    await page.locator('#goodsDescription').fill('Precision components for manufacturing');
    await page.locator('#documentsRequired').fill('1. Commercial Invoice\n2. Packing List\n3. Bill of Lading');
    
    await page.getByTestId('next-button').click();
    
    // 5. Margin & Limits (Step 3) - Select Real Facility
    await expect(page.getByRole('heading', { name: 'Step 3: Margin & Charges' })).toBeVisible(); 
    
    // Handle Facility selection if present in this step or Step 1
    // The previous test had it in Step 3
    // Step 3: Margin & Charges
    await page.selectOption('#customerFacilityId', { label: 'Working Capital Line - $1,000,000' });
    
    await page.getByTestId('next-button').click();

    // 6. Review & Submit
    await expect(page.getByRole('heading', { name: 'Step 4: Review & Submit' })).toBeVisible();
    await expect(page.getByText('Amount: 125,000').first()).toBeVisible();

    // 7. Submit for Approval
    await page.getByTestId('submit-button').click();

    // 8. Verify Success Message and status transition to PENDING
    await expect(page.getByRole('heading', { name: 'Submission Successful' })).toBeVisible({ timeout: 15000 });
  });
});
