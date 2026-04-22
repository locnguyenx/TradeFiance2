import { render, screen } from '@testing-library/react';
import { LimitsDashboard } from './LimitsDashboard';

// ABOUTME: Test suite for Credit Facilities Dashboard mapping to REQ-UI-CMN-04.
// UI Traceability: REQ-UI-CMN-04

describe('LimitsDashboard (REQ-UI-CMN-04)', () => {
    it('Displays overall credit facility utilization and availability', () => {
        render(<LimitsDashboard />);
        expect(screen.getByText(/Total Facility Limit/i)).toBeInTheDocument();
        expect(screen.getByText(/Total Utilization/i)).toBeInTheDocument();
        expect(screen.getByText(/Available headroom/i)).toBeInTheDocument();
    });

    it('Renders facility list with utilization bars', () => {
        render(<LimitsDashboard />);
        expect(screen.getByText(/FAC-IMP-1002/i)).toBeInTheDocument();
        // Check for utilization percentage or bars
        expect(screen.getByText(/60%/i)).toBeInTheDocument(); 
    });

    it('Filters transaction list when an instrument category is clicked', async () => {
        const { fireEvent } = await import('@testing-library/react');
        render(<LimitsDashboard />);
        
        const sgCategory = screen.getByTestId('exposure-Shipping Guarantee');
        fireEvent.click(sgCategory);
        
        expect(screen.getByText(/SG\/NY\/22/i)).toBeInTheDocument();
    });
});
