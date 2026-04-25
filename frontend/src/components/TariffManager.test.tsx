import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { TariffManager } from './TariffManager';
import { tradeApi } from '../api/tradeApi';
import { FeeConfiguration } from '../api/types';

// ABOUTME: Test suite for TariffManager.
// ABOUTME: Verifies fee configuration matrix and updates for v3.0.

jest.mock('../api/tradeApi');

const mockFees: FeeConfiguration[] = [
  {
    feeConfigurationId: 'FEE_ISS_01',
    feeEventEnumId: 'ISSUANCE_FEE',
    calculationTypeEnumId: 'PERCENTAGE',
    ratePercent: 0.125,
    minFloorAmount: 50,
    maxCeilingAmount: 500,
    currencyUomId: 'USD',
    isActive: 'Y'
  }
];

describe('TariffManager', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (tradeApi.getFeeConfigurations as jest.Mock).mockResolvedValue({ feeList: mockFees });
  });

  it('renders fee type list in left navigation', async () => {
    render(<TariffManager />);
    expect(await screen.findByText('Issuance Fee')).toBeInTheDocument();
  });

  it('displays floor and ceiling configuration for selected fee', async () => {
    render(<TariffManager />);
    const feeItem = await screen.findByText('Issuance Fee');
    fireEvent.click(feeItem);
    
    expect(await screen.findByLabelText(/minimum charge/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/maximum charge/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/rate %/i)).toBeInTheDocument();
  });

  it('submits updates when Publish is clicked', async () => {
    (tradeApi.updateFeeConfiguration as jest.Mock).mockResolvedValue({ success: true });
    
    render(<TariffManager />);
    const feeItem = await screen.findByText('Issuance Fee');
    fireEvent.click(feeItem);

    const rateInput = await screen.findByLabelText(/rate %/i);
    fireEvent.change(rateInput, { target: { value: '0.15' } });
    
    fireEvent.click(screen.getByRole('button', { name: /publish/i }));
    
    expect(tradeApi.updateFeeConfiguration).toHaveBeenCalledWith('FEE_ISS_01', expect.objectContaining({
      ratePercent: 0.15
    }));
  });
});
