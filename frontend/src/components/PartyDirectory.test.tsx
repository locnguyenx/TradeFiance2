import { render, screen } from '@testing-library/react';
import { PartyDirectory } from './PartyDirectory';

// ABOUTME: Test suite for Party & KYC Directory mapping to REQ-UI-CMN-03.
// UI Traceability: REQ-UI-CMN-03

describe('PartyDirectory (REQ-UI-CMN-03)', () => {
    it('Displays a searchable list of parties with KYC and AML status', () => {
        render(<PartyDirectory />);
        expect(screen.getByPlaceholderText(/Search by Name or ID/i)).toBeInTheDocument();
        expect(screen.getByText(/Party Directory/i)).toBeInTheDocument();
        expect(screen.getByText(/Party Name/i)).toBeInTheDocument();
        // Use getAllByText for labels that appear multiple times or be more specific
        expect(screen.getAllByText(/KYC/i).length).toBeGreaterThan(0);
        expect(screen.getAllByText(/AML/i).length).toBeGreaterThan(0);
    });

    it('Provides a 5-tab interface for detailed vetting', () => {
        render(<PartyDirectory />);
        expect(screen.getByText('Kyc')).toBeInTheDocument();
        expect(screen.getByText('Compliance')).toBeInTheDocument();
        expect(screen.getByText('Roles')).toBeInTheDocument();
        expect(screen.getByText('Credit')).toBeInTheDocument();
        expect(screen.getByText('History')).toBeInTheDocument();
    });

    it('Switches content when different tabs are clicked', () => {
        const { fireEvent } = require('@testing-library/react');
        render(<PartyDirectory />);
        
        // Initial state
        expect(screen.getByText(/KYC & Vetting Details/i)).toBeInTheDocument();
        
        // Switch to Compliance
        fireEvent.click(screen.getByText('Compliance'));
        expect(screen.getByText(/Compliance Narrative & Flags/i)).toBeInTheDocument();
        
        // Switch to Roles
        fireEvent.click(screen.getByText('Roles'));
        expect(screen.getByText(/Authorized Capacity/i)).toBeInTheDocument();

        // Switch to Credit
        fireEvent.click(screen.getByText('Credit'));
        expect(screen.getByText(/Allocated Facilities/i)).toBeInTheDocument();
    });
});
