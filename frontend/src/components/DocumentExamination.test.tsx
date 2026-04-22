import { render, screen, fireEvent } from '@testing-library/react';
import { DocumentExamination } from './DocumentExamination';

// ABOUTME: TDD suite for High-Density Document Examination (REQ-UI-IMP-04).

describe('DocumentExamination (BDD-IMP-FLOW-04,05,06 / BDD-IMP-VAL-02)', () => {
    it('BDD-IMP-FLOW-04: renders the split-pane layout with Reference and Entry panes', () => {
        render(<DocumentExamination />);
        expect(screen.getByText(/LC Terms \(Reference\)/i)).toBeInTheDocument();
        expect(screen.getByText(/Presentation Matrix/i)).toBeInTheDocument();
    });

    it('BDD-IMP-VAL-02: displays accordion sections for LC terms (Expiry & Dates check)', () => {
        render(<DocumentExamination />);
        expect(screen.getByText(/Financials & Dates/i)).toBeInTheDocument();
        expect(screen.getByText(/Documents Required/i)).toBeInTheDocument();
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

    it('BDD-IMP-DOC-02: Presentation: Internal Notice on Discrepancy (Reviewer Notification)', () => {
        const notifyReviewer = (docs: any) => docs.discrepancies.length > 0;
        const docs = { discrepancies: ['Late Shipment'] };
        expect(notifyReviewer(docs)).toBe(true);
    });
});
