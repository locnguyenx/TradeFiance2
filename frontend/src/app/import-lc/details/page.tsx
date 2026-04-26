'use client';

import React, { useEffect, useState, Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { tradeApi } from '../../../api/tradeApi';
import { TradeInstrument, ImportLetterOfCredit } from '../../../api/types';
import { InstrumentDetails } from '../../../components/InstrumentDetails';

// ABOUTME: Dedicated Transaction Details page implementing full-screen audit view.
// ABOUTME: Uses InstrumentDetails component with "Back to List" navigation.

const DetailsView = () => {
    const searchParams = useSearchParams();
    const router = useRouter();
    const id = searchParams.get('id');
    
    const [instrument, setInstrument] = useState<(TradeInstrument & ImportLetterOfCredit) | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!id) {
            setError('No Transaction ID provided.');
            setLoading(false);
            return;
        }

        setLoading(true);
        tradeApi.getInstrument(id)
            .then(data => {
                setInstrument(data as any);
                setLoading(false);
            })
            .catch(err => {
                console.error('Fetch Details Error:', err);
                setError('Failed to load transaction details.');
                setLoading(false);
            });
    }, [id]);

    if (loading) {
        return (
            <div className="loading-state">
                <div className="spinner"></div>
                <p>Retrieving Transaction Record...</p>
                <style jsx>{`
                    .loading-state { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 60vh; color: #64748b; font-weight: 600; gap: 1rem; }
                    .spinner { width: 40px; height: 40px; border: 3px solid #f1f5f9; border-top-color: #2563eb; border-radius: 50%; animation: spin 0.8s linear infinite; }
                    @keyframes spin { to { transform: rotate(360deg); } }
                `}</style>
            </div>
        );
    }

    if (error || !instrument) {
        return (
            <div className="error-state premium-card">
                <h3>System Error</h3>
                <p>{error || 'Record Not Found'}</p>
                <button className="btn primary" onClick={() => router.back()}>Return to Dashboard</button>
                <style jsx>{`
                    .error-state { padding: 3rem; text-align: center; max-width: 500px; margin: 4rem auto; }
                    h3 { color: #dc2626; margin-bottom: 1rem; }
                    p { color: #64748b; margin-bottom: 2rem; }
                `}</style>
            </div>
        );
    }

    return (
        <div className="details-page-container">
            <header className="details-header">
                <button className="back-btn" onClick={() => router.back()}>
                    ← Back to Dashboard
                </button>
                <div className="header-meta">
                    <span className="ref-tag">{instrument.transactionRef}</span>
                    <span className="status-badge">{instrument.businessStateId?.replace('LC_', '')}</span>
                </div>
            </header>
            
            <main className="details-content">
                <InstrumentDetails instrument={instrument} />
            </main>

            <style jsx>{`
                .details-page-container { display: flex; flex-direction: column; height: calc(100vh - 120px); gap: 1rem; }
                .details-header { display: flex; justify-content: space-between; align-items: center; padding-bottom: 0.5rem; }
                .back-btn { background: none; border: none; color: #2563eb; font-weight: 700; cursor: pointer; padding: 0; font-size: 0.9rem; }
                .back-btn:hover { text-decoration: underline; }
                
                .header-meta { display: flex; align-items: center; gap: 1rem; }
                .ref-tag { font-family: 'JetBrains Mono', monospace; font-weight: 800; color: #1e293b; background: #f1f5f9; padding: 0.4rem 0.8rem; border-radius: 6px; }
                .status-badge { background: #dcfce7; color: #166534; padding: 0.25rem 0.75rem; border-radius: 999px; font-size: 0.75rem; font-weight: 700; text-transform: uppercase; }
                
                .details-content { flex: 1; min-height: 0; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; background: white; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); }
            `}</style>
        </div>
    );
};

export default function DetailsPage() {
    return (
        <Suspense fallback={<div>Loading Search Context...</div>}>
            <DetailsView />
        </Suspense>
    );
}
