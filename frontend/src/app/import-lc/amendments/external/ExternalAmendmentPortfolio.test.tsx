import { render, screen, waitFor, act } from '@testing-library/react';
import AmendmentPage from './page';
import { tradeApi } from '../../../../api/tradeApi';

// ABOUTME: Regression test for Amendment Portfolio page logic.
// UI Traceability: REQ-UI-IMP-03 (Instrument Detail View / Portfolio)

jest.mock('../../../../api/tradeApi');
jest.mock('next/navigation', () => ({
    useSearchParams: () => ({
        get: jest.fn().mockReturnValue(null)
    }),
    useRouter: () => ({
        push: jest.fn(),
        replace: jest.fn(),
        prefetch: jest.fn(),
        back: jest.fn(),
    }),
    usePathname: () => '/'
}));

describe('Amendment Portfolio Page', () => {
    const mockAmendments = [
        { amendmentId: 'AMD-001', instrumentId: 'LC240003', amendmentDate: '2026-05-01', amountIncrease: 5000, amountDecrease: 0 },
        { amendmentId: 'AMD-002', instrumentId: 'LC240003', amendmentDate: '2026-05-02', amountIncrease: 0, amountDecrease: 1000 }
    ];

    beforeEach(() => {
        jest.spyOn(console, 'error').mockImplementation(() => {});
    });

    afterEach(() => {
        (console.error as jest.Mock).mockRestore();
    });

    it('loads and displays the amendment list', async () => {
        (tradeApi.getExternalAmendments as jest.Mock).mockResolvedValue({ amendmentList: mockAmendments });

        await act(async () => {
            render(<AmendmentPage />);
        });

        await waitFor(() => {
            expect(screen.getByText('AMD-001')).toBeInTheDocument();
            expect(screen.getByText('AMD-002')).toBeInTheDocument();
        });

        expect(screen.getByText('+5000.00')).toBeInTheDocument();
        expect(screen.getByText('-1000.00')).toBeInTheDocument();
    });

    it('handles empty response gracefully', async () => {
        (tradeApi.getExternalAmendments as jest.Mock).mockResolvedValue({ amendmentList: [] });

        await act(async () => {
            render(<AmendmentPage />);
        });

        await waitFor(() => {
            expect(screen.getByText(/No records found/i)).toBeInTheDocument();
        });
    });
});
