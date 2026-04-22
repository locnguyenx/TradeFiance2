'use client';

import React from 'react';
import { TariffConfiguration } from '../../components/TariffConfiguration';

export default function TariffsPage() {
  return (
    <div style={{ padding: '2rem' }}>
      <h1 style={{ marginBottom: '2rem', fontSize: '1.875rem', fontWeight: 600 }}>Tariff & Fee Mapping</h1>
      <TariffConfiguration />
    </div>
  );
}
