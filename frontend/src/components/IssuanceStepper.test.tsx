import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { IssuanceStepper } from './IssuanceStepper';
import { tradeApi } from '../api/tradeApi';

// ABOUTME: High-fidelity tests for LC Issuance Stepper mapping to BDD and REQ-UI specs.
// BDD Traceability: BDD-IMP-FLOW-01, BDD-IMP-FLOW-02, BDD-IMP-VAL-02, BDD-IMP-ISS-01
// UI Traceability: REQ-UI-IMP-03

jest.mock('../api/tradeApi', () => ({
    tradeApi: {
        createLc: jest.fn().mockResolvedValue({ instrumentId: '100001', transactionRef: 'LC-2026-001' }),
        getStandardClauses: jest.fn().mockResolvedValue([
            { clauseId: '1', clauseName: 'General Merchandise', clauseText: 'General merchandise as per Proforma Invoice...' }
        ]),
        getProductCatalog: jest.fn().mockResolvedValue({
            productList: [
                { productCatalogId: 'IMP_LC_STANDARD', productName: 'Standard Import LC', isActive: 'Y' }
            ]
        }),
        getFeeConfigurations: jest.fn().mockResolvedValue({
            feeList: [
                { feeConfigId: '1', feeTypeEnumId: 'ISSUANCE_FEE', calculationMethodEnumId: 'PERCENTAGE', ratePercent: 0.125 }
            ]
        })
    }
}));

describe('IssuanceStepper v3.0 (BDD-IMP-FLOW-01, BDD-CMN-VAL-05)', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('renders product catalog dropdown on Step 1', async () => {
        render(<IssuanceStepper />);
        expect(await screen.findByLabelText(/LC Product/i)).toBeInTheDocument();
    });

    it('renders right-navigation section anchors on Step 2', async () => {
        render(<IssuanceStepper />);
        // Complete Step 1
        fireEvent.change(screen.getByLabelText(/Applicant/i), { target: { value: 'Global Corp' } });
        fireEvent.click(screen.getByTestId('next-button'));

        const aside = screen.getByRole('complementary');
        expect(within(aside).getByText('Financials & Dates')).toBeInTheDocument();
        expect(within(aside).getByText('Terms & Shipping')).toBeInTheDocument();
        expect(within(aside).getByText('Narratives')).toBeInTheDocument();
    });

    it('renders charge allocation field on Step 3', async () => {
        render(<IssuanceStepper />);
        // Move to Step 3
        fireEvent.change(screen.getByLabelText(/Applicant/i), { target: { value: 'Global Corp' } });
        fireEvent.click(screen.getByTestId('next-button')); // Step 2
        fireEvent.click(screen.getByTestId('next-button')); // Step 3
        
        expect(screen.getByText(/Step 3: Margin & Charges/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Charge Allocation/i)).toBeInTheDocument();
    });

    it('validates SWIFT characters on goodsDescription blur', async () => {
        render(<IssuanceStepper />);
        // Move to Step 2
        fireEvent.change(screen.getByLabelText(/Applicant/i), { target: { value: 'Global Corp' } });
        fireEvent.click(screen.getByTestId('next-button'));

        const goodsInput = screen.getByLabelText(/Description of Goods/i);
        fireEvent.change(goodsInput, { target: { value: 'Steel Rods @ 50mm' } });
        fireEvent.blur(goodsInput);
        
        await waitFor(() => {
            expect(screen.getByText(/invalid SWIFT character/i)).toBeInTheDocument();
        });
    });

    it('renders v3.0 shipping fields (Tolerance, Port of Discharge) on Step 2', async () => {
        render(<IssuanceStepper />);
        // Move to Step 2
        fireEvent.change(screen.getByLabelText(/Applicant/i), { target: { value: 'Global Corp' } });
        fireEvent.click(screen.getByTestId('next-button'));

        expect(screen.getByLabelText(/Positive Tolerance/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Negative Tolerance/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Port of Discharge/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Latest Shipment Date/i)).toBeInTheDocument();
    });

    it('renders v3.0 confirmation and LC type fields on Step 1', async () => {
        render(<IssuanceStepper />);
        expect(screen.getByLabelText(/LC Type/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Confirmation Instruction/i)).toBeInTheDocument();
    });
});
