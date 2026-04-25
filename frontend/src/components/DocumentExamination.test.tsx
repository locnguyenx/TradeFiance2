import { render, screen, fireEvent } from '@testing-library/react';
import { DocumentExamination } from './DocumentExamination';

// ABOUTME: TDD suite for High-Density Document Examination (REQ-UI-IMP-04).

describe('DocumentExamination (BDD-IMP-FLOW-04,05,06 / BDD-IMP-VAL-02)', () => {
    it('BDD-IMP-FLOW-04: renders the split-pane layout with Reference and Entry panes', () => {
        render(<DocumentExamination />);
        expect(screen.getByText(/LC Terms \(Reference\)/i)).toBeInTheDocument();
        expect(screen.getByText(/Presentation Matrix/i)).toBeInTheDocument();
    });

    it('BDD-IMP-VAL-02: displays accordion sections for LC terms with dynamic values', () => {
        const lc = { amount: 100000, expiryDate: '2026-12-31', positiveTolerance: '10' };
        // @ts-ignore
        render(<DocumentExamination instrument={lc} />);
        expect(screen.getByText(/Financials & Dates/i)).toBeInTheDocument();
        expect(screen.getByText(/100,000/)).toBeInTheDocument();
        expect(screen.getByText(/2026-12-31/)).toBeInTheDocument();
        expect(screen.getByText(/\+\/\- 10%/)).toBeInTheDocument();
    });

    it('v3.0: displays regulatory deadline (5 days from presentation)', () => {
        const presentationDate = '2026-05-01';
        // @ts-ignore
        render(<DocumentExamination presentationDate={presentationDate} />);
        expect(screen.getByText(/Regulatory Deadline/i)).toBeInTheDocument();
        // 2026-05-01 + 5 days = 2026-05-06
        expect(screen.getByText(/2026-05-06/)).toBeInTheDocument();
    });

    it('BDD-IMP-FLOW-04: contains a document matrix with inputs for originals/copies', () => {
        render(<DocumentExamination />);
        expect(screen.getAllByText(/Commercial Invoice/i).length).toBeGreaterThan(0);
        const inputs = screen.getAllByRole('spinbutton');
        expect(inputs.length).toBeGreaterThan(0);
    });

    it('BDD-IMP-FLOW-05 & BDD-IMP-FLOW-06: allows toggling between Clean and Discrepant decisions', () => {
        render(<DocumentExamination />);
        const decBtn = screen.getByText(/Discrepant/i);
        fireEvent.click(decBtn);
        expect(decBtn).toHaveClass('active');
    });

    it('v3.0: renders discrepancy code dropdown with ISBP-745 codes', () => {
        render(<DocumentExamination />);
        const select = screen.getByRole('combobox');
        expect(within(select).getByText(/Late Shipment/i)).toBeInTheDocument();
        expect(within(select).getByText(/Description of Goods Mismatch/i)).toBeInTheDocument();
    });

    it('v3.0: allows waiving discrepant documents and calls waiveDiscrepancy API', async () => {
        const { tradeApi } = require('../api/tradeApi');
        tradeApi.waiveDiscrepancy = jest.fn().mockResolvedValue({ success: true });
        
        render(<DocumentExamination />);
        
        // Log a discrepancy first
        fireEvent.change(screen.getByRole('combobox'), { target: { value: 'LATE_SHIPMENT' } });
        fireEvent.click(screen.getByRole('button', { name: /Log Discrepancy/i }));
        
        // Find Waive button/checkbox (assuming we add a Waive action to the logged items)
        const waiveBtn = await screen.findByRole('button', { name: /Waive/i });
        fireEvent.click(waiveBtn);
        
        expect(tradeApi.waiveDiscrepancy).toHaveBeenCalled();
    });
});

import { within } from '@testing-library/react';
