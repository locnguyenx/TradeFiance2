/**
 * ABOUTME: SLA and Banking Calendar utilities.
 * ABOUTME: Implements holiday-skipping logic and timer enforcement per BDD-CMN-SLA-* and BDD-IMP-DOC-01.
 */

/**
 * Checks if a given date is a weekend (Saturday or Sunday).
 */
export const isWeekend = (date: Date): boolean => {
    const day = date.getDay();
    return day === 0 || day === 6; // 0 = Sunday, 6 = Saturday
};

/**
 * Calculates the deadline by adding a number of banking days to a start date.
 * Skips weekends and specified holidays.
 * Target: BDD-CMN-SLA-01, BDD-IMP-DOC-01
 */
export const addBankingDays = (startDate: Date, daysToAdd: number, holidays: string[] = []): Date => {
    let resultDate = new Date(startDate);
    let addedDays = 0;
    
    while (addedDays < daysToAdd) {
        resultDate.setDate(resultDate.getDate() + 1);
        const isoString = resultDate.toISOString().split('T')[0];
        
        if (!isWeekend(resultDate) && !holidays.includes(isoString)) {
            addedDays++;
        }
    }
    return resultDate;
};

/**
 * Calculates the difference in banking days between two dates.
 * Target: BDD-CMN-SLA-02
 */
export const getBankingDaysDifference = (startDate: Date, endDate: Date, holidays: string[] = []): number => {
    let current = new Date(startDate);
    let diff = 0;
    
    while (current < endDate) {
        current.setDate(current.getDate() + 1);
        const isoString = current.toISOString().split('T')[0];
        if (!isWeekend(current) && !holidays.includes(isoString)) {
            diff++;
        }
    }
    return diff;
};
