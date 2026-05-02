import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { PartyDirectory } from './PartyDirectory';
import { tradeApi } from '../api/tradeApi';
import { TradeParty } from '../api/types';

// ABOUTME: Test suite for PartyDirectory.
// ABOUTME: Verifies KYC and Sanctions status visualization for v3.0.

jest.mock('../api/tradeApi');

const mockParties: TradeParty[] = [
  {
    partyId: 'CORP_BETA',
    partyName: 'Beta Corp Ltd',
    partyTypeEnumId: 'PARTY_COMMERCIAL',
    kycStatus: 'Active',
    sanctionsStatus: 'SANCTION_CLEAR',
    lastKycUpdate: '2026-01-15',
    riskRating: 'LOW',
    registeredAddress: '123 Beta St',
    accountNumber: 'BETA-001',
    countryOfRisk: 'USA'
  }
];

describe('PartyDirectory', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (tradeApi.getParties as jest.Mock).mockResolvedValue({ partyList: mockParties });
    (tradeApi.getParty as jest.Mock).mockResolvedValue(mockParties[0]);
  });

  it('renders party list', async () => {
    render(<PartyDirectory />);
    expect((await screen.findAllByText('Beta Corp Ltd')).length).toBeGreaterThan(0);
  });

  it('displays KYC and Sanctions status for selected party', async () => {
    render(<PartyDirectory />);
    await waitFor(() => {
      expect(screen.queryByText(/loading/i)).not.toBeInTheDocument();
    });
    const partyItems = screen.getAllByText('Beta Corp Ltd');
    fireEvent.click(partyItems[0]); // Click list item
    
    await waitFor(() => {
      expect(screen.getByText(/kyc status/i)).toBeInTheDocument();
    });
    expect(screen.getAllByText(/ACTIVE/i).length).toBeGreaterThan(0);
    expect(screen.getByText('123 Beta St')).toBeInTheDocument();
  });

  it('opens edit modal when clicking Edit Profile', async () => {
    render(<PartyDirectory />);
    await waitFor(() => {
        const partyItems = screen.getAllByText('Beta Corp Ltd');
        fireEvent.click(partyItems[0]);
    });
    
    const editBtn = await screen.findByText('Edit Profile');
    fireEvent.click(editBtn);
    
    // Check if modal title appears (it's inside PartyModal)
    await waitFor(() => {
        expect(screen.getByText('Edit Counterparty')).toBeInTheDocument();
    });
  });

  it('filters list when searching', async () => {
    render(<PartyDirectory />);
    const searchInput = await screen.findByPlaceholderText(/search parties/i);
    fireEvent.change(searchInput, { target: { value: 'Delta' } });
    
    await waitFor(() => {
      expect(tradeApi.getParties).toHaveBeenCalledWith('Delta');
    });
  });
});
