import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { AmendmentStepper } from './AmendmentStepper';
import { tradeApi } from '../api/tradeApi';

jest.mock('../api/tradeApi', () => ({
    tradeApi: {
        getImportLc: jest.fn().mockResolvedValue({
            instrumentId: 'IMLC/2026/001',
            amount: 500000,
            currencyUomId: 'USD',
            expiryDate: '2026-12-31',
            beneficiaryPartyId: 'PARTY_EXP_1',
            effectiveAmount: 500000,
            effectiveExpiryDate: '2026-12-31'
        }),
        createLcAmendment: jest.fn().mockResolvedValue({ success: true })
    }
}));

describe('AmendmentStepper v3.0 (REQ-UI-IMP-06)', () => {
    it('integrates with tradeApi.getImportLc to load real context', async () => {
        render(<AmendmentStepper lcId="IMLC/2026/001" />);
        await screen.findByText(/Current LC Context/i);
        expect(tradeApi.getImportLc).toHaveBeenCalledWith('IMLC/2026/001');
        expect(screen.getAllByText(/500,000/).length).toBeGreaterThan(0);
    });

    it('v3.0: displays Effective vs Snapshot values side-by-side during financial amendment', async () => {
        render(<AmendmentStepper lcId="IMLC/2026/001" />);
        await screen.findByText(/Current LC Context/i);
        
        fireEvent.click(screen.getByTestId('next-button')); // to Step 2
        
        expect(await screen.findByText(/Current Effective Amount:/i)).toBeInTheDocument();
        expect(screen.getAllByText(/500,000/).length).toBeGreaterThan(0);
        
        const deltaInput = screen.getByPlaceholderText(/e.g. \+50000/i);
        fireEvent.change(deltaInput, { target: { value: '25000' } });
        
        expect(screen.getAllByText(/525,000/).length).toBeGreaterThan(0);
    });

    it('v3.0: validates extension logic for expiry dates (only extensions allowed)', async () => {
        render(<AmendmentStepper lcId="IMLC/2026/001" />);
        await screen.findByText(/Current LC Context/i);
        
        fireEvent.click(screen.getByTestId('next-button')); // to Step 2
        
        const dateInput = screen.getByLabelText(/New Expiry Date/i);
        fireEvent.change(dateInput, { target: { value: '2025-11-30' } }); // Earlier than 2026-12-31
        
        fireEvent.click(screen.getByTestId('next-button')); // Try to proceed
        
        expect(await screen.findByText(/Expiry date must be an extension/i)).toBeInTheDocument();
    });

    it('v3.0: shows Tier 4 routing preview for amendments >$500,000 threshold', async () => {
        render(<AmendmentStepper lcId="IMLC/2026/001" />);
        await screen.findByText(/Current LC Context/i);
        
        fireEvent.click(screen.getByTestId('next-button')); // to Step 2
        
        const deltaInput = screen.getByPlaceholderText(/e.g. \+50000/i);
        fireEvent.change(deltaInput, { target: { value: '600000' } }); // Increase > 500k
        
        // Skip to Review Step (Step 5)
        fireEvent.click(screen.getByTestId('next-button')); // Step 3
        fireEvent.click(screen.getByTestId('next-button')); // Step 4
        fireEvent.click(screen.getByTestId('next-button')); // Step 5
        
        expect(await screen.findByText(/tier 4 authorization required/i)).toBeInTheDocument();
    });

    it('v3.0: displays Beneficiary Consent status badge in review card', async () => {
        render(<AmendmentStepper lcId="IMLC/2026/001" />);
        await screen.findByText(/Current LC Context/i);
        
        // Proceed to Review step
        fireEvent.click(screen.getByTestId('next-button')); // Step 2
        fireEvent.click(screen.getByTestId('next-button')); // Step 3
        fireEvent.click(screen.getByTestId('next-button')); // Step 4
        fireEvent.click(screen.getByTestId('next-button')); // Step 5
        
        const checkbox = screen.getByLabelText(/Advise Beneficiary Consent Required/i);
        expect(checkbox).toBeChecked();
        expect(screen.getByText(/Awaiting Beneficiary Consent/i)).toBeInTheDocument();
    });
});
