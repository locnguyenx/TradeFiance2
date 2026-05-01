import { test, expect } from '@playwright/test';

test.describe('Issuance Life-Cycle (True E2E)', () => {
  test('completes a full LC issuance from dashboard to submission', async ({ page }) => {
    // Enable console log forwarding
    page.on('console', msg => {
        console.log(`BROWSER: ${msg.text()}`);
    });

    // 1. Login & Navigation
    await page.goto('/issuance');
    
    // 2. Select Applicant (Triggering facility fetch)
    console.log('Selecting Applicant: Acme Corporation Ltd');
    await page.locator('select#applicant').selectOption({ label: 'Acme Corporation Ltd' });
    
    // 3. Inspect & Select LC Product
    console.log('Selecting LC Product...');
    await page.locator('select#productCatalogId').selectOption({ label: 'Standard Import LC' });

    // Wait for the specific log or just wait for the dropdown to have options
    console.log('Waiting for facility-select options...');
    const facilitySelect = page.getByTestId('facility-select');
    await expect(facilitySelect).toBeVisible();
    
    // Robust wait for options to populate
    await page.waitForFunction(() => {
        const select = document.querySelector('[data-testid="facility-select"]') as HTMLSelectElement;
        return select && select.options.length > 1;
    }, { timeout: 10000 });

    // 4. Select Facility
    await facilitySelect.selectOption({ index: 1 });

    // 5. Fill Step 1: Parties & Limits
    console.log('Waiting for Beneficiary options...');
    const beneficiarySelect = page.locator('select#beneficiary');
    await page.waitForFunction(() => {
        const select = document.querySelector('select#beneficiary') as HTMLSelectElement;
        return select && select.options.length > 1;
    }, { timeout: 10000 });
    
    console.log('Selecting Beneficiary...');
    await beneficiarySelect.selectOption({ label: 'Global Exports Inc' });
    
    console.log('Waiting for Advising Bank options...');
    const advisingBankSelect = page.locator('select#advisingBankPartyId');
    await page.waitForFunction(() => {
        const select = document.querySelector('select#advisingBankPartyId') as HTMLSelectElement;
        return select && select.options.length > 1;
    }, { timeout: 10000 });

    console.log('Selecting Advising Bank...');
    const bankOptions = await advisingBankSelect.evaluate((el) => {
        return Array.from((el as HTMLSelectElement).options).map(o => o.label);
    });
    const selectedBank = bankOptions.find(o => o.includes('Overseas Banking Corp'));
    if (!selectedBank) throw new Error('Overseas Banking Corp not found in advising bank options');
    await advisingBankSelect.selectOption({ label: selectedBank });
    
    // Verify selection before moving next
    const beneficiaryVal = await beneficiarySelect.inputValue();
    if (!beneficiaryVal) throw new Error('Beneficiary selection failed');

    // Move to Step 2
    console.log('Moving to Step 2...');
    await page.getByTestId('next-button').click();

    // 6. Step 2: Main LC Information
    console.log('Filling Main LC Information...');
    await expect(page.getByText(/Step 2: Main LC Information/i)).toBeVisible();
    
    console.log('Filling Amount...');
    await page.locator('input#amount').fill('125000');
    
    console.log('Selecting Currency...');
    await page.locator('select#currency').selectOption('USD');
    
    const today = new Date();
    const expiry = new Date();
    expiry.setFullYear(today.getFullYear() + 1);
    const shipmentDate = new Date();
    shipmentDate.setMonth(today.getMonth() + 6);

    console.log('Filling Expiry Date...');
    await page.locator('input#expiryDate').fill(expiry.toISOString().split('T')[0]);
    
    console.log('Filling Expiry Place...');
    await page.locator('input#expiryPlace').fill('SINGAPORE');

    console.log('Filling Shipment Date...');
    await page.locator('input#latestShipmentDate').fill(shipmentDate.toISOString().split('T')[0]);

    console.log('Filling Goods Description...');
    await page.locator('textarea#goodsDescription').fill('INDUSTRIAL RAW MATERIALS - GRADE A');
    
    console.log('Filling Documents Required...');
    await page.locator('textarea#documentsRequired').fill('1. FULL SET OF CLEAN ON BOARD BILL OF LADING\n2. COMMERCIAL INVOICE IN 3 COPIES');

    // Move to Step 3
    console.log('Moving to Step 3...');
    await page.getByTestId('next-button').click();
    
    // Diagnostic: Check for error banner and field errors
    const errorBanner = page.locator('.error-banner, .text-danger');
    if (await errorBanner.isVisible()) {
        const text = await errorBanner.innerText();
        console.error('ERROR BANNER DETECTED:', text);
        
        // Find fields with 'is-invalid' class
        const invalidFields = await page.evaluate(() => {
            return Array.from(document.querySelectorAll('.is-invalid')).map(el => {
                const label = document.querySelector(`label[for="${el.id}"]`);
                return { id: el.id, label: label?.textContent || 'N/A', value: (el as any).value };
            });
        });
        console.error('INVALID FIELDS:', JSON.stringify(invalidFields, null, 2));
    }

    // 7. Step 3: Margin & Charges
    console.log('Verifying Margin & Charges...');
    await page.waitForTimeout(2000); // Wait for API calls to complete
    await page.screenshot({ path: 'step3-after-api.png' });
    await expect(page.getByText(/Step 3: Margin & Charges/i)).toBeVisible({ timeout: 15000 });
    await page.getByTestId('next-button').click();

    // 8. Step 4: Review & Submit
    console.log('Reviewing LC...');
    await page.screenshot({ path: 'step4-before.png' });
    await expect(page.getByText(/Step 4: Review & Submit/i)).toBeVisible({ timeout: 15000 });
    await expect(page.getByText(/125,000/)).toBeVisible();

    // 9. Submit for Approval
    console.log('Submitting LC...');
    await page.getByTestId('submit-button').click();

    // 10. Verify Success Redirect or Message
    await expect(page.getByText(/Submitted Successfully/i)).toBeVisible({ timeout: 15000 });
    
    // Verify Dashboard context
    await page.goto('/dashboard');
    await expect(page.getByText('ACME_CORP_001')).toBeVisible();
  });
});
