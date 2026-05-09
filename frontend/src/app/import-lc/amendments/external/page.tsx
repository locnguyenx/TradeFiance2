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
  
  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize] = useState(20);
  const [totalCount, setTotalCount] = useState(0);
  const [instrumentSearch, setInstrumentSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(instrumentSearch), 500);
    return () => clearTimeout(timer);
  }, [instrumentSearch]);

    useEffect(() => {
    if (!id && !amendmentId) {
      setLoading(true);
      tradeApi.getExternalAmendments({ 
        pageIndex, 
        pageSize, 
        instrumentSearch: debouncedSearch,
        amendmentBusinessStateId: statusFilter
      })
        .then(data => {
          setAmendments(data.amendmentList || []);
          setTotalCount(data.amendmentCount || 0);
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
  }, [id, amendmentId, pageIndex, pageSize, debouncedSearch, statusFilter]);

  // Reset page index on search or filter change
  useEffect(() => {
      setPageIndex(0);
  }, [debouncedSearch, statusFilter]);

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
    { 
      key: 'amendmentBusinessStateId', 
      label: 'Status',
      render: (val: any) => (
        <span className={`status-tag ${val === 'AMEND_COMMITTED' ? 'success' : val === 'AMEND_REJECTED' ? 'error' : 'warning'}`}>
          {val?.replace('AMEND_', '') || 'DRAFT'}
        </span>
      )
    },
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
        totalCount={totalCount}
        pageIndex={pageIndex}
        pageSize={pageSize}
        onPageChange={setPageIndex}
        onSearchChange={setInstrumentSearch}
        searchValue={instrumentSearch}
        statusOptions={[
            { value: 'AMEND_DRAFT', label: 'Draft' },
            { value: 'AMEND_PENDING_APPROVAL', label: 'Pending Approval' },
            { value: 'AMEND_APPROVED', label: 'Approved (Pending Consent)' },
            { value: 'AMEND_REJECTED', label: 'Rejected' },
            { value: 'AMEND_COMMITTED', label: 'Committed to LC' }
        ]}
        statusValue={statusFilter}
        onStatusChange={setStatusFilter}
      />
    </div>
  );
}
<style jsx>{`
  .status-tag { padding: 0.25rem 0.5rem; border-radius: 4px; font-size: 0.7rem; font-weight: 800; text-transform: uppercase; }
  .status-tag.warning { background: #fffbeb; color: #92400e; }
  .status-tag.success { background: #dcfce7; color: #166534; }
  .status-tag.error { background: #fee2e2; color: #991b1b; }
`}</style>
