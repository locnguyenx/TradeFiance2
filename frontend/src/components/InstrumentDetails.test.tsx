import { render, screen, waitFor } from '@testing-library/react';
import { InstrumentDetails } from './InstrumentDetails';
import { tradeApi } from '../api/tradeApi';

// ABOUTME: Test suite for InstrumentDetails component, verifying data integrity and field formatting.
// UI Traceability: REQ-UI-IMP-03 (Instrument Detail View)

jest.mock('../api/tradeApi');
jest.mock('./SwiftMessageViewer', () => ({
    SwiftMessageViewer: () => <div data-testid="swift-viewer-mock">SWIFT Viewer Mock</div>
}));

describe('InstrumentDetails', () => {
    const mockInstrument = {
        instrumentId: 'LC240003',
        instrumentRef: 'LC-24-0003',
        instrumentTypeEnumId: 'INST_IMPORT_LC',
        amount: 4800000,
        currencyUomId: 'USD',
        issueDate: 1776816000000, // Apr 13, 2026
        effectiveExpiryDate: 1780012800000, // May 20, 2026
        businessStateId: 'LC_ISSUED',
        applicantPartyId: 'PARTY_TEST',
        applicantPartyName: 'Test Party',
        beneficiaryPartyId: 'ACME_CORP_001',
        beneficiaryPartyName: 'Acme Corporation Ltd',
        parties: [
            { roleEnumId: 'TP_APPLICANT', partyName: 'Test Party', partyId: 'PARTY_TEST' },
            { roleEnumId: 'TP_BENEFICIARY', partyName: 'Acme Corporation Ltd', partyId: 'ACME_CORP_001' }
        ]
    };

    beforeEach(() => {
        jest.clearAllMocks();
        (tradeApi.getImportLc as jest.Mock).mockResolvedValue(mockInstrument);
        (tradeApi.getSwiftMessages as jest.Mock).mockResolvedValue({ messageList: [] });
    });

    it('renders basic instrument information correctly', async () => {
        render(<InstrumentDetails instrument={mockInstrument as any} />);
        
        expect(screen.getByText('LC-24-0003')).toBeInTheDocument();
        expect(screen.getByText('4,800,000.00')).toBeInTheDocument();
        expect(screen.getByText('USD')).toBeInTheDocument();
    });

    it('formats numeric timestamps into human-readable dates', async () => {
        render(<InstrumentDetails instrument={mockInstrument as any} />);
        
        // Verify Issue Date (1776816000000) - Flexible for locale differences
        expect(screen.getByText(/Apr|13.*2026/i)).toBeInTheDocument();
        // Verify Expiry Date (1780012800000)
        expect(screen.getByText(/May|20.*2026/i)).toBeInTheDocument();
    });

    it('displays "---" for missing party details (Data Integrity Fix)', async () => {
        render(<InstrumentDetails instrument={mockInstrument as any} />);
        
        // Advising Bank and Issuing Bank are not in mockInstrument.parties
        // The labels are present, but values should be '---'
        const advisingBankLabel = screen.getByText(/Advising Bank \(Receiver\)/i);
        const parentRow = advisingBankLabel.closest('.detail-row');
        expect(parentRow).toHaveTextContent('---');
    });

    it('correctly maps party names from the parties array', async () => {
        render(<InstrumentDetails instrument={mockInstrument as any} />);
        
        expect(screen.getByText('Test Party')).toBeInTheDocument();
        expect(screen.getByText('Acme Corporation Ltd')).toBeInTheDocument();
    });
});
