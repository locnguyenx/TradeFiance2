'use client';

import React from 'react';
import { IssuanceStepper } from '../../components/IssuanceStepper';
import Link from 'next/link';

export default function IssuancePage() {
  return (
    <div style={{ padding: '2rem', maxWidth: '800px', margin: '0 auto' }}>
      <Link href="/import-lc">
        <button style={{ marginBottom: '1rem', background: 'none', border: 'none', color: '#2563eb', cursor: 'pointer' }}>
          ← Back to Dashboard
        </button>
      </Link>
      <IssuanceStepper />
    </div>
  );
}
