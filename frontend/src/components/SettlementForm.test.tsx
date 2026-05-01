import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { SettlementForm } from './SettlementForm';
import { tradeApi } from '../api/tradeApi';

// ABOUTME: Test suite for SettlementForm.
// ABOUTME: Verifies drawing logic and balance tracking for v3.0.

jest.mock('../api/tradeApi');

const mockLc = {
  instrumentId: 'LC_100',
  effectiveOutstandingAmount: 50000,
  currencyUomId: 'USD',
  beneficiaryPartyName: 'Global Supplier'
};

describe('SettlementForm', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (tradeApi.getImportLc as jest.Mock).mockResolvedValue(mockLc);
  });

  it('renders LC details and outstanding balance', async () => {
    await act(async () => {
      render(<SettlementForm instrumentId="LC_100" />);
    });
    expect(await screen.findByText(/Global Supplier/)).toBeInTheDocument();
    expect(screen.getAllByText(/50,000/).length).toBeGreaterThan(0);
  });

  it('calculates remaining balance after drawing amount is entered', async () => {
    await act(async () => {
      render(<SettlementForm instrumentId="LC_100" />);
    });
    const amountInput = await screen.findByLabelText(/drawing amount/i);
    fireEvent.change(amountInput, { target: { value: '10000' } });
    
    expect(await screen.findByText(/40,000/)).toBeInTheDocument();
  });

  it('submits settlement when confirmed', async () => {
    (tradeApi.settleLcPresentation as jest.Mock).mockResolvedValue({ success: true });
    
    await act(async () => {
      render(<SettlementForm instrumentId="LC_100" />);
    });
    const amountInput = await screen.findByLabelText(/drawing amount/i);
    fireEvent.change(amountInput, { target: { value: '10000' } });
    
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /confirm settlement/i }));
    });
    
    expect(tradeApi.settleLcPresentation).toHaveBeenCalledWith('LC_100', expect.any(String), expect.objectContaining({
      principalAmount: 10000
    }));
  });
});
