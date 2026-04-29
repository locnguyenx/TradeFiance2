import { render, screen, waitFor, act } from '@testing-library/react';
import { SystemAdminSettings } from './SystemAdminSettings';
import { tradeApi } from '../api/tradeApi';

jest.mock('../api/tradeApi', () => ({
    tradeApi: {
        getAuditLogs: jest.fn().mockResolvedValue([]),
        getGlobalAuditLogs: jest.fn().mockResolvedValue({ 
            auditLogList: [{
                auditId: '123',
                timestamp: new Date().toISOString(),
                userId: 'test.user',
                actionEnumId: 'AUDIT_UPDATE',
                snapshotDeltaJSON: '{}'
            }] 
        }),
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
        await act(async () => {
            render(<SystemAdminSettings />);
        });
        expect(screen.getByText('User Authority Management')).toBeInTheDocument();
        expect(screen.getByText('Global Transaction Log')).toBeInTheDocument();
        await waitFor(() => {
            expect(screen.getByText(/Import LC Configuration/i)).toBeInTheDocument();
        });
    });

    it('Renders the User Authority Tiers table (REQ-COM-AUTH)', async () => {
        await act(async () => {
            render(<SystemAdminSettings />);
        });
        expect(screen.getByText('Authority Tier')).toBeInTheDocument();
        expect(screen.getByText('Limit Threshold')).toBeInTheDocument();
        expect(screen.getByText('Tier 1 - Maker')).toBeInTheDocument();
    });

    it('Renders the Global Transaction Log component', async () => {
        await act(async () => {
            render(<SystemAdminSettings />);
        });
        expect(screen.getByText('Global Transaction Log')).toBeInTheDocument();
        expect(screen.getByText(/Delta Transformation/i)).toBeInTheDocument();
    });

    it('Renders the Trade Product Configuration Matrix (REQ-COM-PRD-01)', async () => {
        await act(async () => {
            render(<SystemAdminSettings />);
        });
        await waitFor(() => {
            expect(screen.getByLabelText(/Allow Revolving/i)).toBeInTheDocument();
            expect(screen.getByLabelText(/Mandatory Margin/i)).toBeInTheDocument();
            expect(screen.getByText(/Publish New Product/i)).toBeInTheDocument();
        });
    });
});
