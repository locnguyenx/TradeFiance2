import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ShippingGuaranteeForm } from './ShippingGuaranteeForm';
import { tradeApi } from '../api/tradeApi';

jest.mock('../api/tradeApi', () => ({
    tradeApi: {
        getImportLc: jest.fn().mockResolvedValue({
            instrumentId: 'IMLC/2026/001',
            amount: 500000,
            currencyUomId: 'USD'
        })
    }
}));

// ABOUTME: Test suite for Shipping Guarantee indemnity request mapping to REQ-IMP-PRC-05.
// UI Traceability: REQ-UI-IMP-09 (Shipping Guarantee Form)

describe('ShippingGuaranteeForm (REQ-IMP-PRC-05)', () => {
    it('Captures transport document and invoice details for early release', async () => {
        render(<ShippingGuaranteeForm instrumentId="IMLC/2026/001" />);
        await waitFor(() => expect(screen.queryByText(/Loading LC Context/i)).not.toBeInTheDocument());
        
        fireEvent.change(screen.getByLabelText(/Invoice Amount/i), { target: { value: '120000' } });
        fireEvent.change(screen.getByLabelText(/Transport Document Ref/i), { target: { value: 'BL-99218' } });
        
        expect(screen.getByDisplayValue('BL-99218')).toBeInTheDocument();
    });

    it('Calculates the 110% over-collateralized earmark amount', async () => {
        render(<ShippingGuaranteeForm instrumentId="IMLC/2026/001" />);
        await waitFor(() => expect(screen.queryByText(/Loading LC Context/i)).not.toBeInTheDocument());
        
        fireEvent.change(screen.getByLabelText(/Invoice Amount/i), { target: { value: '100000' } });
        
        // 110% of 100,000 = 110,000
        const earmark = screen.getByTestId('earmark-value');
        expect(earmark.textContent).toMatch(/\$ \s*110,000/i);
    });

    it('Validates against negative impact to global facility availability', async () => {
        render(<ShippingGuaranteeForm instrumentId="IMLC/2026/001" />);
        await waitFor(() => expect(screen.queryByText(/Loading LC Context/i)).not.toBeInTheDocument());
        expect(screen.getByText(/Available Facility/i)).toBeInTheDocument();
        expect(screen.getByText(/\$ \s*1,000,000/i)).toBeInTheDocument();
    });
});
