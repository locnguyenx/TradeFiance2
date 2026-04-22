import { render, screen } from '@testing-library/react';
import { PartyDirectory } from './PartyDirectory';

// ABOUTME: Test suite for Party & KYC Directory mapping to REQ-UI-CMN-03.
// UI Traceability: REQ-UI-CMN-03

describe('PartyDirectory (REQ-UI-CMN-03)', () => {
    it('Displays a searchable list of parties with role and KYC status', () => {
        render(<PartyDirectory />);
        expect(screen.getByPlaceholderText(/Search Parties/i)).toBeInTheDocument();
        expect(screen.getByText(/Party List/i)).toBeInTheDocument();
        // Check for specific columns/data points in the list
        expect(screen.getByText(/Legal Name/i)).toBeInTheDocument();
        expect(screen.getByText(/Role/i)).toBeInTheDocument();
        expect(screen.getByText(/KYC Status/i)).toBeInTheDocument();
    });

    it('Provides a tabbed interface for detailed vetting (REQ-UI-CMN-03)', () => {
        render(<PartyDirectory />);
        expect(screen.getByRole('tab', { name: /KYC & Compliance/i })).toBeInTheDocument();
        expect(screen.getByRole('tab', { name: /Credit Facilities/i })).toBeInTheDocument();
        expect(screen.getByRole('tab', { name: /Trade History/i })).toBeInTheDocument();
    });

    it('Switches content when different tabs are clicked', () => {
        const { fireEvent } = require('@testing-library/react');
        render(<PartyDirectory />);
        
        // Initial state
        expect(screen.getByText(/AML Status/i)).toBeInTheDocument();
        
        // Switch to Credit
        fireEvent.click(screen.getByRole('tab', { name: /Credit Facilities/i }));
        expect(screen.getByText('Approved Global Limit')).toBeInTheDocument();
        expect(screen.queryByText(/AML Status/i)).not.toBeInTheDocument();
    });
});
