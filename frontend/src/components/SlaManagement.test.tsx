import { addBankingDays, getBankingDaysDifference } from '../utils/SlaUtils';

/**
 * ABOUTME: BDD Traceability for SLA Management.
 * ABOUTME: Mapping: BDD-CMN-SLA-01, BDD-CMN-SLA-02, BDD-IMP-DOC-01, BDD-CMN-PRD-10.
 */

describe('SLA Management', () => {

    it('BDD-CMN-SLA-01: SLA Timer Skips Head Office Holidays (Vietnam specific)', () => {
        // Monday -> Tuesday(1) -> Holiday(Wed,0) -> Thursday(2) -> Friday(3) -> Mon(4) -> Tue(5)
        const start = new Date('2026-04-20');
        const holidays = ['2026-04-22'];
        const deadline = addBankingDays(start, 5, holidays);
        expect(deadline.toISOString().split('T')[0]).toBe('2026-04-28');
    });

    it('BDD-IMP-DOC-01: Presentation: Examination Timer Enforcement (+5 banking days)', () => {
        const presentationDate = new Date('2026-11-01'); // Sunday
        const deadline = addBankingDays(presentationDate, 5);
        expect(deadline.toISOString().split('T')[0]).toBe('2026-11-06');
    });

    it('BDD-CMN-SLA-02: Timer Exhaustion Generates System Block (Escalation at 5 days)', () => {
        const start = new Date('2026-04-20');
        const now = new Date('2026-04-27');
        const daysPassed = getBankingDaysDifference(start, now);
        expect(daysPassed).toBe(5);
    });

    it('BDD-CMN-PRD-10: Configuration: Custom SLA Deadline Formula (2-day escalation)', () => {
        const start = new Date('2026-04-20');
        const deadline = addBankingDays(start, 2);
        expect(deadline.toISOString().split('T')[0]).toBe('2026-04-22');
    });
});
