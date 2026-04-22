import { render, screen } from '@testing-library/react';
import { SystemAdminSettings } from './SystemAdminSettings';

describe('SystemAdminSettings Sub-menu Render', () => {
    it('generates specific panels for User Authorities, Audit, and Product Config', () => {
        render(<SystemAdminSettings />);
        expect(screen.getByText('User Authority Management')).toBeInTheDocument();
        expect(screen.getByText('System Audit Logs (Delta JSON)')).toBeInTheDocument();
        expect(screen.getByText('Trade Product Configuration Matrix')).toBeInTheDocument();
    });

    it('Renders the User Authority Tiers table (REQ-COM-AUTH)', () => {
        render(<SystemAdminSettings />);
        expect(screen.getByText('Authority Tier')).toBeInTheDocument();
        expect(screen.getByText('Limit Threshold')).toBeInTheDocument();
        expect(screen.getByText('Tier 1 - Maker')).toBeInTheDocument();
    });

    it('Renders the System Audit Logs table (REQ-COM-MAS-03)', () => {
        render(<SystemAdminSettings />);
        expect(screen.getByText('Timestamp')).toBeInTheDocument();
        expect(screen.getByText('User ID')).toBeInTheDocument();
        expect(screen.getByText('Delta Payload')).toBeInTheDocument();
    });

    it('Renders the Trade Product Configuration Matrix (REQ-COM-PRD-01)', () => {
        render(<SystemAdminSettings />);
        expect(screen.getByLabelText(/Allow Revolving/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Mandatory Margin/i)).toBeInTheDocument();
        expect(screen.getByText(/Save Configuration/i)).toBeInTheDocument();
    });
});
