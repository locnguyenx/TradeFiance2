import { render, screen, within } from '@testing-library/react';
import { CheckersQueue } from './CheckersQueue';

const mockItem = {
    instrumentId: '1',
    transactionRef: 'TF-IMP-001',
    module: 'Import LC',
    action: 'Issuance',
    makerUserId: 'maker1',
    baseEquivalentAmount: 50000,
    timeInQueue: '2h 15m',
    priorityEnumId: 'NORMAL',
    lifecycleStatusId: 'INST_PENDING_APPROVAL'
};

describe('CheckersQueue v3.0 (REQ-UI-CMN-02)', () => {
    it('displays priority column and sorts URGENT above NORMAL', () => {
        const items = [
            { ...mockItem, id: '1', transactionRef: 'TX-A', priorityEnumId: 'NORMAL' },
            { ...mockItem, id: '2', transactionRef: 'TX-B', priorityEnumId: 'URGENT' },
        ];
        // @ts-ignore - CheckersQueue doesn't accept items prop yet
        render(<CheckersQueue items={items} />);
        const rows = screen.getAllByRole('row');
        // Header + 2 data rows. URGENT (TX-B) should appear first.
        expect(within(rows[1]).getByText('TX-B')).toBeInTheDocument();
        expect(within(rows[2]).getByText('TX-A')).toBeInTheDocument();
    });

    it('shows PARTIAL APPROVAL badge for Tier 4 pending second checker', () => {
        const items = [
            { ...mockItem, id: '1', lifecycleStatusId: 'INST_PARTIAL_APPROVAL' },
        ];
        // @ts-ignore
        render(<CheckersQueue items={items} />);
        expect(screen.getByText(/PARTIAL APPROVAL/i)).toBeInTheDocument();
    });

    it('renders tier indicator in KPI banner', () => {
        // @ts-ignore
        render(<CheckersQueue items={[]} userTier="TIER_3" />);
        expect(screen.getByText(/Your Authority: TIER 3/i)).toBeInTheDocument();
    });
});
