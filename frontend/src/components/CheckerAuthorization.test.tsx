import { render, screen } from '@testing-library/react';
import { CheckerAuthorization } from './CheckerAuthorization';

describe('CheckerAuthorization Risk Assessment', () => {
    it('presents the risk matrix including limit compliance and discrepancy status', () => {
        render(<CheckerAuthorization />);
        expect(screen.getByText(/Transaction Risk Analysis/i)).toBeInTheDocument();
        expect(screen.getByText(/Limit Compliance:/i)).toBeInTheDocument();
        expect(screen.getByText(/Discrepancy Severity:/i)).toBeInTheDocument();
        expect(screen.getByText(/Approve Transaction/i)).toBeInTheDocument();
    });
});
