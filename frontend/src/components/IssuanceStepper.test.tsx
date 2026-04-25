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

    const completeStep0 = async () => {
        expect(await screen.findByLabelText(/LC Product/i)).toBeInTheDocument();
        fireEvent.change(screen.getByLabelText(/Applicant/i), { target: { value: 'Global Corp' } });
        fireEvent.change(screen.getByLabelText(/LC Product/i), { target: { value: 'IMP_LC_STANDARD' } });
    };

    const completeStep1 = async () => {
        fireEvent.change(screen.getByLabelText(/Amount/i), { target: { value: '100000' } });
        fireEvent.change(screen.getByLabelText(/Currency/i), { target: { value: 'USD' } });
        fireEvent.change(screen.getByLabelText(/Expiry Place/i), { target: { value: 'AT COUNTERS' } });
        fireEvent.change(screen.getByLabelText(/Latest Shipment Date/i), { target: { value: '2026-12-31' } });
        fireEvent.change(screen.getByLabelText(/Description of Goods/i), { target: { value: 'Electronic components' } });
    };

    const completeStep2 = async () => {
        fireEvent.change(screen.getByLabelText(/Charge Allocation/i), { target: { value: 'BENEFICIARY' } });
        fireEvent.change(screen.getByLabelText(/Customer Facility/i), { target: { value: 'FAC-001' } });
    };

    it('renders product catalog dropdown on Step 1', async () => {
        render(<IssuanceStepper />);
        expect(await screen.findByLabelText(/LC Product/i)).toBeInTheDocument();
    });

    it('validates mandatory fields in Step 1 before moving to Step 2', async () => {
        render(<IssuanceStepper />);
        fireEvent.click(screen.getByTestId('next-button'));
        
        // Wait for the error banner to appear and check its content
        await waitFor(() => {
            const banner = document.querySelector('.error-banner');
            expect(banner).toBeInTheDocument();
            expect(banner?.textContent).toMatch(/LC Product/i);
            expect(banner?.textContent).toMatch(/Applicant/i);
        });
    });

    it('renders right-navigation section anchors on Step 2 after valid Step 1', async () => {
        render(<IssuanceStepper />);
        await completeStep0();
        fireEvent.click(screen.getByTestId('next-button'));

        const aside = screen.getByRole('complementary');
        expect(within(aside).getByText('Financials & Dates')).toBeInTheDocument();
    });

    it('renders charge allocation field on Step 3 after valid Step 1 and 2', async () => {
        render(<IssuanceStepper />);
        await completeStep0();
        fireEvent.click(screen.getByTestId('next-button')); // To Step 2
        
        await completeStep1();
        fireEvent.click(screen.getByTestId('next-button')); // To Step 3
        
        expect(screen.getByText(/Step 3: Margin & Charges/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Charge Allocation/i)).toBeInTheDocument();
    });

    it('validates SWIFT characters on goodsDescription blur', async () => {
        render(<IssuanceStepper />);
        await completeStep0();
        fireEvent.click(screen.getByTestId('next-button'));

        const goodsInput = screen.getByLabelText(/Description of Goods/i);
        fireEvent.change(goodsInput, { target: { value: 'Steel Rods @ 50mm' } });
        fireEvent.blur(goodsInput);
        
        await waitFor(() => {
            expect(screen.getByText(/invalid SWIFT character/i)).toBeInTheDocument();
        });
    });

    it('shows Save Draft only on the final Review step and handles submission', async () => {
        render(<IssuanceStepper />);
        await completeStep0();
        fireEvent.click(screen.getByTestId('next-button')); // To Step 2
        
        // On Step 2, Save Draft should NOT be visible
        expect(screen.queryByText(/Save Draft/i)).not.toBeInTheDocument();
        
        await completeStep1();
        fireEvent.click(screen.getByTestId('next-button')); // To Step 3
        
        await completeStep2();
        fireEvent.click(screen.getByTestId('next-button')); // To Step 4 (Review)

        // On Final Step, Save Draft SHOULD be visible
        expect(screen.getByText(/Save Draft/i)).toBeInTheDocument();
        
        fireEvent.click(screen.getByText(/Save Draft/i));
        expect(await screen.findByText(/Draft Saved Successfully/i)).toBeInTheDocument();
        
        fireEvent.click(screen.getByTestId('submit-button'));
        expect(await screen.findByText(/Successfully Submitted for Approval/i)).toBeInTheDocument();
    });

    it('blocks progression if there is a date error', async () => {
        render(<IssuanceStepper />);
        await completeStep0();
        fireEvent.click(screen.getByTestId('next-button')); // To Step 2
        
        // Input invalid dates (Expiry before Issue)
        fireEvent.change(screen.getByLabelText(/Issue Date/i), { target: { value: '2026-12-31' } });
        fireEvent.change(screen.getByLabelText(/Expiry Date/i), { target: { value: '2026-01-01' } });
        
        // Error should be visible below field already
        expect(await screen.findByText(/Expiry Date cannot be in the past or before Issue Date/i)).toBeInTheDocument();
        
        // Click Next
        fireEvent.click(screen.getByTestId('next-button'));
        
        // Should show date conflict error in banner
        await waitFor(() => {
            const banner = document.querySelector('.error-banner');
            expect(banner).toBeInTheDocument();
            expect(banner?.textContent).toMatch(/Date Conflict/i);
        });
    });
});
