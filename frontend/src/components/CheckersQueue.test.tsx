import { render, screen, within, waitFor } from '@testing-library/react';
import { CheckersQueue } from './CheckersQueue';
import { tradeApi } from '../api/tradeApi';

jest.mock('../api/tradeApi', () => ({
    tradeApi: {
        getApprovals: jest.fn().mockResolvedValue({ approvalsList: [] })
    }
}));

const mockItem = {
    transactionId: 'TXN-001',
    instrumentId: '1',
    transactionRef: 'TF-IMP-001',
    module: 'Import LC',
    action: 'Issuance',
    makerUserId: 'maker1',
    baseEquivalentAmount: 50000,
    timeInQueue: '2h 15m',
    priorityEnumId: 'NORMAL',
    transactionStatusId: 'TXN_PENDING_APPROVAL',
    businessStateId: 'INST_ISSUED'
};

describe('CheckersQueue v3.0 (REQ-UI-CMN-02)', () => {
    it('displays priority column and sorts URGENT above NORMAL', () => {
        const items = [
            { ...mockItem, transactionId: '1', transactionRef: 'TX-A', priorityEnumId: 'NORMAL' },
            { ...mockItem, transactionId: '2', transactionRef: 'TX-B', priorityEnumId: 'URGENT' },
        ];
        render(<CheckersQueue items={items} />);
        const rows = screen.getAllByRole('row');
        // Rows[0] is header
        expect(within(rows[1]).getByText('TX-B')).toBeInTheDocument();
        expect(within(rows[2]).getByText('TX-A')).toBeInTheDocument();
    });

    it('shows PARTIAL APPROVED badge for Tier 4 pending second checker', () => {
        const items = [
            { ...mockItem, transactionId: '1', transactionStatusId: 'TXN_PARTIAL_APPROVED' },
        ];
        render(<CheckersQueue items={items} />);
        expect(screen.getByText(/PARTIAL APPROVED/i)).toBeInTheDocument();
    });

    it('renders tier indicator in KPI banner', () => {
        render(<CheckersQueue items={[]} userTier="TIER_3" />);
        expect(screen.getByText(/Your Authority: TIER 3/i)).toBeInTheDocument();
    });

    it('fetches data from API if no items provided', async () => {
        const mockApprovals = [{ ...mockItem, transactionRef: 'API-REF' }];
        (tradeApi.getApprovals as jest.Mock).mockResolvedValue({ approvalsList: mockApprovals });
        
        render(<CheckersQueue />);
        
        await waitFor(() => {
            expect(screen.getByText('API-REF')).toBeInTheDocument();
        });
    });
});
