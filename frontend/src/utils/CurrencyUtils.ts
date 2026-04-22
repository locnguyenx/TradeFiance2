/**
 * ABOUTME: Currency Precision and Rounding utilities.
 * ABOUTME: Implements ISO-standard decimal handling per BDD-CMN-FX-01..02.
 */

export const CURRENCY_CONFIG: Record<string, { decimals: number }> = {
    JPY: { decimals: 0 },
    USD: { decimals: 2 },
    EUR: { decimals: 2 },
    VND: { decimals: 0 }
};

/**
 * Rounds an amount based on the target currency's ISO decimal precision.
 * Target: BDD-CMN-FX-01, BDD-CMN-FX-02
 */
export const roundToCurrency = (amount: number, currency: string): number => {
    const config = CURRENCY_CONFIG[currency] || { decimals: 2 };
    const factor = Math.pow(10, config.decimals);
    return Math.round(amount * factor) / factor;
};

/**
 * Formats an amount for display with correct decimal positioning.
 */
export const formatCurrencyDisplay = (amount: number, currency: string): string => {
    const rounded = roundToCurrency(amount, currency);
    const config = CURRENCY_CONFIG[currency] || { decimals: 2 };
    return rounded.toLocaleString(undefined, {
        minimumFractionDigits: config.decimals,
        maximumFractionDigits: config.decimals
    });
};
