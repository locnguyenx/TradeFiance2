import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { CancellationRequest } from './CancellationRequest';
import { tradeApi } from '../api/tradeApi';

jest.mock('../api/tradeApi', () => ({
    tradeApi: {
        getImportLc: jest.fn().mockResolvedValue({
            instrumentId: 'IMLC/2026/001',
            amount: 500000,
            currencyUomId: 'USD'
        })
    }
}));

// ABOUTME: Test suite for LC Cancellation request mapping to REQ-IMP-PRC-06.
// UI Traceability: REQ-UI-IMP-10 (Cancellation Request)

describe('CancellationRequest (REQ-IMP-PRC-06)', () => {
    it('Requires beneficiary consent verification for irrevocable LC closure', async () => {
        render(<CancellationRequest instrumentId="IMLC/2026/001" />);
        await waitFor(() => expect(screen.queryByText(/Loading LC Context/i)).not.toBeInTheDocument());
        
        expect(screen.getByLabelText(/Beneficiary Consent Received/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Cancellation Reason/i)).toBeInTheDocument();
    });

    it('Confirms full limit release upon cancellation', async () => {
        render(<CancellationRequest instrumentId="IMLC/2026/001" />);
        await waitFor(() => expect(screen.queryByText(/Loading LC Context/i)).not.toBeInTheDocument());
        
        fireEvent.click(screen.getByLabelText(/Beneficiary Consent Received/i));
        
        expect(screen.getByText(/\$ \s*500,000/i)).toBeInTheDocument();
    });

    it('Disables submit button until consent is confirmed', async () => {
        render(<CancellationRequest instrumentId="IMLC/2026/001" />);
        await waitFor(() => expect(screen.queryByText(/Loading LC Context/i)).not.toBeInTheDocument());
        
        const submitBtn = screen.getByRole('button', { name: /Submit Cancellation/i });
        expect(submitBtn).toBeDisabled();
        
        fireEvent.click(screen.getByLabelText(/Beneficiary Consent Received/i));
        expect(submitBtn).not.toBeDisabled();
    });
});
