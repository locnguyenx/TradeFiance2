import { render, screen } from '@testing-library/react';
import { TariffConfiguration } from './TariffConfiguration';

// ABOUTME: Test suite for Tariff & Fee Configuration mapping to REQ-UI-CMN-05.
// UI Traceability: REQ-UI-CMN-05

describe('TariffConfiguration (REQ-UI-CMN-05)', () => {
    it('Displays global tariff rules and tier-based fee structures', () => {
        render(<TariffConfiguration />);
        expect(screen.getByText(/Base Rule Set/i)).toBeInTheDocument();
        expect(screen.getAllByText(/Issuance Commission/i).length).toBeGreaterThan(0);
        // Check for specific tier fields
        expect(screen.getByText(/Exception \/ Tier Pricing Grid/i)).toBeInTheDocument();
    });

    it('Allows configuration of MT610 SWIFT charge codes', () => {
        render(<TariffConfiguration />);
        expect(screen.getByText(/MT610 Charge Mapping/i)).toBeInTheDocument();
        expect(screen.getByText(/Claiming Bank Fees/i)).toBeInTheDocument();
    });

    it('Shows Maker/Checker approval status for pending tariff changes', () => {
        render(<TariffConfiguration />);
        expect(screen.getByText(/Pending Approval/i)).toBeInTheDocument();
        expect(screen.getByText(/Effective Date/i)).toBeInTheDocument();
    });
});
