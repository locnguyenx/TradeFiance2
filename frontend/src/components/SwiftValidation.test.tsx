import { filterXChars, splitLines, formatTolerance, formatSwiftAmount } from '../utils/SwiftUtils';

/**
 * ABOUTME: BDD Traceability for SWIFT MT7xx.
 * ABOUTME: Mapping: BDD-IMP-SWT-01, BDD-IMP-SWT-02, BDD-IMP-SWT-03, BDD-IMP-SWT-04, BDD-IMP-SWT-05.
 */

describe('SWIFT Validation', () => {

    it('BDD-IMP-SWT-01: MT700: X-Character Base Validation', () => {
        const input = 'Acme Corp @ 123 Street';
        const expected = 'Acme Corp   123 Street';
        expect(filterXChars(input)).toBe(expected);
    });

    it('BDD-IMP-SWT-05: MT700: Native 65-Character Array Splitting', () => {
        const longText = 'A'.repeat(65) + 'B'.repeat(65) + 'C'.repeat(10);
        const lines = splitLines(longText);
        expect(lines).toHaveLength(3);
        expect(lines[0]).toBe('A'.repeat(65));
        expect(lines[1]).toBe('B'.repeat(65));
        expect(lines[2]).toBe('C'.repeat(10));
    });

    it('BDD-IMP-SWT-03: MT700: Tolerance Output Formatter (Tag 39A)', () => {
        expect(formatTolerance(5, 5)).toBe('5/5');
        expect(formatTolerance(10, 0)).toBe('10/0');
    });

    it('BDD-IMP-SWT-02: MT700: Mandatory Block Validation (Amount Formatting)', () => {
        expect(formatSwiftAmount('USD', 500000)).toBe('USD500000,00');
    });

    it('BDD-IMP-SWT-04: MT700: \'A\' Designation Swap (59/59A) - BIC usage', () => {
        const buildBeneficiaryTag = (useBic: boolean, bic: string, name: string) => {
            return useBic ? { tag: '59A', value: bic } : { tag: '59', value: name };
        };
        const resultA = buildBeneficiaryTag(true, 'ACMEUS33', 'Acme Corp');
        expect(resultA.tag).toBe('59A');
        const resultText = buildBeneficiaryTag(false, 'ACMEUS33', 'Acme Corp');
        expect(resultText.tag).toBe('59');
    });
});
