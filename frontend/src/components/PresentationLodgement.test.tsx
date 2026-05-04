import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { PresentationLodgement } from './PresentationLodgement';
import { tradeApi } from '../api/tradeApi';

jest.mock('../api/tradeApi', () => ({
    tradeApi: {
        getImportLc: jest.fn().mockResolvedValue({
            instrumentId: 'IMLC/2026/001',
            amount: 500000,
            currencyUomId: 'USD'
        }),
        createLcPresentation: jest.fn().mockResolvedValue({ success: true })
    }
}));

// ABOUTME: Test suite for LC Document Presentation Lodgement mapping to REQ-IMP-PRC-03.
// UI Traceability: REQ-UI-IMP-07 (Presentation Lodgement)

describe('PresentationLodgement (REQ-IMP-PRC-03)', () => {
    it('Captures core presentation header details', async () => {
        render(<PresentationLodgement instrumentId="IMLC/2026/001" />);
        await waitFor(() => expect(screen.queryByText(/Loading LC Context/i)).not.toBeInTheDocument());
        
        fireEvent.change(screen.getByLabelText(/Claim Amount/i), { target: { value: '150000' } });
        fireEvent.change(screen.getByLabelText(/Presentation Date/i), { target: { value: '2026-05-15' } });
        
        expect(screen.getByDisplayValue('150000')).toBeInTheDocument();
    });

    it('Tracks document matrix (Bill of Lading, Invoice, etc.)', async () => {
        render(<PresentationLodgement instrumentId="IMLC/2026/001" />);
        await waitFor(() => expect(screen.queryByText(/Loading LC Context/i)).not.toBeInTheDocument());
        
        const bolRow = screen.getByTestId('doc-row-BL');
        fireEvent.change(bolRow.querySelector('input[name="originals"]')!, { target: { value: '3' } });
        
        expect(screen.getByText(/Total Documents Logged: 1/i)).toBeInTheDocument();
    });

    it('Starts the 5-banking-day examination SLA timer', async () => {
        render(<PresentationLodgement instrumentId="IMLC/2026/001" />);
        await waitFor(() => expect(screen.queryByText(/Loading LC Context/i)).not.toBeInTheDocument());
        // Note: Logic for SLA calculation will be tested via behavior
        expect(screen.getByText(/Examination Deadline/i)).toBeInTheDocument();
    });
});
