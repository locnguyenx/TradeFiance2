'use client';

import React, { use } from 'react';
import { useSearchParams } from 'next/navigation';
import { TransactionDetails } from '../../../components/TransactionDetails';

export default function TransactionDetailPage() {
  const searchParams = useSearchParams();
  const transactionId = searchParams.get('id');

  if (!transactionId) {
    return <div className="p-12 text-center text-red-500 font-bold">Error: No Transaction ID provided.</div>;
  }

  return (
    <div style={{ padding: '2rem' }}>
      <TransactionDetails transactionId={transactionId} />
    </div>
  );
}
