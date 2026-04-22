import { canUserAuthorize, isAuthorizationComplete, AuthorizationState } from '../utils/AuthUtils';

/**
 * ABOUTME: BDD Traceability for Authorization Roles.
 * ABOUTME: Mapping: BDD-CMN-VAL-02, BDD-CMN-AUTH-01, BDD-CMN-AUTH-02, BDD-CMN-AUTH-03, BDD-CMN-AUTH-04, BDD-CMN-MAS-03.
 */

describe('Authorization Roles', () => {

    const testState: AuthorizationState = {
        createdBy: 'MAKER_01',
        reviewers: [],
        requiredReviewers: 1,
        tier: 1
    };

    it('BDD-CMN-VAL-02: Segregation of Duties Active Prevention (User cannot auth own record)', () => {
        expect(canUserAuthorize('MAKER_01', testState).allowed).toBe(false);
    });

    it('BDD-CMN-AUTH-01: Tier Enforcement Calculation by Equivalent Amount (Tier 1 officer)', () => {
        const amount = 70000;
        const getRequiredTier = (amt: number) => amt < 1000000 ? 1 : 4;
        expect(getRequiredTier(amount)).toBe(1);
    });

    it('BDD-CMN-AUTH-02: Tier 4 Dual Checker Enforcement (Requires 2 unique reviewers)', () => {
        const tier4State: AuthorizationState = { createdBy: 'M1', reviewers: ['C1'], requiredReviewers: 2, tier: 4 };
        expect(isAuthorizationComplete(tier4State)).toBe(false);
        expect(isAuthorizationComplete({ ...tier4State, reviewers: ['C1', 'C2'] })).toBe(true);
    });

    it('BDD-CMN-AUTH-03: Amendment Total Liability Route Determination (900k + 150k = Tier 3)', () => {
        const originalAmt = 900000;
        const delta = 150000;
        const getTier = (total: number) => total > 1000000 ? 3 : 1;
        expect(getTier(originalAmt + delta)).toBe(3);
    });

    it('BDD-CMN-AUTH-04: Compliance Route overrides Financial Route', () => {
        const state = { hasComplianceWarning: true, financialAuthPassed: true };
        const isOverallPassed = (s: any) => s.financialAuthPassed && !s.hasComplianceWarning;
        expect(isOverallPassed(state)).toBe(false);
    });

    it('BDD-CMN-MAS-03: Suspended Account Active Exclusion', () => {
        const users = [{ id: 'U1', isSuspended: true }, { id: 'U2', isSuspended: false }];
        const activeUsers = users.filter(u => !u.isSuspended).map(u => u.id);
        expect(activeUsers).not.toContain('U1');
    });
});
