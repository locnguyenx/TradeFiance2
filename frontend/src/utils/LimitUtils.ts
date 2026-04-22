import { ERROR_MESSAGES } from '../constants/TradeConstants';

interface Facility {
    id: string;
    totalLimit: number;
    utilizedAmount: number;
    expiryDate: string; // ISO format
}

/**
 * Validates if an amount can be earmarked against a facility.
 * Target: BDD-CMN-VAL-01, BDD-CMN-ENT-05
 */
export const validateLimit = (amount: number, facility: Facility): { valid: boolean; reason?: string } => {
    // Check Expiry
    const now = new Date();
    const expiry = new Date(facility.expiryDate);
    if (expiry < now) {
        return { valid: false, reason: ERROR_MESSAGES.FACILITY_EXPIRED };
    }

    // Check Availability
    const available = facility.totalLimit - facility.utilizedAmount;
    if (amount > available) {
        return { valid: false, reason: ERROR_MESSAGES.LIMIT_BREACH };
    }

    return { valid: true };
};

/**
 * Calculates max drawing allowed including tolerance.
 * Target: BDD-IMP-VAL-01
 */
export const calculateMaxDrawing = (baseAmount: number, positiveTolerancePct: number): number => {
    return baseAmount + (baseAmount * (positiveTolerancePct / 100));
};

/**
 * Checks if a drawing exceeds the calculated tolerance.
 * Target: BDD-IMP-VAL-01
 */
export const checkDrawingWithinTolerance = (drawingAmount: number, baseAmount: number, positiveTolerancePct: number): boolean => {
    const max = calculateMaxDrawing(baseAmount, positiveTolerancePct);
    return drawingAmount <= max;
};
