import { render, screen, waitFor } from '@testing-library/react';
import { SystemAdminSettings } from './SystemAdminSettings';
import { tradeApi } from '../api/tradeApi';

jest.mock('../api/tradeApi', () => ({
    tradeApi: {
        getAuditLogs: jest.fn().mockResolvedValue([]),
        getProductCatalog: jest.fn().mockResolvedValue({ 
            productList: [{ 
                productCatalogId: 'IMLC', 
                productName: 'Import LC',
                allowRevolving: 'Y',
                mandatoryMarginPercent: 10,
                maxToleranceLimit: 5,
                documentExamSlaDays: 5
            }] 
        }),
        getFeeConfigurations: jest.fn().mockResolvedValue({ feeList: [] }),
        getUserAuthorityProfiles: jest.fn().mockResolvedValue({ profileList: [] })
    }
}));

describe('SystemAdminSettings Sub-menu Render', () => {
    it('generates specific panels for User Authorities, Audit, and Product Config', async () => {
        render(<SystemAdminSettings />);
        expect(screen.getByText('User Authority Management')).toBeInTheDocument();
        expect(screen.getByText('System Audit Logs (Delta JSON)')).toBeInTheDocument();
        await waitFor(() => {
            expect(screen.getByText(/Import LC Configuration/i)).toBeInTheDocument();
        });
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

    it('Renders the Trade Product Configuration Matrix (REQ-COM-PRD-01)', async () => {
        render(<SystemAdminSettings />);
        await waitFor(() => {
            expect(screen.getByLabelText(/Allow Revolving/i)).toBeInTheDocument();
            expect(screen.getByLabelText(/Mandatory Margin/i)).toBeInTheDocument();
            expect(screen.getByText(/Publish New Product/i)).toBeInTheDocument();
        });
    });
});
