'use client';

import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'next/navigation';
import { tradeApi } from '../../../api/tradeApi';
import { RecordList } from '../../../components/RecordList';
import { ShippingGuaranteeDetails } from '../../../components/ShippingGuaranteeDetails';
import { ShippingGuaranteeForm } from '../../../components/ShippingGuaranteeForm';

// ABOUTME: Enhanced Shipping Guarantee Portfolio page with direct record browsing.
// ABOUTME: Decoupled from Transaction log to provide product-specific visibility.

export default function ShippingGuaranteesPage() {
    const searchParams = useSearchParams();
    const id = searchParams.get('id');
    const guaranteeId = searchParams.get('guaranteeId');
    
    const [guarantees, setGuarantees] = useState<any[]>([]);
    const [selectedGuarantee, setSelectedGuarantee] = useState<any | null>(null);
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
        if (!id && !guaranteeId) {
            setLoading(true);
            tradeApi.getShippingGuarantees({ 
                pageIndex, 
                pageSize, 
                instrumentSearch: debouncedSearch,
                sgStatusId: statusFilter
            })
                .then(data => {
                    setGuarantees(data.guaranteeList || []);
                    setTotalCount(data.guaranteeCount || 0);
                    setLoading(false);
                })
                .catch(err => {
                    console.error("Fetch Guarantees Error:", err);
                    setLoading(false);
                });
        } else if (guaranteeId) {
            setLoading(true);
            tradeApi.getShippingGuarantee(guaranteeId)
                .then(data => {
                    setSelectedGuarantee(data);
                    setLoading(false);
                })
                .catch(err => {
                    console.error("Fetch Guarantee Error:", err);
                    setLoading(false);
                });
        }
    }, [id, guaranteeId, pageIndex, pageSize, debouncedSearch, statusFilter]);

    // Reset page index on search or filter change
    useEffect(() => {
        setPageIndex(0);
    }, [debouncedSearch, statusFilter]);

    if (guaranteeId && selectedGuarantee) {
        return (
            <div className="p-8">
                <button 
                    className="mb-6 text-slate-500 hover:text-blue-600 font-bold flex items-center gap-2"
                    onClick={() => window.location.href = '/import-lc/shipping-guarantees'}
                >
                    ← Back to Guarantee Portfolio
                </button>
                <ShippingGuaranteeDetails guarantee={selectedGuarantee} />
            </div>
        );
    }

    if (id) {
        return (
            <div className="p-8">
                <h1>Request Shipping Guarantee</h1>
                <ShippingGuaranteeForm instrumentId={id} />
            </div>
        );
    }

    const columns = [
        { key: 'guaranteeId', label: 'Guarantee ID' },
        { key: 'instrumentId', label: 'Instrument' },
        { key: 'vesselName', label: 'Vessel' },
        { key: 'billOfLadingNo', label: 'B/L Number' },
        { 
            key: 'invoiceAmount', 
            label: 'Amount',
            render: (val: any) => val?.toLocaleString()
        },
        { key: 'expiryDate', label: 'Expiry' }
    ];

    return (
        <div className="p-8">
            <RecordList 
                title="Shipping Guarantee Portfolio"
                description="Direct access to all active and historical shipping guarantees."
                records={guarantees}
                columns={columns}
                loading={loading}
                onRowClick={(rec) => window.location.href = `/import-lc/shipping-guarantees?id=${rec.instrumentId}&guaranteeId=${rec.guaranteeId}`}
                totalCount={totalCount}
                pageIndex={pageIndex}
                pageSize={pageSize}
                onPageChange={setPageIndex}
                onSearchChange={setInstrumentSearch}
                searchValue={instrumentSearch}
                statusOptions={[
                    { value: 'SG_DRAFT', label: 'Draft' },
                    { value: 'SG_ISSUED', label: 'Issued' },
                    { value: 'SG_RETURNED', label: 'Returned' }
                ]}
                statusValue={statusFilter}
                onStatusChange={setStatusFilter}
            />
        </div>
    );
}
