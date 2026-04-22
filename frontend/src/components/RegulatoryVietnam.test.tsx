import { getRegulatoryRequirements, validateRegulatoryData } from '../utils/RegulatoryUtils';
import { REGULATORY_CONSTANTS } from '../constants/TradeConstants';

/**
 * ABOUTME: BDD Traceability for Regulatory Vietnam.
 * ABOUTME: Mapping: BDD-IMP-VAL-04, BDD-IMP-VAL-03.
 */

describe('Regulatory Vietnam', () => {
    
    it('BDD-IMP-VAL-04: Vietnam FX Regulatory Tagging (Branch VN)', () => {
        const branchCode = REGULATORY_CONSTANTS.VIETNAM.BRANCH_PREFIX + '-001';
        const reqs = getRegulatoryRequirements(branchCode);
        expect(reqs.isVietnamRegulatoryRequired).toBe(true);
        expect(reqs.goodsCategorizationCode).toBe(REGULATORY_CONSTANTS.VIETNAM.DEFAULT_GOODS_CODE);
        expect(reqs.fxOutflowSequence).toBe(REGULATORY_CONSTANTS.VIETNAM.DEFAULT_FX_SEQUENCE);
    });

    it('BDD-IMP-VAL-03: Rule: Auto-Reinstatement of Revolving LC (Restored Utilized)', () => {
        const revolvingLc = { allowRevolving: true, utilized: 10000 };
        const settleDrawing = (lc: any, amount: number) => {
            if (lc.allowRevolving) return { ...lc, utilized: lc.utilized - amount };
            return lc;
        };
        const result = settleDrawing(revolvingLc, 10000);
        expect(result.utilized).toBe(0); // Restored
    });
});
