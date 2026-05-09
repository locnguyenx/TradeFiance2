'use client';

import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'next/navigation';
import { tradeApi } from '../../../../api/tradeApi';
import { RecordList } from '../../../../components/RecordList';
import { InternalAmendmentDetails } from '../../../../components/InternalAmendmentDetails';

// ABOUTME: Internal Amendment Portfolio page (Bank-only operational adjustments).
// ABOUTME: Dedicated view for non-UCP adjustments like Facility or Fee account changes.

export default function InternalAmendmentPage() {
  const searchParams = useSearchParams();
  const amendmentId = searchParams.get('amendmentId');
  
  const [amendments, setAmendments] = useState<any[]>([]);
  const [selectedAmendment, setSelectedAmendment] = useState<any | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!amendmentId) {
      setLoading(true);
      tradeApi.getInternalAmendments()
        .then(data => {
          setAmendments(data.amendmentList || []);
          setLoading(false);
        })
        .catch(err => {
          console.error("Fetch Internal Amendments Error:", err);
          setLoading(false);
        });
    } else {
      setLoading(true);
      tradeApi.getInternalAmendment(amendmentId)
        .then(data => {
          setSelectedAmendment(data.amendment || data);
          setLoading(false);
        })
        .catch(err => {
          console.error("Fetch Internal Amendment Error:", err);
          setLoading(false);
        });
    }
  }, [amendmentId]);

  if (amendmentId && selectedAmendment) {
    return (
      <div className="p-8">
        <button 
          className="mb-6 text-slate-500 hover:text-blue-600 font-bold flex items-center gap-2"
          onClick={() => window.location.href = '/import-lc/amendments/internal'}
        >
          ← Back to Internal Amendment Portfolio
        </button>
        <InternalAmendmentDetails amendment={selectedAmendment} />
      </div>
    );
  }

  const columns = [
    { key: 'internalAmendmentId', label: 'Internal ID' },
    { key: 'instrumentId', label: 'Instrument' },
    { key: 'amendmentDate', label: 'Date', render: (val: any) => val ? new Date(val).toLocaleDateString() : '---' },
    { key: 'newFacilityId', label: 'New Facility' },
    { key: 'newFeeDebitAccountId', label: 'New Fee Account' },
  ];

  return (
    <div className="p-8">
      <RecordList 
        title="Internal Bank Amendment Portfolio"
        description="Operational adjustments (Facility, Fees, Margin) that do not impact the Beneficiary."
        records={amendments}
        columns={columns}
        loading={loading}
        onRowClick={(rec) => window.location.href = `/import-lc/amendments/internal?amendmentId=${rec.internalAmendmentId}`}
      />
    </div>
  );
}
