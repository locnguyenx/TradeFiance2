'use client';

import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'next/navigation';
import { tradeApi } from '../../../../api/tradeApi';
import { RecordList } from '../../../../components/RecordList';
import { ExternalAmendmentDetails } from '../../../../components/ExternalAmendmentDetails';
import { AmendmentStepper } from '../../../../components/AmendmentStepper';

// ABOUTME: External Amendment Portfolio page (UCP 600 / MT 707 compliant).
// ABOUTME: Dedicated view for changes requiring beneficiary consent and SWIFT generation.

export default function ExternalAmendmentPage() {
  const searchParams = useSearchParams();
  const id = searchParams.get('id');
  const amendmentId = searchParams.get('amendmentId');
  
  const [amendments, setAmendments] = useState<any[]>([]);
  const [selectedAmendment, setSelectedAmendment] = useState<any | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id && !amendmentId) {
      setLoading(true);
      tradeApi.getExternalAmendments()
        .then(data => {
          setAmendments(data.amendmentList || []);
          setLoading(false);
        })
        .catch(err => {
          console.error("Fetch External Amendments Error:", err);
          setLoading(false);
        });
    } else if (amendmentId) {
      setLoading(true);
      tradeApi.getExternalAmendment(amendmentId)
        .then(data => {
          setSelectedAmendment(data.amendment || data);
          setLoading(false);
        })
        .catch(err => {
          console.error("Fetch External Amendment Error:", err);
          setLoading(false);
        });
    }
  }, [id, amendmentId]);

  if (amendmentId && selectedAmendment) {
    return (
      <div className="p-8">
        <button 
          className="mb-6 text-slate-500 hover:text-blue-600 font-bold flex items-center gap-2"
          onClick={() => window.location.href = '/import-lc/amendments/external'}
        >
          ← Back to External Amendment Portfolio
        </button>
        <ExternalAmendmentDetails amendment={selectedAmendment} />
      </div>
    );
  }

  const columns = [
    { key: 'amendmentId', label: 'Amendment ID' },
    { key: 'instrumentId', label: 'Instrument' },
    { key: 'amendmentDate', label: 'Date', render: (val: any) => val ? new Date(val).toLocaleDateString() : '---' },
    { 
      key: 'amountAdjustment', 
      label: 'Adjustment',
      render: (_, rec: any) => {
        const inc = Number(rec.amountIncrease) || 0;
        const dec = Number(rec.amountDecrease) || 0;
        const val = inc - dec;
        const formatted = val > 0 ? '+' + val.toFixed(2) : val.toFixed(2);
        return (
          <span className={val > 0 ? 'text-green-600' : val < 0 ? 'text-red-600' : ''}>
            {formatted}
          </span>
        );
      }
    },
    { key: 'beneficiaryConsentStatusId', label: 'Bene Consent' },
    { key: 'newExpiryDate', label: 'New Expiry', render: (val: any) => val ? new Date(val).toLocaleDateString() : '---' }
  ];

  return (
    <div className="p-8">
      <RecordList 
        title="External LC Amendment Portfolio"
        description="Records of amendments requiring SWIFT MT 707 and Beneficiary Consent."
        records={amendments}
        columns={columns}
        loading={loading}
        onRowClick={(rec) => window.location.href = `/import-lc/amendments/external?id=${rec.instrumentId}&amendmentId=${rec.amendmentId}`}
      />
    </div>
  );
}
