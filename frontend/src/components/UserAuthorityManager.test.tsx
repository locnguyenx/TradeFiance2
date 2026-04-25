import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { UserAuthorityManager } from './UserAuthorityManager';
import { tradeApi } from '../api/tradeApi';
import { UserAuthorityProfile } from '../api/types';

// ABOUTME: Test suite for UserAuthorityManager.
// ABOUTME: Verifies user delegation tiers and approval limits management.

jest.mock('../api/tradeApi');

const mockProfiles: UserAuthorityProfile[] = [
  {
    userAuthorityId: 'AUTH_001',
    userId: 'CHECKER_ALPHA',
    delegationTierId: 'TIER_3',
    customLimit: 5000000,
    currencyUomId: 'USD',
    isSuspended: 'N'
  }
];

describe('UserAuthorityManager', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (tradeApi.getUserAuthorityProfiles as jest.Mock).mockResolvedValue({ profileList: mockProfiles });
  });

  it('renders user list', async () => {
    render(<UserAuthorityManager />);
    expect(await screen.findByText('CHECKER_ALPHA')).toBeInTheDocument();
  });

  it('displays tier and limit configuration for selected user', async () => {
    render(<UserAuthorityManager />);
    const userItem = await screen.findByText('CHECKER_ALPHA');
    fireEvent.click(userItem);
    
    expect(await screen.findByLabelText(/authority tier/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/max approval limit/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/account suspended/i)).toBeInTheDocument();
  });

  it('submits updates when Save is clicked', async () => {
    (tradeApi.updateUserAuthorityProfile as jest.Mock).mockResolvedValue({ success: true });
    
    render(<UserAuthorityManager />);
    const userItem = await screen.findByText('CHECKER_ALPHA');
    fireEvent.click(userItem);

    const limitInput = await screen.findByLabelText(/max approval limit/i);
    fireEvent.change(limitInput, { target: { value: '6000000' } });
    
    fireEvent.click(screen.getByRole('button', { name: /save changes/i }));
    
    expect(tradeApi.updateUserAuthorityProfile).toHaveBeenCalledWith('AUTH_001', expect.objectContaining({
      customLimit: 6000000
    }));
  });
});
