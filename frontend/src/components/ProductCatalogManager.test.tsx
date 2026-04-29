import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { ProductCatalogManager } from './ProductCatalogManager';
import { tradeApi } from '../api/tradeApi';
import { TradeProductCatalog } from '../api/types';

// ABOUTME: Test suite for ProductCatalogManager.
// ABOUTME: Verifies Master-Detail layout and configuration updates for v3.0.

jest.mock('../api/tradeApi');

const mockProducts: TradeProductCatalog[] = [
  {
    productId: 'IMP_LC_SIGHT',
    productName: 'Sight Import LC',
    isActive: 'Y',
    allowedTenorEnumId: 'SIGHT',
    maxToleranceLimit: 10,
    allowRevolving: 'N',
    allowAdvancePayment: 'N',
    isStandby: 'N',
    isTransferable: 'N',
    accountingFrameworkEnumId: 'STANDARD',
    mandatoryMarginPercent: 10,
    documentExamSlaDays: 5,
    defaultSwiftFormatEnumId: 'MT700'
  }
];

describe('ProductCatalogManager', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (tradeApi.getProductCatalog as jest.Mock).mockResolvedValue({ productList: mockProducts });
  });

  it('renders product list in left navigation', async () => {
    await act(async () => {
      render(<ProductCatalogManager />);
    });
    await waitFor(() => {
      expect(screen.getByText('Sight Import LC')).toBeInTheDocument();
    });
  });

  it('displays config form when a product is selected', async () => {
    await act(async () => {
      render(<ProductCatalogManager />);
    });
    await waitFor(() => {
      fireEvent.click(screen.getByText('Sight Import LC'));
    });
    
    expect(screen.getByLabelText(/allow revolving/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/mandatory margin %/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/document exam sla days/i)).toBeInTheDocument();
  });

  it('submits updates when Save Draft is clicked', async () => {
    (tradeApi.updateProductCatalog as jest.Mock).mockResolvedValue({ success: true });
    
    await act(async () => {
      render(<ProductCatalogManager />);
    });
    await waitFor(() => {
      fireEvent.click(screen.getByText('Sight Import LC'));
    });

    const revolvingToggle = screen.getByLabelText(/allow revolving/i);
    fireEvent.click(revolvingToggle);
    
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /save draft/i }));
    });
    
    expect(tradeApi.updateProductCatalog).toHaveBeenCalledWith('IMP_LC_SIGHT', expect.objectContaining({
      allowRevolving: 'Y'
    }));
  });
});
