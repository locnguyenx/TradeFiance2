'use client';

import React from 'react';
import { PartyDirectory } from '../../components/PartyDirectory';

export default function PartiesPage() {
  return (
    <div style={{ padding: '2rem' }}>
      <h1>Party Directory & KYC Management</h1>
      <PartyDirectory />
    </div>
  );
}
