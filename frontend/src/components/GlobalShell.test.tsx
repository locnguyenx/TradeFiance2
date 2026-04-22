import { render, screen } from '@testing-library/react';
import { GlobalShell } from './GlobalShell';

describe('GlobalShell Complete Navigation Layout', () => {
    it('renders the complete lateral navigation menus matching REQ-UI-CMN-01', () => {
        render(<GlobalShell><div>Content</div></GlobalShell>);
        // Primary
        expect(screen.getByText('Dashboard')).toBeInTheDocument();
        expect(screen.getByText('My Approvals')).toBeInTheDocument();
        
        // Modules
        expect(screen.getByText('Import LC')).toBeInTheDocument();
        expect(screen.getByText('Export LC (Phase 2)')).toBeInTheDocument();
        
        // Master Data
        expect(screen.getByText('Party & KYC Directory')).toBeInTheDocument();
        expect(screen.getByText('Credit Facilities')).toBeInTheDocument();
        expect(screen.getByText('Tariff & Fee Configuration')).toBeInTheDocument();
        expect(screen.getByText('Product Configuration')).toBeInTheDocument();
        
        // System Admin
        expect(screen.getByText('System Admin')).toBeInTheDocument();
        expect(screen.getByText('User Authority Tiers')).toBeInTheDocument();
        expect(screen.getByText('Audit Logs')).toBeInTheDocument();
    });
});
