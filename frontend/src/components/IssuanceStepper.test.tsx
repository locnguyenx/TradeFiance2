import { render, screen, fireEvent } from '@testing-library/react';
import { IssuanceStepper } from './IssuanceStepper';

describe('IssuanceStepper Horizontal Form', () => {
    it('advances through 5 strictly defined data entry steps sequentially', () => {
        render(<IssuanceStepper />);
        expect(screen.getByText('Step 1: Parties & Limits')).toBeInTheDocument();
        fireEvent.click(screen.getByText('Next'));
        expect(screen.getByText('Step 2: Financials & Dates')).toBeInTheDocument();
        fireEvent.click(screen.getByText('Next'));
        expect(screen.getByText('Step 3: Terms & Shipping')).toBeInTheDocument();
        fireEvent.click(screen.getByText('Next'));
        expect(screen.getByText('Step 4: Narratives (MT700 Block)')).toBeInTheDocument();
        fireEvent.click(screen.getByText('Next'));
        expect(screen.getByText('Step 5: Review & Submit')).toBeInTheDocument();
    });
});
