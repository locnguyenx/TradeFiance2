'use client';

import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'next/navigation';
import { tradeApi } from '../../../api/tradeApi';
import { RecordList } from '../../../components/RecordList';
import { AmendmentDetails } from '../../../components/AmendmentDetails';
import { AmendmentStepper } from '../../../components/AmendmentStepper';

// ABOUTME: Enhanced Amendment Portfolio page with direct record browsing (REQ-UI-IMP-03).
// ABOUTME: Decoupled from Transaction queue to provide permanent record visibility.

export default function AmendmentPage() {
  const searchParams = useSearchParams();
  const id = searchParams.get('id');
  const amendmentId = searchParams.get('amendmentId');
  
  const [amendments, setAmendments] = useState<any[]>([]);
  const [selectedAmendment, setSelectedAmendment] = useState<any | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id && !amendmentId) {
      setLoading(true);
      tradeApi.getAmendments()
        .then(data => {
          setAmendments(data.amendmentList || []);
          setLoading(false);
        })
        .catch(err => {
          console.error("Fetch Amendments Error:", err);
          setLoading(false);
        });
    } else if (amendmentId) {
      setLoading(true);
      tradeApi.getAmendment(id || 'DUMMY', amendmentId)
        .then(data => {
          setSelectedAmendment(data);
          setLoading(false);
        })
        .catch(err => {
          console.error("Fetch Amendment Error:", err);
          setLoading(false);
        });
    }
  }, [id, amendmentId]);

  if (amendmentId && selectedAmendment) {
    return (
      <div className="p-8">
        <button 
          className="mb-6 text-slate-500 hover:text-blue-600 font-bold flex items-center gap-2"
          onClick={() => window.location.href = '/import-lc/amendments'}
        >
          ← Back to Amendment Portfolio
        </button>
        <AmendmentDetails amendment={selectedAmendment} />
      </div>
    );
  }

  if (id) {
    return (
      <div className="p-8">
        <h1>Issue LC Amendment</h1>
        <AmendmentStepper lcId={id} />
      </div>
    );
  }

  const columns = [
    { key: 'amendmentId', label: 'Amendment ID' },
    { key: 'instrumentId', label: 'Instrument' },
    { key: 'amendmentDate', label: 'Date' },
    { 
      key: 'amountAdjustment', 
      label: 'Adjustment',
      render: (val: any) => (
        <span className={val > 0 ? 'text-green-600' : val < 0 ? 'text-red-600' : ''}>
          {val ? val.toLocaleString(undefined, { signDisplay: 'always' }) : '0.00'}
        </span>
      )
    },
    { key: 'newExpiryDate', label: 'New Expiry' }
  ];

  return (
    <div className="p-8">
      <RecordList 
        title="Import LC Amendment Portfolio"
        description="Direct access to all historical and pending amendment records."
        records={amendments}
        columns={columns}
        loading={loading}
        onRowClick={(rec) => window.location.href = `/import-lc/amendments?id=${rec.instrumentId}&amendmentId=${rec.amendmentId}`}
      />
    </div>
  );
}
