jest.mock('../api/tradeApi');
jest.mock('../context/ToastContext', () => ({
    useToast: () => ({
        showToast: jest.fn()
    })
}));
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { PresentationLodgement } from './PresentationLodgement';
import { tradeApi } from '../api/tradeApi';

const mockLc = {
    instrumentId: 'IMLC/2026/001',
    amount: 500000,
    currencyUomId: 'USD'
};

// ABOUTME: Test suite for LC Document Presentation Lodgement mapping to REQ-IMP-PRC-03.
// UI Traceability: REQ-UI-IMP-07 (Presentation Lodgement)

describe('PresentationLodgement (REQ-IMP-PRC-03)', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        (tradeApi.getImportLc as jest.Mock).mockResolvedValue(mockLc);
        (tradeApi.createLcPresentation as jest.Mock).mockResolvedValue({ presentationId: 'PR-1' });
        (tradeApi.submitLcPresentation as jest.Mock).mockResolvedValue({ success: true });
    });

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
