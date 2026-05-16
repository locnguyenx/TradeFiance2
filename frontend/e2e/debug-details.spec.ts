import { test, expect } from '@playwright/test';

test('Debug Details Page', async ({ page }) => {
  // Login
  await page.goto('http://localhost:3000/login');
  await page.fill('input[name="username"]', 'trade.admin');
  await page.fill('input[name="password"]', 'trade123');
  await page.click('button[type="submit"]');
  
  // Wait for dashboard
  await page.waitForURL('**/import-lc');
  
  // Navigate directly to a known ID
  console.log('Navigating to details page...');
  await page.goto('http://localhost:3000/import-lc/details?id=100277');
  
  // Wait for content
  await page.waitForSelector('.audit-view', { timeout: 10000 });
  console.log('Details page loaded!');
  
  // Check Advised status
  const advised = await page.locator('.detail-row', { hasText: 'Advised' }).textContent();
  console.log('Advised Status:', advised);
});
