import { render, screen } from '@testing-library/react';
import { CheckerAuthorization } from './CheckerAuthorization';

// ABOUTME: Test suite for Checker Authorization Workspace mapping to REQ-UI-IMP-05.
// UI Traceability: REQ-UI-IMP-05, BDD-IMP-FLOW-03, BDD-IMP-ISS-01

describe('CheckerAuthorization (REQ-UI-IMP-05)', () => {
    it('Displays the risk-limit assessment widget (BDD-IMP-ISS-01)', () => {
        render(<CheckerAuthorization instrumentId="IMLC/2026/001" />);
        expect(screen.getByText(/Facility Exposure Widget/i)).toBeInTheDocument();
        expect(screen.getByText(/Compliance Deck/i)).toBeInTheDocument();
    });

    it('Renders transaction details with delta highlighting for amendments', () => {
        render(<CheckerAuthorization instrumentId="IMLC/2026/001" />);
        expect(screen.getByText(/Instrument Data/i)).toBeInTheDocument();
        expect(screen.getByText(/Instrument Data \(Highlighting Deltas\)/i)).toBeInTheDocument();
        // Check for delta tags or labels (checking for the arrow used in the delta box)
        expect(screen.getAllByText(/→/i).length).toBeGreaterThan(0);
    });

    it('Provides mandatory authorisation actions (Authorize/Reject)', () => {
        render(<CheckerAuthorization instrumentId="IMLC/2026/001" />);
        expect(screen.getByRole('button', { name: /Authorize/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Reject/i })).toBeInTheDocument();
    });
});
