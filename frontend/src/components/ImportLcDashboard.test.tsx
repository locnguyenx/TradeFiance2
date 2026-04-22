import { render, screen } from '@testing-library/react';
import { ImportLcDashboard } from './ImportLcDashboard';

describe('ImportLcDashboard Rendering', () => {
    it('constructs top KPI widgets and explicit transaction filters', () => {
        render(<ImportLcDashboard />);
        expect(screen.getByText(/Drafts Awaiting/i)).toBeInTheDocument();
        expect(screen.getByText(/LCs Expiring within 7 Days/i)).toBeInTheDocument();
        expect(screen.getByText('Active Transaction Data Table')).toBeInTheDocument();
    });
});
