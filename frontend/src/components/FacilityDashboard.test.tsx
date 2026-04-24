import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { FacilityDashboard } from './FacilityDashboard';
import { tradeApi } from '../api/tradeApi';

// ABOUTME: Test suite for FacilityDashboard.
// ABOUTME: Verifies credit limit utilization and exposure visualization for v3.0.

jest.mock('../api/tradeApi');

const mockExposure = {
  totalLimit: 10000000,
  totalExposure: 6500000,
  utilizationPercent: 65,
  facilityBreakdown: [
    { facilityId: 'FAC_001', facilityName: 'Import LC Facility', limit: 8000000, exposure: 5000000 },
    { facilityId: 'FAC_002', facilityName: 'Bank Guarantee Facility', limit: 2000000, exposure: 1500000 }
  ]
};

describe('FacilityDashboard', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (tradeApi.getExposureData as jest.Mock).mockResolvedValue(mockExposure);
  });

  it('renders total exposure summary', async () => {
    render(<FacilityDashboard />);
    expect(await screen.findByText(/6,500,000/)).toBeInTheDocument();
    expect(screen.getByText(/65%/)).toBeInTheDocument();
  });

  it('renders facility breakdown list', async () => {
    render(<FacilityDashboard />);
    expect(await screen.findByText('Import LC Facility')).toBeInTheDocument();
    expect(screen.getByText('Bank Guarantee Facility')).toBeInTheDocument();
  });

  it('shows warning when utilization is high', async () => {
    (tradeApi.getExposureData as jest.Mock).mockResolvedValue({
      ...mockExposure,
      utilizationPercent: 95
    });
    
    render(<FacilityDashboard />);
    expect(await screen.findByText(/HIGH UTILIZATION/i)).toBeInTheDocument();
  });
});
