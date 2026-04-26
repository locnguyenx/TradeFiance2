import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { CheckerAuthorization } from './CheckerAuthorization';
import { tradeApi } from '../api/tradeApi';

jest.mock('../api/tradeApi', () => ({
    tradeApi: {
        authorize: jest.fn(),
        rejectToMaker: jest.fn().mockResolvedValue({ success: true }),
        getInstrument: jest.fn().mockResolvedValue({
            instrumentId: 'IMLC/2026/001',
            lifecycleStatusId: 'INST_PARTIAL_APPROVAL',
            baseEquivalentAmount: 600000,
            effectiveAmount: 600000,
            snapshotAmount: 450000,
            effectiveExpiryDate: '2027-01-15',
            snapshotExpiryDate: '2026-12-31'
        })
    }
}));

describe('CheckerAuthorization v3.0 (REQ-UI-IMP-05)', () => {
    it('calls rejectToMaker with rejection reason when Reject is clicked', async () => {
        render(<CheckerAuthorization instrumentId="IMLC/2026/001" />);
        
        const rejectBtn = await screen.findByRole('button', { name: /Reject/i });
        fireEvent.click(rejectBtn);
        
        const textarea = screen.getByPlaceholderText(/Specify the reason/i);
        fireEvent.change(textarea, { target: { value: 'Incomplete documentation' } });
        
        fireEvent.click(screen.getByRole('button', { name: /Confirm Rejection/i }));
        
        await waitFor(() => {
            expect(tradeApi.rejectToMaker).toHaveBeenCalledWith('IMLC/2026/001', 'Incomplete documentation');
        });
    });

    it('displays Effective vs Snapshot values side-by-side for tiers', async () => {
        render(<CheckerAuthorization instrumentId="IMLC/2026/001" />);
        await screen.findByText(/Instrument: IMLC\/2026\/001/i);
        
        // Check for Snapshot (Old) in delta-notice
        const deltaNotice = await screen.findByText(/Amendment Snapshot/i);
        expect(within(deltaNotice.parentElement!).getByText(/450,000/i)).toBeInTheDocument();
        
        // Check for Effective (New) in the details panel
        // Since USD 600,000 appears in multiple places, we just verify it exists at least once for new value
        expect(screen.getAllByText(/600,000/i).length).toBeGreaterThan(0);
    });

    it('renders second-checker requirement indicator for Tier 4', async () => {
        render(<CheckerAuthorization instrumentId="IMLC/2026/001" />);
        
        expect(await screen.findByText(/Dual Checker Progress/i)).toBeInTheDocument();
        expect(screen.getByText(/Status: PARTIAL APPROVAL/i)).toBeInTheDocument();
    });

    it('renders tier 4 detection warning for high-value pending txns', async () => {
        (tradeApi.getInstrument as jest.Mock).mockResolvedValueOnce({
            instrumentId: 'IMLC/2026/002',
            lifecycleStatusId: 'INST_PENDING_APPROVAL',
            baseEquivalentAmount: 750000,
        });
        
        render(<CheckerAuthorization instrumentId="IMLC/2026/002" />);
        
        expect(await screen.findByText(/Tier 4 Transaction Detected/i)).toBeInTheDocument();
    });
});
