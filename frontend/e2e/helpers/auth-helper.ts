import { Page } from '@playwright/test';

/**
 * Perform a Basic Auth login to establish a Moqui session.
 * This ensures that the frontend can call /rest/s1/trade services without mocking.
 */
export async function loginAsAdmin(page: Page) {
  // Use the admin credentials from TradeFinanceUsers.xml
  const credentials = Buffer.from('trade.admin:trade123').toString('base64');
  
  // 1. Inject credentials directly into the tradeApi object in the browser context
  // This ensures that tradeApi._fetch will use the Authorization header immediately.
  await page.addInitScript((creds) => {
    // We wait for the window to load or just define a global variable that the app can use
    // Or we can try to find the tradeApi if it's exposed, but it's likely bundled.
    // However, setExtraHTTPHeaders should work if the app uses native fetch.
    (window as any).__E2E_CREDENTIALS__ = creds;
  }, credentials);

  // 2. Set the Authorization header for all subsequent requests in this context
  await page.setExtraHTTPHeaders({
    'Authorization': `Basic ${credentials}`
  });

  // 3. Perform a hit to the PROXY URL (3001) to trigger session cookie creation on the frontend domain
  await page.request.get('http://localhost:3001/rest/s1/trade/product-catalog', {
    headers: {
      'Authorization': `Basic ${credentials}`
    }
  });
}
