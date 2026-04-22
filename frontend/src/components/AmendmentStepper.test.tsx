import { render, screen, fireEvent } from '@testing-library/react';
import { AmendmentStepper } from './AmendmentStepper';

// ABOUTME: Test suite for LC Amendment workflow mapping to REQ-IMP-PRC-02.
// UI Traceability: REQ-UI-IMP-06 (Amendment Stepper)

describe('AmendmentStepper (REQ-IMP-PRC-02)', () => {
    it('Loads existing LC context in Step 1', () => {
        render(<AmendmentStepper lcId="IMLC/2026/001" />);
        expect(screen.getByText(/Current LC Context/i)).toBeInTheDocument();
        expect(screen.getByText(/IMLC\/2026\/001/i)).toBeInTheDocument();
    });

    it('Allows input of financial delta (Amount increase/decrease) in Step 2', () => {
        render(<AmendmentStepper lcId="IMLC/2026/001" />);
        // Move to Step 2
        fireEvent.click(screen.getByTestId('next-button'));
        
        expect(screen.getByLabelText(/Amount Adjustment/i)).toBeInTheDocument();
        const amountInput = screen.getByPlaceholderText(/e.g. \+50000/i);
        fireEvent.change(amountInput, { target: { value: '50000' } });
        expect(screen.getByText(/New Total Liability: \$ 550,000/i)).toBeInTheDocument();
    });

    it('Tracks Beneficiary Consent requirement', () => {
        render(<AmendmentStepper lcId="IMLC/2026/001" />);
        // Move to Review Step
        for(let i=0; i<4; i++) fireEvent.click(screen.getByTestId('next-button'));
        
        expect(screen.getByLabelText(/Advise Beneficiary Consent Required/i)).toBeInTheDocument();
    });

    it('Generates MT 707 SWIFT Preview in Step 4', () => {
        render(<AmendmentStepper lcId="IMLC/2026/001" />);
        // Step 2 entry
        fireEvent.click(screen.getByTestId('next-button'));
        fireEvent.change(screen.getByPlaceholderText(/e.g. \+50000/i), { target: { value: '50000' } });
        
        // Move to Step 4
        fireEvent.click(screen.getByTestId('next-button')); // to step 3
        fireEvent.click(screen.getByTestId('next-button')); // to step 4
        
        expect(screen.getByRole('heading', { name: /MT 707 Preview/i })).toBeInTheDocument();
        const swiftBlock = screen.getByTestId('swift-block');
        expect(swiftBlock.textContent).toContain(':32B: USD50000');
    });
});
