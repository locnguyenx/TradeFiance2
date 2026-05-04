import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { CheckerAuthorization } from './CheckerAuthorization';
import { tradeApi } from '../api/tradeApi';

jest.mock('../api/tradeApi', () => ({
    tradeApi: {
        authorize: jest.fn().mockResolvedValue({ success: true }),
        rejectToMaker: jest.fn().mockResolvedValue({ success: true }),
        getTransaction: jest.fn().mockResolvedValue({
            transactionId: 'TXN-001',
            instrumentId: 'IMLC/2026/001',
            transactionStatusId: 'TXN_PARTIAL_APPROVED',
            proposedAmount: 600000,
            proposedExpiryDate: '2027-01-15'
        }),
        getImportLc: jest.fn().mockResolvedValue({
            instrumentId: 'IMLC/2026/001',
            baseEquivalentAmount: 450000,
            expiryDate: '2026-12-31',
            businessStateId: 'INST_ISSUED'
        }),
        getSwiftMessages: jest.fn().mockResolvedValue({ swiftMessageList: [] })
    }
}));

describe('CheckerAuthorization v3.0 (REQ-UI-IMP-05)', () => {
    it('calls rejectToMaker with rejection reason when Reject is clicked', async () => {
        render(<CheckerAuthorization transactionId="TXN-001" />);
        
        const rejectBtn = await screen.findByRole('button', { name: /Reject/i });
        fireEvent.click(rejectBtn);
        
        const textarea = screen.getByPlaceholderText(/Specify the reason/i);
        fireEvent.change(textarea, { target: { value: 'Incomplete documentation' } });
        
        fireEvent.click(screen.getByRole('button', { name: /Confirm Rejection/i }));
        
        await waitFor(() => {
            expect(tradeApi.rejectToMaker).toHaveBeenCalledWith('TXN-001', 'Incomplete documentation');
        });
    });

    it('displays Effective vs Snapshot values side-by-side for tiers', async () => {
        render(<CheckerAuthorization transactionId="TXN-001" />);
        await screen.findAllByText(/IMLC\/2026\/001/i);
        
        // Check for Snapshot (Old) in delta-notice
        const deltaNotice = await screen.findByText(/Amendment Snapshot/i);
        expect(within(deltaNotice.parentElement!).getByText(/450,000/i)).toBeInTheDocument();
        
        // Check for Effective (New) in the details panel
        expect(screen.getAllByText(/600,000/i).length).toBeGreaterThan(0);
    });

    it('renders second-checker requirement indicator for Tier 4', async () => {
        render(<CheckerAuthorization transactionId="TXN-001" />);
        
        expect(await screen.findByText(/Dual Checker Progress/i)).toBeInTheDocument();
        expect(screen.getByText(/Status: PARTIAL APPROVED/i)).toBeInTheDocument();
    });

    it('renders tier 4 detection warning for high-value pending txns', async () => {
        (tradeApi.getTransaction as jest.Mock).mockResolvedValueOnce({
            transactionId: 'TXN-002',
            instrumentId: 'IMLC/2026/002',
            transactionStatusId: 'TXN_SUBMITTED',
            proposedAmount: 750000,
        });
        
        render(<CheckerAuthorization transactionId="TXN-002" />);
        
        expect(await screen.findByText(/Tier 4 Transaction Detected/i)).toBeInTheDocument();
    });
});
