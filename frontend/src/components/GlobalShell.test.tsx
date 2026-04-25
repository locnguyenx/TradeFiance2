import { render, screen } from '@testing-library/react';
import { GlobalShell } from './GlobalShell';

jest.mock('next/navigation', () => ({
    usePathname: () => '/import-lc',
}));

describe('GlobalShell Modern Navigation Layout', () => {
    it('renders high-density minimalist navigation matching REQ-UI-MOD-01', () => {
        render(<GlobalShell><div>Content</div></GlobalShell>);
        
        // Brand
        expect(screen.getByText('TRADEFINANCE')).toBeInTheDocument();
        
        // Sections
        expect(screen.getByText('OPERATIONS')).toBeInTheDocument();
        expect(screen.getByText('LIFECYCLE MANAGEMENT')).toBeInTheDocument();
        expect(screen.getByText('MASTER DATA')).toBeInTheDocument();
        expect(screen.getByText('ADMINISTRATION')).toBeInTheDocument();
        
        // Items
        expect(screen.getByText('Dashboard')).toBeInTheDocument();
        expect(screen.getByText('New LC Issuance')).toBeInTheDocument();
        expect(screen.getByText('Party & KYC Directory')).toBeInTheDocument();
        expect(screen.getByText('Credit Facilities (Limits)')).toBeInTheDocument();
        expect(screen.getByText('User Authority Tiers')).toBeInTheDocument();
        
        // User Profile
        expect(screen.getByText('Loc Nguyen')).toBeInTheDocument();
    });
});
