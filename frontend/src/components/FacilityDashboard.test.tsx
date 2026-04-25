import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { FacilityDashboard } from './FacilityDashboard';
import { tradeApi } from '../api/tradeApi';

// ABOUTME: Test suite for FacilityDashboard.
// ABOUTME: Verifies credit limit utilization and exposure visualization for v3.0.

jest.mock('../api/tradeApi');

const mockExposure = {
  totalLimit: 10000000,
  facilityBreakdown: [
    { 
      facilityId: 'FAC_001', 
      facilityName: 'Import LC Facility', 
      limit: 8000000, 
      firm: 5000000, 
      contingent: 1000000, 
      reserved: 500000,
      available: 1500000
    },
    { 
      facilityId: 'FAC_002', 
      facilityName: 'Bank Guarantee Facility', 
      limit: 2000000, 
      firm: 500000, 
      contingent: 500000, 
      reserved: 500000,
      available: 500000
    }
  ]
};

const mockDetail = {
  facility: mockExposure.facilityBreakdown[0],
  transactions: [
    {
      instrumentId: 'INST_001',
      transactionRef: 'LC240001',
      businessStateId: 'TX_APPROVED',
      effectiveOutstandingAmount: 2000000,
      transactionDate: '2024-03-01'
    }
  ]
};

describe('FacilityDashboard', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (tradeApi.getExposureData as jest.Mock).mockResolvedValue(mockExposure);
    (tradeApi.getFacilityDetail as jest.Mock).mockResolvedValue(mockDetail);
  });

  it('renders facility selector with options', async () => {
    render(<FacilityDashboard />);
    const selector = await screen.findByRole('combobox');
    expect(selector).toBeInTheDocument();
    expect(screen.getByText(/Import LC Facility/)).toBeInTheDocument();
    expect(screen.getByText(/Bank Guarantee Facility/)).toBeInTheDocument();
  });

  it('renders segmented exposure bar for selected facility', async () => {
    render(<FacilityDashboard />);
    expect(await screen.findByText('Facility Exposure Breakdown')).toBeInTheDocument();
    expect(screen.getByText(/Firm:/)).toBeInTheDocument();
    expect(screen.getByText(/\$5,000,000/)).toBeInTheDocument();
    expect(screen.getByText(/Contingent:/)).toBeInTheDocument();
    expect(screen.getByText(/\$1,000,000/)).toBeInTheDocument();
  });

  it('renders utilization breakdown table with transactions', async () => {
    render(<FacilityDashboard />);
    expect(await screen.findByText('Utilization Breakdown')).toBeInTheDocument();
    expect(await screen.findByText('LC240001')).toBeInTheDocument();
    expect(screen.getByText('$2,000,000')).toBeInTheDocument();
    expect(screen.getByText('TX_APPROVED')).toBeInTheDocument();
  });

  it('shows high utilization warning when applicable', async () => {
    (tradeApi.getFacilityDetail as jest.Mock).mockResolvedValue({
      ...mockDetail,
      facility: { ...mockDetail.facility, available: 500000 } // 93.75% util
    });
    
    render(<FacilityDashboard />);
    expect(await screen.findByText(/HIGH UTILIZATION/i)).toBeInTheDocument();
  });
});
