'use client';

import { SettlementForm } from '../../../components/SettlementForm';
import { GlobalShell } from '../../../components/GlobalShell';
import { useSearchParams } from 'next/navigation';
import { Suspense } from 'react';

function SettlementContent() {
  const searchParams = useSearchParams();
  const instrumentId = searchParams.get('id') || searchParams.get('instrumentId') || '';

  return (
    <div className="p-8">
      <h2 className="text-2xl font-bold mb-6">Initiate LC Settlement</h2>
      {instrumentId ? (
        <SettlementForm instrumentId={instrumentId} />
      ) : (
        <div className="bg-gray-50 border-2 border-dashed border-gray-200 rounded-lg p-12 text-center text-gray-500 italic">
          Please select an LC from the dashboard to initiate settlement.
        </div>
      )}
    </div>
  );
}

export default function SettlementPage() {
  return (
    <Suspense fallback={<div className="p-8 text-center">Loading Settlement Context...</div>}>
      <SettlementContent />
    </Suspense>
  );
}
