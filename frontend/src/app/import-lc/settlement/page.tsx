'use client';

import React, { useState, useEffect, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import { tradeApi } from '../../../api/tradeApi';
import { RecordList } from '../../../components/RecordList';
import { SettlementDetails } from '../../../components/SettlementDetails';
import { SettlementForm } from '../../../components/SettlementForm';

// ABOUTME: Enhanced Settlement Portfolio page with direct record browsing.
// ABOUTME: Decoupled from Transaction queue to provide permanent record visibility.

function SettlementContent() {
    const searchParams = useSearchParams();
    const id = searchParams.get('id');
    const settlementId = searchParams.get('settlementId');
    
    const [settlements, setSettlements] = useState<any[]>([]);
    const [selectedSettlement, setSelectedSettlement] = useState<any | null>(null);
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
        if (!id && !settlementId) {
            setLoading(true);
            tradeApi.getSettlements({ 
                pageIndex, 
                pageSize, 
                instrumentSearch: debouncedSearch,
                settlementStatusId: statusFilter
            })
                .then(data => {
                    setSettlements(data.settlementList || []);
                    setTotalCount(data.settlementCount || 0);
                    setLoading(false);
                })
                .catch(err => {
                    console.error("Fetch Settlements Error:", err);
                    setLoading(false);
                });
        } else if (settlementId) {
            setLoading(true);
            tradeApi.getSettlement(settlementId)
                .then(data => {
                    setSelectedSettlement(data);
                    setLoading(false);
                })
                .catch(err => {
                    console.error("Fetch Settlement Error:", err);
                    setLoading(false);
                });
        } else {
          setLoading(false);
        }
    }, [id, settlementId, pageIndex, pageSize, debouncedSearch, statusFilter]);

    // Reset page index on search or filter change
    useEffect(() => {
        setPageIndex(0);
    }, [debouncedSearch, statusFilter]);

    if (settlementId && selectedSettlement) {
        return (
            <div className="p-8">
                <button 
                    className="mb-6 text-slate-500 hover:text-blue-600 font-bold flex items-center gap-2"
                    onClick={() => window.location.href = '/import-lc/settlement'}
                >
                    ← Back to Settlement Portfolio
                </button>
                <SettlementDetails settlement={selectedSettlement} />
            </div>
        );
    }

    if (id) {
        return (
            <div className="p-8">
                <h2 className="text-2xl font-bold mb-6">Initiate LC Settlement</h2>
                <SettlementForm instrumentId={id} />
            </div>
        );
    }

    const columns = [
        { key: 'settlementId', label: 'Settlement ID' },
        { key: 'instrumentId', label: 'Instrument' },
        { key: 'valueDate', label: 'Value Date' },
        { 
            key: 'principalAmount', 
            label: 'Amount',
            render: (val: any, row: any) => `${row.remittanceCurrency || 'USD'} ${val?.toLocaleString()}`
        },
        { 
            key: 'settlementStatusId', 
            label: 'Status',
            render: (val: any) => (
                <span className={`status-tag ${val === 'SETTLE_PAID' ? 'success' : 'warning'}`}>
                    {val?.replace('SETTLE_', '') || 'PENDING'}
                </span>
            )
        }
    ];

    return (
        <div className="p-8">
            <RecordList 
                title="LC Settlement Portfolio"
                description="Direct access to all historical and pending settlement records."
                records={settlements}
                columns={columns}
                loading={loading}
                onRowClick={(rec) => window.location.href = `/import-lc/settlement?id=${rec.instrumentId}&settlementId=${rec.settlementId}`}
                totalCount={totalCount}
                pageIndex={pageIndex}
                pageSize={pageSize}
                onPageChange={setPageIndex}
                onSearchChange={setInstrumentSearch}
                searchValue={instrumentSearch}
                statusOptions={[
                    { value: 'SETTLE_PENDING', label: 'Pending' },
                    { value: 'SETTLE_EXECUTED', label: 'Executed' },
                    { value: 'SETTLE_FAILED', label: 'Failed' }
                ]}
                statusValue={statusFilter}
                onStatusChange={setStatusFilter}
            />
            <style jsx>{`
                .status-tag { padding: 0.25rem 0.5rem; border-radius: 4px; font-size: 0.7rem; font-weight: 800; text-transform: uppercase; }
                .status-tag.warning { background: #fffbeb; color: #92400e; }
                .status-tag.success { background: #dcfce7; color: #166534; }
            `}</style>
        </div>
    );
}

export default function SettlementPage() {
    return (
        <Suspense fallback={<div className="p-8 text-center text-slate-500">Loading Settlement context...</div>}>
            <SettlementContent />
        </Suspense>
    );
}
