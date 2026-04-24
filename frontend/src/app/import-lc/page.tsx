'use client';

import React from 'react';
import dynamic from 'next/dynamic';
import Link from 'next/link';

const ImportLcDashboard = dynamic(
  () => import('../../components/ImportLcDashboard').then(mod => mod.ImportLcDashboard),
  { ssr: false }
);

export default function DashboardPage() {
  return (
    <div style={{ padding: '2rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
        <h1 style={{ margin: 0, fontSize: '1.875rem', fontWeight: 600 }}>Import LC Dashboard</h1>
        <Link href="/issuance">
          <button 
            style={{ 
              backgroundColor: '#2563eb', 
              color: 'white', 
              padding: '0.625rem 1.25rem', 
              borderRadius: '0.5rem',
              border: 'none',
              fontWeight: 500,
              cursor: 'pointer'
            }}
          >
            New Issuance
          </button>
        </Link>
      </div>
      <ImportLcDashboard />
    </div>
  );
}
