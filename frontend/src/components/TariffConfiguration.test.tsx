import { render, screen } from '@testing-library/react';
import { TariffConfiguration } from './TariffConfiguration';

describe('TariffConfiguration Rules Grid', () => {
    it('executes baseline rule rendering displaying default fees and minimums', () => {
        render(<TariffConfiguration />);
        expect(screen.getAllByText('Issuance Commission').length).toBeGreaterThan(0);
        expect(screen.getByText('Base Rule Set')).toBeInTheDocument();
        expect(screen.getByDisplayValue('0.125')).toBeInTheDocument();
        expect(screen.getByText('Exception / Tier Pricing Grid')).toBeInTheDocument();
    });
});
