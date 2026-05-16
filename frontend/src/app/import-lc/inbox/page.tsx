'use client';

import React from 'react';
import dynamic from 'next/dynamic';
import { Mail } from 'lucide-react';

const TradeInbox = dynamic(
  () => import('../../../components/TradeInbox').then(mod => mod.TradeInbox),
  { ssr: false }
);

export default function InboxPage() {
  return (
    <div style={{ padding: '2rem' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '2rem' }}>
        <div style={{ 
          backgroundColor: '#2563eb', 
          color: 'white', 
          padding: '0.75rem', 
          borderRadius: '0.75rem',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center'
        }}>
          <Mail size={24} />
        </div>
        <div>
          <h1 style={{ margin: 0, fontSize: '1.875rem', fontWeight: 600 }}>Trade Inbox</h1>
          <p style={{ margin: 0, color: '#94a3b8', fontSize: '0.875rem' }}>
            Process inbound SWIFT messages and correlate orphans to instruments.
          </p>
        </div>
      </div>
      
      <TradeInbox />
    </div>
  );
}
