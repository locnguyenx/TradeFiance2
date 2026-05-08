import { render, screen, waitFor, act } from '@testing-library/react';
import PresentationsPage from './page';
import { tradeApi } from '../../../api/tradeApi';

// ABOUTME: Regression test for Presentation Portfolio page logic.
// UI Traceability: REQ-UI-IMP-03 (Instrument Detail View / Portfolio)

jest.mock('../../../api/tradeApi');
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

describe('Presentation Portfolio Page', () => {
    const mockPresentations = [
        { presentationId: 'PRES-001', instrumentId: 'LC240003', presentationDate: '2026-05-01', claimAmount: 5000, isDiscrepant: 'N' },
        { presentationId: 'PRES-002', instrumentId: 'LC240003', presentationDate: '2026-05-02', claimAmount: 1000, isDiscrepant: 'Y' }
    ];

    beforeEach(() => {
        jest.spyOn(console, 'error').mockImplementation(() => {});
    });

    afterEach(() => {
        (console.error as jest.Mock).mockRestore();
    });

    it('loads and displays the presentation list', async () => {
        (tradeApi.getPresentations as jest.Mock).mockResolvedValue({ presentationList: mockPresentations });

        await act(async () => {
            render(<PresentationsPage />);
        });

        await waitFor(() => {
            expect(screen.getByText('PRES-001')).toBeInTheDocument();
            expect(screen.getByText('PRES-002')).toBeInTheDocument();
        });

        expect(screen.getByText('CLEAN')).toBeInTheDocument();
        expect(screen.getByText('DISCREPANT')).toBeInTheDocument();
    });
});
