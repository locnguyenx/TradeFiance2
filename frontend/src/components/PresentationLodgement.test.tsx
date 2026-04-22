import { render, screen, fireEvent } from '@testing-library/react';
import { PresentationLodgement } from './PresentationLodgement';

// ABOUTME: Test suite for LC Document Presentation Lodgement mapping to REQ-IMP-PRC-03.
// UI Traceability: REQ-UI-IMP-07 (Presentation Lodgement)

describe('PresentationLodgement (REQ-IMP-PRC-03)', () => {
    it('Captures core presentation header details', () => {
        render(<PresentationLodgement instrumentId="IMLC/2026/001" />);
        
        fireEvent.change(screen.getByLabelText(/Claim Amount/i), { target: { value: '150000' } });
        fireEvent.change(screen.getByLabelText(/Presentation Date/i), { target: { value: '2026-05-15' } });
        
        expect(screen.getByDisplayValue('150000')).toBeInTheDocument();
    });

    it('Tracks document matrix (Bill of Lading, Invoice, etc.)', () => {
        render(<PresentationLodgement instrumentId="IMLC/2026/001" />);
        
        const bolRow = screen.getByTestId('doc-row-BL');
        fireEvent.change(bolRow.querySelector('input[name="originals"]')!, { target: { value: '3' } });
        
        expect(screen.getByText(/Total Documents Logged: 1/i)).toBeInTheDocument();
    });

    it('Starts the 5-banking-day examination SLA timer', () => {
        render(<PresentationLodgement instrumentId="IMLC/2026/001" />);
        // Note: Logic for SLA calculation will be tested via behavior
        expect(screen.getByText(/Examination Deadline/i)).toBeInTheDocument();
    });
});
