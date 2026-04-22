import { render, screen, fireEvent } from '@testing-library/react';
import { CancellationRequest } from './CancellationRequest';

// ABOUTME: Test suite for LC Cancellation request mapping to REQ-IMP-PRC-06.
// UI Traceability: REQ-UI-IMP-10 (Cancellation Request)

describe('CancellationRequest (REQ-IMP-PRC-06)', () => {
    it('Requires beneficiary consent verification for irrevocable LC closure', () => {
        render(<CancellationRequest instrumentId="IMLC/2026/001" />);
        
        expect(screen.getByLabelText(/Beneficiary Consent Received/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Cancellation Reason/i)).toBeInTheDocument();
    });

    it('Confirms full limit release upon cancellation', () => {
        render(<CancellationRequest instrumentId="IMLC/2026/001" />);
        
        fireEvent.click(screen.getByLabelText(/Beneficiary Consent Received/i));
        
        expect(screen.getByText(/\$ \s*500,000/i)).toBeInTheDocument();
    });

    it('Disables submit button until consent is confirmed', () => {
        render(<CancellationRequest instrumentId="IMLC/2026/001" />);
        
        const submitBtn = screen.getByRole('button', { name: /Submit Cancellation/i });
        expect(submitBtn).toBeDisabled();
        
        fireEvent.click(screen.getByLabelText(/Beneficiary Consent Received/i));
        expect(submitBtn).not.toBeDisabled();
    });
});
