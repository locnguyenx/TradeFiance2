import { render, screen, fireEvent, waitFor } from '@testing-library/react';
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
        ])
    }
}));

describe('IssuanceStepper (REQ-UI-IMP-03 / BDD-IMP-FLOW-01,02)', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('Step 1: Displays Available Facility and KYC Status (REQ-UI-IMP-03 / BDD-IMP-ISS-02)', async () => {
        render(<IssuanceStepper />);
        // Simulate selecting an applicant (once search is implemented)
        // For now, check if the placeholder/empty state widgets are present
        expect(screen.getByText(/Available Facility Limit/i)).toBeInTheDocument();
        expect(screen.getByText(/KYC Status/i)).toBeInTheDocument();
    });

    it('Step 2: Validates Expiry Date must be after Issue Date (BDD-IMP-VAL-02)', async () => {
        render(<IssuanceStepper />);
        // Move to Step 2
        fireEvent.change(screen.getByLabelText(/Applicant/i), { target: { value: 'Global Corp' } });
        fireEvent.change(screen.getByLabelText(/Beneficiary/i), { target: { value: 'Export Ltd' } });
        fireEvent.click(screen.getByTestId('next-button'));

        // Set invalid date range
        fireEvent.change(screen.getByLabelText(/Issue Date/i), { target: { value: '2026-01-10' } });
        fireEvent.change(screen.getByLabelText(/Expiry Date/i), { target: { value: '2026-01-01' } });

        expect(screen.getByText(/Expiry Date cannot be in the past or before Issue Date/i)).toBeInTheDocument();
        expect(screen.getByTestId('next-button')).toBeDisabled();
    });

    it('BDD-IMP-FLOW-01: Successfully creates a Draft Application', async () => {
        render(<IssuanceStepper />);
        // Fill out minimum fields for Step 1
        fireEvent.change(screen.getByLabelText(/Applicant/i), { target: { value: 'Global Corp' } });
        
        const saveDraftBtn = screen.getByText(/Save Draft/i);
        fireEvent.click(saveDraftBtn);

        await waitFor(() => {
            expect(tradeApi.createLc).toHaveBeenCalledWith(expect.objectContaining({
                businessStateId: 'LC_DRAFT',
                applicantName: 'Global Corp'
            }));
        });
    });

    it('REQ-UI-IMP-03: Full 5-Step Flow to Submission (BDD-IMP-FLOW-02)', async () => {
        render(<IssuanceStepper />);
        
        // Step 1: Parties
        fireEvent.change(screen.getByLabelText(/Applicant/i), { target: { value: 'Global Corp' } });
        fireEvent.click(screen.getByTestId('next-button'));

        // Step 2: Financials
        expect(screen.getByText(/Step 2: Financials & Dates/i)).toBeInTheDocument();
        fireEvent.change(screen.getByLabelText(/Amount/i), { target: { value: '50000' } });
        fireEvent.click(screen.getByTestId('next-button'));

        // Step 3: Terms & Shipping
        expect(screen.getByText(/Step 3: Terms & Shipping/i)).toBeInTheDocument();
        fireEvent.click(screen.getByTestId('next-button'));

        // Step 4: Narratives
        expect(screen.getByText(/Step 4: Narratives/i)).toBeInTheDocument();
        fireEvent.click(screen.getByTestId('next-button'));

        // Step 5: Review
        expect(screen.getByText(/Step 5: Review & Submit/i)).toBeInTheDocument();
        expect(screen.getByText(/Amount: 50,000/i)).toBeInTheDocument();
        
        const submitBtn = screen.getByText(/Submit for Approval/i);
        fireEvent.click(submitBtn);

        await waitFor(() => {
            expect(tradeApi.createLc).toHaveBeenCalledWith(expect.objectContaining({
                businessStateId: 'LC_PENDING_APPROVAL'
            }));
        });
    });

    /*
    it('injects standard clauses into narrative fields', async () => {
        render(<IssuanceStepper />);
        
        // Navigate to Step 4
        fireEvent.change(screen.getByLabelText(/Applicant/i), { target: { value: 'Global Corp' } });
        fireEvent.click(screen.getByTestId('next-button')); // Step 2
        fireEvent.click(screen.getByTestId('next-button')); // Step 3
        fireEvent.click(screen.getByTestId('next-button')); // Step 4
        
        expect(screen.getByText(/Step 4: Narratives/i)).toBeInTheDocument();
        
        // Open clause selector for Goods
        const goodsClauseBtn = screen.getAllByText(/\+ Standard Clauses/i)[0];
        fireEvent.click(goodsClauseBtn);
        
        expect(screen.getByText(/Standard Clauses: GOODS/i)).toBeInTheDocument();
        
        // Select a clause
        const clauseItem = await screen.findByText(/General Merchandise/i);
        fireEvent.click(clauseItem);
        
        const textarea = screen.getByLabelText(/Description of Goods/i) as HTMLTextAreaElement;
        expect(textarea.value).toContain('General merchandise');
    });
    */
});
