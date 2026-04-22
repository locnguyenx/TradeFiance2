import { render, screen, waitFor } from '@testing-library/react';
import { ImportLcDashboard } from './ImportLcDashboard';
import { tradeApi } from '../api/tradeApi';

// ABOUTME: Test suite for Import LC Dashboard mapping to REQ-UI-IMP-02.
// UI Traceability: REQ-UI-IMP-02, REQ-UI-IMP-01

jest.mock('../api/tradeApi', () => ({
    tradeApi: {
        getImportLcs: jest.fn().mockResolvedValue({ 
            lcList: [{ 
                instrumentId: '1', 
                transactionRef: 'LC-2026-001', 
                applicantName: 'Global Corp',
                beneficiaryName: 'Export Ltd',
                amount: 50000,
                currency: 'USD',
                expiryDate: '2026-12-31',
                businessStateId: 'LC_ISSUED',
                slaDaysRemaining: 4
            }],
            lcListCount: 1 
        }),
        getKpis: jest.fn().mockResolvedValue({ 
            pendingDrafts: 5, 
            expiringSoon: 2, 
            discrepantDocs: 1 
        })
    }
}));

describe('ImportLcDashboard (REQ-UI-IMP-02)', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('Renders all standard KPI cards (REQ-UI-IMP-02)', async () => {
        render(<ImportLcDashboard />);
        await waitFor(() => {
            expect(screen.getByText(/Drafts Awaiting My Submission/i)).toBeInTheDocument();
            expect(screen.getByText('5')).toBeInTheDocument();
            expect(screen.getByText(/LCs Expiring within 7 Days/i)).toBeInTheDocument();
            expect(screen.getByText('2')).toBeInTheDocument();
            expect(screen.getByText(/Discrepant Presentations/i)).toBeInTheDocument();
            expect(screen.getByText('1')).toBeInTheDocument();
        });
    });

    it('Data table displays all mandatory columns including SLA Timer (REQ-UI-IMP-02)', async () => {
        render(<ImportLcDashboard />);
        await waitFor(() => {
            expect(screen.getByText('LC-2026-001')).toBeInTheDocument();
            expect(screen.getByText('Global Corp')).toBeInTheDocument();
            expect(screen.getByText('Export Ltd')).toBeInTheDocument();
            expect(screen.getByText('USD')).toBeInTheDocument();
            expect(screen.getByText('50,000')).toBeInTheDocument();
            expect(screen.getByText('2026-12-31')).toBeInTheDocument();
            expect(screen.getByText('ISSUED')).toBeInTheDocument();
            expect(screen.getByText('4 days')).toBeInTheDocument(); // SLA Timer
        });
    });

    it('Provides row level actions (REQ-UI-IMP-02)', async () => {
        render(<ImportLcDashboard />);
        await waitFor(() => {
            const actionBtn = screen.getByTestId('row-action-1');
            expect(actionBtn).toBeInTheDocument();
        });
    });

    it('Opens dropdown menu with lifecycle actions when clicked (REQ-UI-IMP-02)', async () => {
        const { fireEvent } = await import('@testing-library/react');
        render(<ImportLcDashboard />);
        await waitFor(() => screen.getByTestId('row-action-1'));
        
        const actionBtn = screen.getByTestId('row-action-1');
        fireEvent.click(actionBtn);

        expect(screen.getByText(/New Amendment/i)).toBeInTheDocument();
        expect(screen.getByText(/Present Documents/i)).toBeInTheDocument();
        expect(screen.getByText(/Request Cancellation/i)).toBeInTheDocument();
    });
});
