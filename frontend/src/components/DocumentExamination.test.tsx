import { render, screen, fireEvent } from '@testing-library/react';
import { DocumentExamination } from './DocumentExamination';

// ABOUTME: TDD suite for High-Density Document Examination (REQ-UI-IMP-04).

describe('DocumentExamination (REQ-UI-IMP-04)', () => {
    it('renders the split-pane layout with Reference and Entry panes', () => {
        render(<DocumentExamination />);
        expect(screen.getByText(/LC Terms \(Reference\)/i)).toBeInTheDocument();
        expect(screen.getByText(/Presentation Matrix/i)).toBeInTheDocument();
    });

    it('displays accordion sections for LC terms', () => {
        render(<DocumentExamination />);
        expect(screen.getByText(/Financials & Dates/i)).toBeInTheDocument();
        expect(screen.getByText(/Documents Required/i)).toBeInTheDocument();
    });

    it('contains a document matrix with inputs for originals/copies', () => {
        render(<DocumentExamination />);
        expect(screen.getAllByText(/Commercial Invoice/i).length).toBeGreaterThan(0);
        const inputs = screen.getAllByRole('spinbutton');
        expect(inputs.length).toBeGreaterThan(0);
    });

    it('allows toggling between Clean and Discrepant decisions', () => {
        render(<DocumentExamination />);
        const cleanBtn = screen.getByText(/Clean/i);
        const discrepantBtn = screen.getByText(/Discrepant/i);
        
        fireEvent.click(discrepantBtn);
        expect(discrepantBtn).toHaveClass('active');
        
        fireEvent.click(cleanBtn);
        expect(cleanBtn).toHaveClass('active');
    });
});
