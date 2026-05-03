import { render, screen } from '@testing-library/react';
import { GlobalShell } from './GlobalShell';
import { ToastProvider } from '../context/ToastContext';

jest.mock('next/navigation', () => ({
    usePathname: () => '/import-lc',
    useRouter: () => ({
        push: jest.fn(),
    }),
}));

jest.mock('../context/AuthContext', () => ({
    useAuth: () => ({
        user: {
            userId: '10001',
            firstName: 'Test',
            lastName: 'User',
            roles: ['TRADE_MAKER']
        },
        loading: false,
        isAuthenticated: true,
        logout: jest.fn(),
        hasRole: (role: string) => role === 'TRADE_MAKER',
    }),
}));

describe('GlobalShell Modern Navigation Layout', () => {
    it('renders high-density minimalist navigation with authenticated user', () => {
        render(
            <ToastProvider>
                <GlobalShell><div>Content</div></GlobalShell>
            </ToastProvider>
        );
        
        // Brand
        expect(screen.getByText('TRADEFINANCE')).toBeInTheDocument();
        
        // Sections
        expect(screen.getByText('OPERATIONS')).toBeInTheDocument();
        expect(screen.getByText('IMPORT LC')).toBeInTheDocument();
        
        // Items
        expect(screen.getByText('Operations Dashboard')).toBeInTheDocument();
        expect(screen.getByText('Import LC Dashboard')).toBeInTheDocument();
        
        // User Profile (Dynamic)
        expect(screen.getByText('Test User')).toBeInTheDocument();
        expect(screen.getByText('MAKER')).toBeInTheDocument(); // TRADE_MAKER -> MAKER
        expect(screen.getByTitle('Logout')).toBeInTheDocument();
    });
});
