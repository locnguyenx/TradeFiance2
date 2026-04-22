import { REGULATORY_CONSTANTS } from '../constants/TradeConstants';

export interface RegulatoryTags {
    goodsCategorizationCode?: string;
    fxOutflowSequence?: string;
    isVietnamRegulatoryRequired: boolean;
}

/**
 * Determines if Vietnam regulatory tagging is required based on branch location.
 * Target: BDD-IMP-VAL-04
 */
export const getRegulatoryRequirements = (branchCode: string): RegulatoryTags => {
    const isVietnam = branchCode.startsWith(REGULATORY_CONSTANTS.VIETNAM.BRANCH_PREFIX);
    
    return {
        isVietnamRegulatoryRequired: isVietnam,
        ...(isVietnam && {
            goodsCategorizationCode: REGULATORY_CONSTANTS.VIETNAM.DEFAULT_GOODS_CODE,
            fxOutflowSequence: REGULATORY_CONSTANTS.VIETNAM.DEFAULT_FX_SEQUENCE
        })
    };
};

/**
 * Validates mandatory regulatory fields for Vietnam.
 * Target: BDD-IMP-VAL-04
 */
export const validateRegulatoryData = (data: RegulatoryTags): boolean => {
    if (!data.isVietnamRegulatoryRequired) return true;
    return !!(data.goodsCategorizationCode && data.fxOutflowSequence);
};
