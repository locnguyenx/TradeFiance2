import { render, screen } from '@testing-library/react';
import { PartyDirectory } from './PartyDirectory';

describe('PartyDirectory Split-Pane Rendering', () => {
    it('renders a searchable list and a details panel for KYC data', () => {
        render(<PartyDirectory />);
        expect(screen.getByPlaceholderText(/Search Parties/i)).toBeInTheDocument();
        expect(screen.getByText('Party List')).toBeInTheDocument();
        expect(screen.getByText('KYC & Credit Details')).toBeInTheDocument();
        expect(screen.getByText(/AML Status:/i)).toBeInTheDocument();
    });
});
