import { tradeApi } from './tradeApi';

// ABOUTME: tradeApi.integration.test.ts performs real HTTP calls against a live Moqui backend.
// ABOUTME: Requires NEXT_PUBLIC_API_URL to be set to a running Moqui instance (e.g., http://localhost:8080).

describe('tradeApi Integration (Live Backend)', () => {
    const isIntegration = !!process.env.NEXT_PUBLIC_API_URL;

    // Skip tests if no backend URL is provided
    if (!isIntegration) {
        it('skips integration tests because NEXT_PUBLIC_API_URL is not set', () => {
            console.warn('Skipping tradeApi integration tests: NEXT_PUBLIC_API_URL is missing.');
        });
        return;
    }

    beforeEach(async () => {
        // Set maker credentials by default
        tradeApi.setCredentials('trade.maker', 'trade123');
    });

    afterEach(() => {
        tradeApi.clearCredentials();
    });

    it('fetches real KPIs from Moqui', async () => {
        const kpis = await tradeApi.getKpis();
        expect(kpis).toHaveProperty('pendingDrafts');
    });

    it('creates and then retrieves an LC by ID', async () => {
        const ref = `CRUD-TEST-${Date.now()}`;
        const createResult = await tradeApi.createLc({ transactionRef: ref, amount: 50000 });
        expect(createResult.instrumentId).toBeDefined();

        const lc = await tradeApi.getImportLc(createResult.instrumentId);
        expect(lc.transactionRef).toBe(ref);
        expect(lc.baseEquivalentAmount).toBe(50000);
    });

    it('updates an existing LC and verifies the change', async () => {
        const createResult = await tradeApi.createLc({ amount: 10000 });
        await tradeApi.updateLc(createResult.instrumentId, { amount: 20000 });
        
        const updatedLc = await tradeApi.getImportLc(createResult.instrumentId);
        expect(updatedLc.baseEquivalentAmount).toBe(20000);
    });

    it('handles pagination and count', async () => {
        // Ensure at least 2 records exist
        await tradeApi.createLc({ amount: 100 });
        await tradeApi.createLc({ amount: 200 });

        const result = await tradeApi.getImportLcs({ pageSize: 1 });
        expect(result.lcList.length).toBe(1);
        expect(result.lcListCount).toBeGreaterThanOrEqual(2);
    });

    it('performs filtered search by transactionRef', async () => {
        const uniqueRef = `SEARCH-${Date.now()}`;
        await tradeApi.createLc({ transactionRef: uniqueRef, amount: 123 });

        const result = await tradeApi.getImportLcs({ transactionRef: uniqueRef });
        expect(result.lcList.length).toBe(1);
        expect(result.lcList[0].transactionRef).toBe(uniqueRef);
    });

    it('performs a real authorization action', async () => {
        const createResult = await tradeApi.createLc({ amount: 100.0 });
        
        // Switch to checker for authorization
        tradeApi.setCredentials('trade.checker', 'trade123');
        const authResult = await tradeApi.authorize(createResult.instrumentId, 'trade.checker');
        expect(authResult).toHaveProperty('isAuthorized');
    });
});
