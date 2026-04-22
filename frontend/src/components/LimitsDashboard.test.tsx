import { render, screen } from '@testing-library/react';
import { LimitsDashboard } from './LimitsDashboard';

describe('LimitsDashboard Rendering', () => {
    it('displays credit facility limits and availability utilization data', () => {
        render(<LimitsDashboard />);
        expect(screen.getByText(/Total Credit Facility Limit/i)).toBeInTheDocument();
        expect(screen.getByText(/Available Balance/i)).toBeInTheDocument();
        expect(screen.getByText(/FAC-IMP-100/i)).toBeInTheDocument();
    });
});
