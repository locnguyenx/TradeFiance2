/**
 * ABOUTME: SWIFT message utilities for MT7xx series.
 * ABOUTME: Implements character filtering (X-char set) and line-length constraints per BDD-IMP-SWT-*.
 */

export const SWIFT_X_CHARS = /^[A-Za-z0-9/\-?:().,'+ ]+$/;
export const SWIFT_Z_CHARS = /^[A-Za-z0-9.,\-()/\='+:? !#&*<>;@]+$/;

/**
 * Filter string to SWIFT X-character set. 
 * Replaces invalid characters with spaces.
 * Target: BDD-IMP-SWT-01
 */
export const filterXChars = (input: string): string => {
    return input.split('').map(char => {
        return SWIFT_X_CHARS.test(char) ? char : ' ';
    }).join('');
};

/**
 * Checks if string contains only characters from SWIFT X-character set.
 */
export const isValidXChars = (input: string): boolean => {
    if (!input) return true;
    return input.split('\n').every(line => SWIFT_X_CHARS.test(line.trimEnd()));
};

/**
 * Checks if string contains only characters from SWIFT Z-character set.
 */
export const isValidZChars = (input: string): boolean => {
    if (!input) return true;
    return input.split('\n').every(line => SWIFT_Z_CHARS.test(line.trimEnd()));
};

/**
 * Returns the number of lines in a multi-line string.
 */
export const getLineCount = (input: string): number => {
    if (!input) return 0;
    return input.split('\n').length;
};

/**
 * Splits long text into an array of lines, each maximum 65 characters.
 * Target: BDD-IMP-SWT-05
 */
export const splitLines = (text: string, maxLen: number = 65): string[] => {
    const lines: string[] = [];
    const filtered = filterXChars(text);
    
    for (let i = 0; i < filtered.length; i += maxLen) {
        lines.push(filtered.substring(i, i + maxLen));
    }
    return lines;
};

/**
 * Formats tolerance values for Tag 39A.
 * Target: BDD-IMP-SWT-03
 */
export const formatTolerance = (positive: number, negative: number): string => {
    return `${positive}/${negative}`;
};

/**
 * Formats amount for SWIFT (e.g. USD500000,).
 * Target: BDD-IMP-SWT-02
 */
export const formatSwiftAmount = (currency: string, amount: number): string => {
    const formattedAmount = amount.toFixed(2).replace('.', ',');
    return `${currency}${formattedAmount}`;
};
