import { render, screen, fireEvent, waitFor, within, act } from '@testing-library/react';
import { IssuanceStepper } from './IssuanceStepper';
import { tradeApi } from '../api/tradeApi';

// ABOUTME: High-fidelity tests for LC Issuance Stepper mapping to BDD and REQ-UI specs.
// BDD Traceability: BDD-IMP-FLOW-01, BDD-IMP-FLOW-02, BDD-IMP-VAL-02, BDD-IMP-ISS-01
// UI Traceability: REQ-UI-IMP-03

jest.mock('../api/tradeApi', () => ({
    tradeApi: {
        createLc: jest.fn().mockResolvedValue({ instrumentId: '900001', transactionRef: 'TF-IMP-26-0001' }),
        updateLc: jest.fn().mockResolvedValue({ instrumentId: '900001', transactionRef: 'TF-IMP-26-0001' }),
        validateLcSwiftFields: jest.fn().mockResolvedValue({ errors: [] }),
        getStandardClauses: jest.fn().mockResolvedValue([
            { clauseId: '1', clauseName: 'General Merchandise', clauseText: 'General merchandise as per Proforma Invoice...' }
        ]),
        getProductCatalog: jest.fn().mockResolvedValue({
            productList: [
                { productId: 'IMP_LC_STANDARD', productName: 'Standard Import LC', isActive: 'Y' },
                { productId: 'IMP_LC_USANCE', productName: 'Usance Import LC', isActive: 'Y' }
            ]
        }),
        getFeeConfigurations: jest.fn().mockResolvedValue({
            feeList: [
                { feeConfigId: '1', feeTypeEnumId: 'ISSUANCE_FEE', calculationMethodEnumId: 'PERCENTAGE', ratePercent: 0.125 }
            ]
        }),
        getParties: jest.fn().mockResolvedValue({
            partyList: [
                { partyId: 'GLOBAL_CORP', partyName: 'Global Corp', partyTypeEnumId: 'PARTY_COMMERCIAL' },
                { partyId: 'ADVISING_BANK', partyName: 'Advising Bank', partyTypeEnumId: 'PARTY_BANK', swiftBic: 'ADVBUK22' }
            ]
        }),
        getCustomerFacilities: jest.fn().mockResolvedValue({
            facilityList: [
                { facilityId: 'FAC-001', description: 'Working Capital Line', limitAmount: 1000000 }
            ]
        })
    }
}));

describe('IssuanceStepper v3.0 (BDD-IMP-FLOW-01, BDD-CMN-VAL-05)', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        (tradeApi.validateLcSwiftFields as jest.Mock).mockResolvedValue({ errors: [] });
        (tradeApi.createLc as jest.Mock).mockResolvedValue({ instrumentId: '900001', transactionRef: 'TF-IMP-26-0001' });
    });

    const completeStep0 = async () => {
        const productSelect = await screen.findByLabelText(/LC Product/i) as HTMLSelectElement;
        // Wait for mock data to populate options
        await waitFor(() => {
            expect(productSelect.options.length).toBeGreaterThan(1);
        });
        
        await act(async () => {
            fireEvent.change(screen.getByLabelText(/Applicant \(Tag 50\)/i), { target: { value: 'GLOBAL_CORP' } });
        });
        fireEvent.change(screen.getByLabelText(/LC Product/i), { target: { value: 'IMP_LC_STANDARD' } });
        fireEvent.change(screen.getByLabelText(/Beneficiary \(Tag 59\)/i), { target: { value: 'GLOBAL_CORP' } });
        fireEvent.change(screen.getByLabelText(/Advising Bank \(Receiver\)/i), { target: { value: 'ADVISING_BANK' } });
        fireEvent.change(screen.getByLabelText(/Customer Facility/i), { target: { value: 'FAC-001' } });
    };

    const completeStep1 = async () => {
        fireEvent.change(await screen.findByLabelText('Amount'), { target: { value: '100000' } });
        fireEvent.change(await screen.findByLabelText(/Currency/i), { target: { value: 'USD' } });
        fireEvent.change(await screen.findByLabelText(/Expiry Place/i), { target: { value: 'AT COUNTERS' } });
        fireEvent.change(await screen.findByLabelText(/Latest Shipment Date/i), { target: { value: '2026-12-31' } });
        fireEvent.change(await screen.findByLabelText(/Description of Goods \(Tag 45A\)/i), { target: { value: 'Electronic components' } });
    };

    const completeStep2 = async () => {
        fireEvent.change(screen.getByLabelText(/Charge Allocation/i), { target: { value: 'BENEFICIARY' } });
    };

    it('renders product catalog dropdown on Step 1', async () => {
        await act(async () => {
            render(<IssuanceStepper />);
        });
        expect(await screen.findByLabelText(/LC Product/i)).toBeInTheDocument();
    });

    it('validates mandatory fields in Step 1 before moving to Step 2', async () => {
        await act(async () => {
            render(<IssuanceStepper />);
        });
        fireEvent.click(screen.getByTestId('next-button'));
        
        // Wait for the error banner to appear and check its content
        await waitFor(() => {
            const banner = document.querySelector('.error-banner');
            expect(banner).toBeInTheDocument();
            expect(banner?.textContent).toMatch(/LC Product/i);
            expect(banner?.textContent).toMatch(/Applicant/i);
            expect(banner?.textContent).toMatch(/Beneficiary/i);
            expect(banner?.textContent).toMatch(/Advising Bank \(Receiver\)/i);
            expect(banner?.textContent).toMatch(/Customer Facility/i);
        });
    });

    it('auto-saves draft when moving from Step 1 to Step 2', async () => {
        await act(async () => {
            render(<IssuanceStepper />);
        });
        await completeStep0();
        await act(async () => {
            fireEvent.click(screen.getByTestId('next-button'));
        });

        await waitFor(() => {
            expect(tradeApi.createLc).toHaveBeenCalledWith(expect.objectContaining({
                businessStateId: 'LC_DRAFT',
                applicant: 'Global Corp'
            }));
        });
        
        const aside = screen.getByRole('complementary');
        expect(within(aside).getByText('Financials & Dates')).toBeInTheDocument();
    });

    it('renders charge allocation field on Step 3 after valid Step 1 and 2', async () => {
        await act(async () => {
            render(<IssuanceStepper />);
        });
        await completeStep0();
        await act(async () => {
            fireEvent.click(screen.getByTestId('next-button')); // To Step 2
        });
        await waitFor(() => expect(screen.getByText(/Step 2/i)).toBeInTheDocument());
        
        await completeStep1();
        await act(async () => {
            fireEvent.click(screen.getByTestId('next-button')); // To Step 3
        });
        await waitFor(() => expect(screen.getByText(/Step 3/i)).toBeInTheDocument());
        
        expect(screen.getByText(/Step 3: Margin & Charges/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Charge Allocation/i)).toBeInTheDocument();
    });

    it('calls backend validation when moving from Step 2 to Step 3', async () => {
        (tradeApi.validateLcSwiftFields as jest.Mock).mockResolvedValue({
            errors: [{ fieldName: 'goodsDescription', message: 'Z-Charset violation' }]
        });

        await act(async () => {
            render(<IssuanceStepper />);
        });
        await completeStep0();
        await act(async () => {
            fireEvent.click(screen.getByTestId('next-button')); 
        });
        
        // Wait for transition to Step 2
        await waitFor(() => expect(screen.getByText(/Step 2/i)).toBeInTheDocument());
        
        await completeStep1();
        await act(async () => {
            fireEvent.click(screen.getByTestId('next-button')); // Trigger Validation
        });
        
        await waitFor(() => {
            expect(tradeApi.validateLcSwiftFields).toHaveBeenCalled();
        });
        
        await waitFor(() => {
            expect(screen.getAllByText(/Z-Charset violation/i).length).toBeGreaterThan(0);
        });
        // Should STAY on Step 2 (label) because transition is blocked
        expect(screen.getByText(/Step 2: Main LC Information/i)).toBeInTheDocument();
        expect(screen.queryByText(/Step 3/i)).not.toBeInTheDocument();
    });

    it('shows Save Draft only on the final Review step and handles submission', async () => {
        await act(async () => {
            render(<IssuanceStepper />);
        });
        await completeStep0();
        await act(async () => {
            fireEvent.click(screen.getByTestId('next-button')); 
        });
        await waitFor(() => expect(screen.getByText(/Step 2/i)).toBeInTheDocument());
        
        // On Step 2, Save Draft should NOT be visible
        expect(screen.queryByText(/Save Draft/i)).not.toBeInTheDocument();
        
        await completeStep1();
        await act(async () => {
            fireEvent.click(screen.getByTestId('next-button')); 
        });
        await waitFor(() => expect(screen.getByText(/Step 3/i)).toBeInTheDocument());
        
        await completeStep2();
        await act(async () => {
            fireEvent.click(screen.getByTestId('next-button'));
        });
        await waitFor(() => expect(screen.getByText(/Step 4/i)).toBeInTheDocument());

        // On Final Step, Save Draft SHOULD be visible
        expect(screen.getByText(/Save Draft/i)).toBeInTheDocument();
        
        await act(async () => {
            fireEvent.click(screen.getByText(/Save Draft/i));
        });
        expect(await screen.findByText(/Draft Saved Successfully/i)).toBeInTheDocument();
        
        await act(async () => {
            fireEvent.click(screen.getByTestId('submit-button'));
        });
        expect(await screen.findByText(/Submission Successful/i)).toBeInTheDocument();
    });

    it('renders all required Bank fields in Step 1 and 2', async () => {
        await act(async () => {
            render(<IssuanceStepper />);
        });
        // Step 1 (Parties)
        expect(screen.getByLabelText(/Advise Through Bank/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Advising Bank/i)).toBeInTheDocument();
        
        await completeStep0();
        await act(async () => {
            fireEvent.click(screen.getByTestId('next-button')); 
        });
        
        // Wait for transition to Step 2 (Financials)
        await waitFor(() => expect(screen.getByText(/Step 2/i)).toBeInTheDocument());
        
        expect(screen.getByText(/Available With \(Tag 41A\/D\)/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Any Bank/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Specific Bank/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Drawee Bank/i)).toBeInTheDocument();
    });
});
