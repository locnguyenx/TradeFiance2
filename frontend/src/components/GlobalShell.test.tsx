import { render, screen } from '@testing-library/react';
import { GlobalShell } from './GlobalShell';

describe('GlobalShell Modern Navigation Layout', () => {
    it('renders high-density minimalist navigation matching REQ-UI-MOD-01', () => {
        render(<GlobalShell><div>Content</div></GlobalShell>);
        
        // Brand
        expect(screen.getByText('TRADEFINANCE')).toBeInTheDocument();
        
        // Sections
        expect(screen.getByText('Workspace')).toBeInTheDocument();
        expect(screen.getByText('Import LC Module')).toBeInTheDocument();
        expect(screen.getByText('Master Data')).toBeInTheDocument();
        
        // Items
        expect(screen.getByText('Operations Dashboard')).toBeInTheDocument();
        expect(screen.getByText('New LC Issuance')).toBeInTheDocument();
        expect(screen.getByText('Party Directory')).toBeInTheDocument();
        
        // User Profile
        expect(screen.getByText('Loc Nguyen')).toBeInTheDocument();
    });
});
