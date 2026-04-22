import { render, screen } from '@testing-library/react';
import { SystemAdminSettings } from './SystemAdminSettings';

describe('SystemAdminSettings Sub-menu Render', () => {
    it('generates specific panels for User Authorities, Audit, and Product Config', () => {
        render(<SystemAdminSettings />);
        expect(screen.getByText('User Authority Management')).toBeInTheDocument();
        expect(screen.getByText('System Audit Logs (Delta JSON)')).toBeInTheDocument();
        expect(screen.getByText('Trade Product Configuration Matrix')).toBeInTheDocument();
    });
});
