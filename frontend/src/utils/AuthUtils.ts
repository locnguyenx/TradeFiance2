/**
 * ABOUTME: Maker-Checker and Authorization utilities.
 * ABOUTME: Implements Four-Eyes principle and Tier-based gating per BDD-CMN-VAL-02 and BDD-CMN-AUTH-02.
 */

export interface AuthorizationState {
    createdBy: string;
    reviewers: string[];
    requiredReviewers: number;
    tier: number;
}

/**
 * Checks if a user is allowed to authorize a transaction.
 * Target: BDD-CMN-VAL-02 (Segregation of Duties)
 */
export const canUserAuthorize = (userId: string, state: AuthorizationState): { allowed: boolean; reason?: string } => {
    if (userId === state.createdBy) {
        return { allowed: false, reason: 'Segregation of Duties: Maker cannot Authorize own record.' };
    }
    
    if (state.reviewers.includes(userId)) {
        return { allowed: false, reason: 'User has already reviewed this transaction.' };
    }

    return { allowed: true };
};

/**
 * Checks if the authorization process is complete based on tier requirements.
 * Target: BDD-CMN-AUTH-02 (Dual Checker)
 */
export const isAuthorizationComplete = (state: AuthorizationState): boolean => {
    return state.reviewers.length >= state.requiredReviewers;
};
