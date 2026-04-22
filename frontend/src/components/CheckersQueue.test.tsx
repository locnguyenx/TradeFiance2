import { render, screen } from '@testing-library/react';
import { CheckersQueue } from './CheckersQueue';

// ABOUTME: Test suite for Global Checker Queue mapping to REQ-UI-CMN-02.
// UI Traceability: REQ-UI-CMN-02

describe('CheckersQueue (REQ-UI-CMN-02)', () => {
    it('Renders the priority inbox with transaction metadata and SLA timers', () => {
        render(<CheckersQueue />);
        expect(screen.getByText(/Global Checker Queue/i)).toBeInTheDocument();
        // Check for specific columns
        expect(screen.getByText(/Transaction Ref/i)).toBeInTheDocument();
        expect(screen.getAllByText(/Module/i).length).toBeGreaterThan(0);
        expect(screen.getAllByText(/SLA/i).length).toBeGreaterThan(0);
    });

    it('Categorizes transactions by priority and status', () => {
        render(<CheckersQueue />);
        expect(screen.getByText(/High Priority/i)).toBeInTheDocument();
        expect(screen.getAllByText(/Pending Authorisation/i).length).toBeGreaterThan(0);
    });

    it('Provides row level actions to launch Authorization Workspace', () => {
        render(<CheckersQueue />);
        const authButtons = screen.getAllByRole('button', { name: /Authorize/i });
        expect(authButtons.length).toBeGreaterThan(0);
    });

    it('Launches the Full-Screen Authorization Modal when row action is clicked (REQ-UI-CMN-02)', async () => {
        const { fireEvent } = await import('@testing-library/react');
        render(<CheckersQueue />);
        
        const authBtn = screen.getAllByRole('button', { name: /Authorize/i })[0];
        fireEvent.click(authBtn);

        // Expect the authorization workspace to appear
        expect(screen.getByText(/Instrument Data \(Highlighting Deltas\)/i)).toBeInTheDocument();
        expect(screen.getByText(/Compliance Deck/i)).toBeInTheDocument();
        expect(screen.getAllByText(/Authorize/i).length).toBeGreaterThan(0);
    });
});
