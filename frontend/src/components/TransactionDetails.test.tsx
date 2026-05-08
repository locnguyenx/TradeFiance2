import { render, screen, waitFor, act } from '@testing-library/react';
import { TransactionDetails } from './TransactionDetails';
import { tradeApi } from '../api/tradeApi';

// ABOUTME: Regression test for TransactionDetails component.
// UI Traceability: REQ-UI-TXN-01 (Transaction Detail View)

jest.mock('../api/tradeApi');
jest.mock('next/navigation', () => ({
    useRouter: () => ({
        push: jest.fn(),
    }),
}));

describe('TransactionDetails Component', () => {
    const mockTransaction = {
        transactionId: 'TXN100109',
        instrumentId: 'LC100003',
        transactionTypeEnumId: 'IMP_NEW',
        transactionStatusId: 'TX_PENDING',
        transactionDate: '2026-05-01',
        makerUserId: 'trade.admin',
        proposedAmount: 1000
    };

    const mockInstrument = {
        instrumentId: 'LC100003',
        instrumentRef: 'LC-2026-001',
        amount: 1000,
        currencyUomId: 'USD',
        businessStateId: 'LC_ISSUED'
    };

    beforeEach(() => {
        jest.clearAllMocks();
        jest.spyOn(console, 'error').mockImplementation(() => {});
    });

    afterEach(() => {
        (console.error as jest.Mock).mockRestore();
    });

    it('successfully loads and displays transaction and instrument details', async () => {
        (tradeApi.getTransaction as jest.Mock).mockResolvedValue(mockTransaction);
        (tradeApi.getImportLc as jest.Mock).mockResolvedValue(mockInstrument);

        await act(async () => {
            render(<TransactionDetails transactionId="TXN100109" />);
        });

        await waitFor(() => {
            expect(screen.getByText('TXN100109')).toBeInTheDocument();
            expect(screen.getAllByText('LC100003').length).toBeGreaterThan(0);
            expect(tradeApi.getTransaction).toHaveBeenCalledWith('TXN100109');
            expect(tradeApi.getImportLc).toHaveBeenCalledWith('LC100003');
        });
    });

    it('displays error message when transaction fails to load', async () => {
        (tradeApi.getTransaction as jest.Mock).mockRejectedValue(new Error('404'));

        await act(async () => {
            render(<TransactionDetails transactionId="TXN100109" />);
        });

        await waitFor(() => {
            expect(screen.getByText(/Transaction record not found/i)).toBeInTheDocument();
        });
    });
});
