import { render, screen, fireEvent } from '@testing-library/react';
import { SettlementInitiation } from './SettlementInitiation';

// ABOUTME: Test suite for LC Settlement Initiation mapping to REQ-IMP-PRC-04.
// UI Traceability: REQ-UI-IMP-08 (Settlement Initiation)

describe('SettlementInitiation (BDD-IMP-FLOW-07,08 / BDD-IMP-VAL-01)', () => {
    it('BDD-IMP-FLOW-07: Requires debit account and value date for payment', () => {
        render(<SettlementInitiation instrumentId="IMLC/2026/001" />);
        
        expect(screen.getByLabelText(/Applicant Debit Account/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Value Date/i)).toBeInTheDocument();
    });

    it('BDD-IMP-VAL-01 & BDD-IMP-FLOW-07: Calculates settlement breakdown (Principal, Interest, Charges)', () => {
        render(<SettlementInitiation instrumentId="IMLC/2026/001" />);
        
        fireEvent.change(screen.getByLabelText(/Principal Amount/i), { target: { value: '100000' } });
        
        // 100,000 principal + 150 charges = 100,150
        expect(screen.getByText(/\$?\s*100,150/i)).toBeInTheDocument();
    });

    it('BDD-IMP-FLOW-08: Validates funds availability before final submission', async () => {
        render(<SettlementInitiation instrumentId="IMLC/2026/001" />);
        
        // Select an account first
        fireEvent.change(screen.getByLabelText(/Applicant Debit Account/i), { target: { value: 'ACC-101' } });
        
        fireEvent.click(screen.getByText(/Check Funds/i));
        expect(await screen.findByText(/Funds Verified/i)).toBeInTheDocument();
    });
});
