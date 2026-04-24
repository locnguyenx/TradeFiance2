'use client';

import { SettlementForm } from '../../../components/SettlementForm';
import { GlobalShell } from '../../../components/GlobalShell';
import { useSearchParams } from 'next/navigation';
import { Suspense } from 'react';

function SettlementContent() {
  const searchParams = useSearchParams();
  const instrumentId = searchParams.get('instrumentId') || '';

  return (
    <>
      {instrumentId ? (
        <SettlementForm instrumentId={instrumentId} />
      ) : (
        <div className="p-8 text-center text-gray-500 italic">
          Please select an LC from the dashboard to initiate settlement.
        </div>
      )}
    </>
  );
}

export default function SettlementPage() {
  return (
    <GlobalShell>
      <Suspense fallback={<div className="p-8 text-center">Loading Settlement Context...</div>}>
        <SettlementContent />
      </Suspense>
    </GlobalShell>
  );
}
