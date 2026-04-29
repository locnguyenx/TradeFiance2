import React from 'react';
import { TransactionDashboard } from '../../components/TransactionDashboard';

export default async function TransactionsPage() {
  return (
    <div style={{ padding: '2rem' }}>
      <TransactionDashboard />
    </div>
  );
}
