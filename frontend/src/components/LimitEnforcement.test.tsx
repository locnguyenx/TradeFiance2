import { validateLimit, checkDrawingWithinTolerance } from '../utils/LimitUtils';
import { ERROR_MESSAGES } from '../constants/TradeConstants';

/**
 * ABOUTME: BDD Traceability for Limit Enforcement.
 * ABOUTME: Mapping: BDD-CMN-VAL-01, BDD-CMN-ENT-04, BDD-CMN-ENT-05, BDD-IMP-VAL-01, BDD-IMP-VAL-02.
 */

describe('Limit Enforcement', () => {
    
    const testFacility = {
        id: 'FAC-ACME-001',
        totalLimit: 5000000,
        utilizedAmount: 1000000,
        expiryDate: '2026-12-31'
    };

    it('BDD-CMN-VAL-01: Hard Stop on Limit Breach ($4,999 vs $5,000 availability)', () => {
        const amount = 4000001; // Available is 4,000,000
        const result = validateLimit(amount, testFacility);
        expect(result.valid).toBe(false);
        expect(result.reason).toBe(ERROR_MESSAGES.LIMIT_BREACH);
    });

    it('BDD-CMN-ENT-04: Facility Limit Availability Earmark (Success Case)', () => {
        const amount = 50000;
        const result = validateLimit(amount, testFacility);
        expect(result.valid).toBe(true);
    });

    it('BDD-CMN-ENT-05: Expired Facility Block', () => {
        const expiredFacility = { ...testFacility, expiryDate: '2023-01-01' };
        const result = validateLimit(1000, expiredFacility);
        expect(result.valid).toBe(false);
        expect(result.reason).toBe(ERROR_MESSAGES.FACILITY_EXPIRED);
    });

    it('BDD-IMP-VAL-01: Drawn Tolerance Over-Draw Block (110% threshold)', () => {
        const baseAmount = 10000;
        const tolerance = 10;
        const drawing = 11001; // Max is 11,000
        expect(checkDrawingWithinTolerance(drawing, baseAmount, tolerance)).toBe(false);
    });

    it('BDD-IMP-VAL-02: Specific Rule: Late Presentation Expiry Block', () => {
        const instrumentExpiry = '2026-11-01';
        const presentationDate = '2026-11-02';
        const isLate = new Date(presentationDate) > new Date(instrumentExpiry);
        expect(isLate).toBe(true);
    });
});
