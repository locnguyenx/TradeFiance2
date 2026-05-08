import { render, screen, fireEvent } from '@testing-library/react';
import { RecordList } from './RecordList';

// ABOUTME: Test suite for RecordList component, verifying generic portfolio browsing functionality.
// UI Traceability: REQ-UI-CMN-01 (Generic Record Browsing)

describe('RecordList', () => {
    const mockRecords = [
        { id: '1', name: 'Record One', status: 'Active' },
        { id: '2', name: 'Record Two', status: 'Pending' }
    ];

    const columns = [
        { key: 'id', label: 'ID' },
        { key: 'name', label: 'Name' },
        { key: 'status', label: 'Status' }
    ];

    it('renders the title and description', () => {
        render(
            <RecordList 
                title="Test Portfolio" 
                description="Test Description" 
                records={mockRecords} 
                columns={columns} 
            />
        );
        expect(screen.getByText('Test Portfolio')).toBeInTheDocument();
        expect(screen.getByText('Test Description')).toBeInTheDocument();
    });

    it('renders all table headers correctly', () => {
        render(
            <RecordList 
                title="Test" 
                records={mockRecords} 
                columns={columns} 
            />
        );
        expect(screen.getByText('ID')).toBeInTheDocument();
        expect(screen.getByText('Name')).toBeInTheDocument();
        expect(screen.getByText('Status')).toBeInTheDocument();
    });

    it('renders record data correctly', () => {
        render(
            <RecordList 
                title="Test" 
                records={mockRecords} 
                columns={columns} 
            />
        );
        expect(screen.getByText('Record One')).toBeInTheDocument();
        expect(screen.getByText('Active')).toBeInTheDocument();
        expect(screen.getByText('Record Two')).toBeInTheDocument();
        expect(screen.getByText('Pending')).toBeInTheDocument();
    });

    it('displays loading state correctly', () => {
        render(
            <RecordList 
                title="Test" 
                records={[]} 
                columns={columns} 
                loading={true}
            />
        );
        expect(screen.getByText(/Loading Portfolio Records/i)).toBeInTheDocument();
    });

    it('displays empty state when no records are provided', () => {
        render(
            <RecordList 
                title="Test" 
                records={[]} 
                columns={columns} 
                loading={false}
            />
        );
        expect(screen.getByText(/No records found/i)).toBeInTheDocument();
    });

    it('triggers onRowClick when a row is clicked', () => {
        const onRowClick = jest.fn();
        render(
            <RecordList 
                title="Test" 
                records={mockRecords} 
                columns={columns} 
                onRowClick={onRowClick}
            />
        );
        
        fireEvent.click(screen.getByText('Record One'));
        expect(onRowClick).toHaveBeenCalledWith(mockRecords[0]);
    });
});
