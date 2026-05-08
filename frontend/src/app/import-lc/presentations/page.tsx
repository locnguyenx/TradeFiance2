'use client';

import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'next/navigation';
import { tradeApi } from '../../../api/tradeApi';
import { RecordList } from '../../../components/RecordList';
import { PresentationDetails } from '../../../components/PresentationDetails';
import { PresentationLodgement } from '../../../components/PresentationLodgement';

// ABOUTME: Enhanced Presentation Portfolio page with direct record browsing.
// ABOUTME: Decoupled from Transaction queue to provide permanent record visibility.

export default function PresentationsPage() {
    const searchParams = useSearchParams();
    const id = searchParams.get('id');
    const presentationId = searchParams.get('presentationId');
    
    const [presentations, setPresentations] = useState<any[]>([]);
    const [selectedPresentation, setSelectedPresentation] = useState<any | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!id && !presentationId) {
            setLoading(true);
            tradeApi.getPresentations()
                .then(data => {
                    setPresentations(data.presentationList || []);
                    setLoading(false);
                })
                .catch(err => {
                    console.error("Fetch Presentations Error:", err);
                    setLoading(false);
                });
        } else if (presentationId) {
            setLoading(true);
            tradeApi.getPresentation(presentationId)
                .then(data => {
                    setSelectedPresentation(data);
                    setLoading(false);
                })
                .catch(err => {
                    console.error("Fetch Presentation Error:", err);
                    setLoading(false);
                });
        }
    }, [id, presentationId]);

    if (presentationId && selectedPresentation) {
        return (
            <div className="p-8">
                <button 
                    className="mb-6 text-slate-500 hover:text-blue-600 font-bold flex items-center gap-2"
                    onClick={() => window.location.href = '/import-lc/presentations'}
                >
                    ← Back to Presentation Portfolio
                </button>
                <PresentationDetails presentation={selectedPresentation} />
            </div>
        );
    }

    if (id) {
        return (
            <div className="p-8">
                <h1>Log LC Presentation</h1>
                <PresentationLodgement instrumentId={id} />
            </div>
        );
    }

    const columns = [
        { key: 'presentationId', label: 'Presentation ID' },
        { key: 'instrumentId', label: 'Instrument' },
        { key: 'presentationDate', label: 'Date' },
        { 
            key: 'claimAmount', 
            label: 'Amount',
            render: (val: any, row: any) => `${row.claimCurrency || 'USD'} ${val?.toLocaleString()}`
        },
        { 
            key: 'isDiscrepant', 
            label: 'Status',
            render: (val: any) => (
                <span className={`status-tag ${val === 'Y' ? 'urgent' : 'success'}`}>
                    {val === 'Y' ? 'DISCREPANT' : 'CLEAN'}
                </span>
            )
        }
    ];

    return (
        <div className="p-8">
            <RecordList 
                title="Document Presentation Portfolio"
                description="Direct access to all historical and pending document presentations."
                records={presentations}
                columns={columns}
                loading={loading}
                onRowClick={(rec) => window.location.href = `/import-lc/presentations?id=${rec.instrumentId}&presentationId=${rec.presentationId}`}
            />
            <style jsx>{`
                .status-tag { padding: 0.25rem 0.5rem; border-radius: 4px; font-size: 0.7rem; font-weight: 800; text-transform: uppercase; }
                .status-tag.urgent { background: #fee2e2; color: #991b1b; }
                .status-tag.success { background: #dcfce7; color: #166534; }
            `}</style>
        </div>
    );
}
