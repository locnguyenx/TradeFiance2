'use client';

import React from 'react';
import { LimitsDashboard } from '../../components/LimitsDashboard';

export default function FacilitiesPage() {
  return (
    <div style={{ padding: '2rem' }}>
      <h1 style={{ marginBottom: '2rem', fontSize: '1.875rem', fontWeight: 600 }}>Credit Facilities</h1>
      <LimitsDashboard />
    </div>
  );
}
