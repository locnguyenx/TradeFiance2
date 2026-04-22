import { render, screen } from '@testing-library/react';
import { DocumentExamination } from './DocumentExamination';

describe('DocumentExamination Split-Screen Context', () => {
    it('executes baseline rendering of digital document viewer and discrepancy checklist', () => {
        render(<DocumentExamination />);
        expect(screen.getByText(/Digital Document Viewer/i)).toBeInTheDocument();
        expect(screen.getByText(/Discrepancy Reporting Checklist/i)).toBeInTheDocument();
        expect(screen.getByText(/MT700 Logic Check/i)).toBeInTheDocument();
    });
});
