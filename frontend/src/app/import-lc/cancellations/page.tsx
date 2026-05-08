'use client';

import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'next/navigation';
import { tradeApi } from '../../../api/tradeApi';
import { RecordList } from '../../../components/RecordList';
import { CancellationRequest } from '../../../components/CancellationRequest';
import { TransactionDetails } from '../../../components/TransactionDetails';

// ABOUTME: Enhanced Cancellations Portfolio page with direct record browsing.
// ABOUTME: Decoupled from generic Transaction log to provide product-specific visibility.

export default function CancellationsPage() {
    const searchParams = useSearchParams();
    const id = searchParams.get('id');
    const transactionId = searchParams.get('transactionId');
    
    const [cancellations, setCancellations] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!id && !transactionId) {
            setLoading(true);
            tradeApi.getTransactions({ transactionTypeEnumId: 'IMP_CANCEL' })
                .then(data => {
                    setCancellations(data.transactionList || []);
                    setLoading(false);
                })
                .catch(err => {
                    console.error("Fetch Cancellations Error:", err);
                    setLoading(false);
                });
        }
    }, [id, transactionId]);

    if (transactionId) {
        return (
            <div className="p-8">
                <button 
                    className="mb-6 text-slate-500 hover:text-blue-600 font-bold flex items-center gap-2"
                    onClick={() => window.location.href = '/import-lc/cancellations'}
                >
                    ← Back to Cancellation Portfolio
                </button>
                <TransactionDetails transactionId={transactionId} />
            </div>
        );
    }

    if (id) {
        return (
            <div className="p-8">
                <h1>Request LC Cancellation</h1>
                <CancellationRequest instrumentId={id} />
            </div>
        );
    }

    const columns = [
        { key: 'transactionId', label: 'Request ID' },
        { key: 'instrumentId', label: 'Instrument' },
        { key: 'transactionDate', label: 'Request Date', render: (val: any) => new Date(val).toLocaleDateString() },
        { key: 'makerUserId', label: 'Requested By' },
        { 
            key: 'transactionStatusId', 
            label: 'Status',
            render: (val: any) => (
                <span className={`status-tag ${val === 'TX_APPROVED' ? 'success' : val === 'TX_PENDING' ? 'warning' : 'default'}`}>
                    {val?.replace('TX_', '') || 'PENDING'}
                </span>
            )
        }
    ];

    return (
        <div className="p-8">
            <RecordList 
                title="LC Cancellation Requests"
                description="Browse and track all formal requests to terminate active letters of credit."
                records={cancellations}
                columns={columns}
                loading={loading}
                onRowClick={(rec) => window.location.href = `/import-lc/cancellations?transactionId=${rec.transactionId}`}
            />
            <style jsx>{`
                .status-tag { padding: 0.25rem 0.5rem; border-radius: 4px; font-size: 0.7rem; font-weight: 800; text-transform: uppercase; }
                .status-tag.warning { background: #fffbeb; color: #92400e; }
                .status-tag.success { background: #dcfce7; color: #166534; }
                .status-tag.default { background: #f1f5f9; color: #475569; }
            `}</style>
        </div>
    );
}
