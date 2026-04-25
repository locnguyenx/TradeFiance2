import { render, screen, waitFor } from '@testing-library/react';
import { ImportLcDashboard } from './ImportLcDashboard';
import { FacilityDashboard } from './FacilityDashboard';
import { tradeApi } from '../api/tradeApi';

// ABOUTME: Resilience tests ensuring UI stability against malformed or empty API responses.
// Traceability: BDD-CMN-RES-01 (System stability under partial data failure)

jest.mock('../api/tradeApi', () => ({
    tradeApi: {
        getImportLcs: jest.fn(),
        getKpis: jest.fn(),
        getExposureData: jest.fn()
    }
}));

describe('Dashboard Resilience (Empty/Malformed API)', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('ImportLcDashboard handles empty lcList without crashing', async () => {
        (tradeApi.getImportLcs as jest.Mock).mockResolvedValue({}); // Empty object instead of {lcList: []}
        (tradeApi.getKpis as jest.Mock).mockResolvedValue({});

        render(<ImportLcDashboard />);
        
        await waitFor(() => {
            expect(screen.getByText(/No active transactions found/i)).toBeInTheDocument();
        });
    });

    it('FacilityDashboard handles missing exposure metrics without crashing', async () => {
        (tradeApi.getExposureData as jest.Mock).mockResolvedValue({}); 

        render(<FacilityDashboard />);
        
        await waitFor(() => {
            expect(screen.getByText(/Facility Exposure Breakdown/i)).toBeInTheDocument();
            const zeros = screen.getAllByText(/\$0/);
            expect(zeros.length).toBeGreaterThan(0);
        });
    });

    it('FacilityDashboard handles null exposure data', async () => {
        (tradeApi.getExposureData as jest.Mock).mockResolvedValue(null);

        render(<FacilityDashboard />);
        
        await waitFor(() => {
            expect(screen.getByText(/Facility Exposure Breakdown/i)).toBeInTheDocument();
            const zeros = screen.getAllByText(/\$0/);
            expect(zeros.length).toBeGreaterThan(0);
        });
    });
});
