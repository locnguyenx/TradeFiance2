'use client';

import React, { useEffect, useState, Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { tradeApi } from '../../../api/tradeApi';
import { TradeInstrument, ImportLetterOfCredit } from '../../../api/types';
import { InstrumentDetails } from '../../../components/InstrumentDetails';
import { InstrumentTimeline } from '../../../components/InstrumentTimeline';

// ABOUTME: Dedicated Transaction Details page implementing full-screen audit view.
// ABOUTME: Uses InstrumentDetails component with "Back to List" navigation.

const DetailsView = () => {
    const searchParams = useSearchParams();
    const router = useRouter();
    const id = searchParams.get('id');
    
    const [instrument, setInstrument] = useState<(TradeInstrument & ImportLetterOfCredit) | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [activeTab, setActiveTab] = useState<'DETAILS' | 'TIMELINE'>('DETAILS');

    const fetchDetails = () => {
        if (!id) return;
        setLoading(true);
        tradeApi.getImportLc(id)
            .then(data => {
                setInstrument(data as any);
                setLoading(false);
            })
            .catch(err => {
                console.error('Fetch Details Error:', err);
                setError('Failed to load transaction details.');
                setLoading(false);
            });
    };

    useEffect(() => {
        if (!id) {
            setError('No Transaction ID provided.');
            setLoading(false);
            return;
        }
        fetchDetails();
    }, [id]);

    if (loading) {
        return (
            <div className="loading-state">
                <div className="spinner"></div>
                <p>Retrieving Trade Asset Record...</p>
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
                    .error-state { padding: 3rem; text-align: center; max-width: 500px; margin: 4rem auto; border: 1px solid #e2e8f0; border-radius: 12px; }
                    h3 { color: #dc2626; margin-bottom: 1rem; }
                    p { color: #64748b; margin-bottom: 2rem; }
                    .btn.primary { background: #2563eb; color: white; padding: 0.5rem 1rem; border-radius: 6px; border: none; font-weight: 700; cursor: pointer; }
                `}</style>
            </div>
        );
    }

    return (
        <div className="details-page-container">
            <header className="details-header">
                <div className="left-meta">
                    <button className="back-btn" onClick={() => router.push('/import-lc')}>
                        ← Back to List
                    </button>
                    <div className="header-meta">
                        <span className="ref-tag">{instrument.transactionRef}</span>
                        <span className="status-badge">{instrument.businessStateId?.replace('LC_', '').replace('INST_', '')}</span>
                    </div>
                </div>
                <div className="view-selector">
                    <button className={`tab-btn ${activeTab === 'DETAILS' ? 'active' : ''}`} onClick={() => setActiveTab('DETAILS')}>Current State</button>
                    <button className={`tab-btn ${activeTab === 'TIMELINE' ? 'active' : ''}`} onClick={() => setActiveTab('TIMELINE')}>Audit Narrative</button>
                </div>
            </header>
            
            <main className="details-content">
                {activeTab === 'DETAILS' ? (
                    <InstrumentDetails instrument={instrument} />
                ) : (
                    <div className="timeline-container">
                        <InstrumentTimeline instrumentId={id!} onActionComplete={fetchDetails} />
                    </div>
                )}
            </main>

            <style jsx>{`
                .details-page-container { display: flex; flex-direction: column; height: calc(100vh - 120px); gap: 1rem; }
                .details-header { display: flex; justify-content: space-between; align-items: flex-end; padding-bottom: 0.5rem; border-bottom: 1px solid #e2e8f0; }
                .left-meta { display: flex; flex-direction: column; gap: 0.5rem; }
                .back-btn { background: none; border: none; color: #64748b; font-weight: 700; cursor: pointer; padding: 0; font-size: 0.8rem; text-align: left; }
                .back-btn:hover { color: #2563eb; }
                
                .header-meta { display: flex; align-items: center; gap: 1rem; }
                .ref-tag { font-family: 'JetBrains Mono', monospace; font-weight: 800; color: #1e293b; font-size: 1.25rem; }
                .status-badge { background: #dcfce7; color: #166534; padding: 0.25rem 0.75rem; border-radius: 999px; font-size: 0.7rem; font-weight: 800; text-transform: uppercase; border: 1px solid #bbf7d0; }
                
                .view-selector { display: flex; background: #f1f5f9; padding: 4px; border-radius: 8px; }
                .tab-btn { border: none; background: transparent; color: #64748b; font-size: 0.8125rem; font-weight: 600; padding: 0.5rem 1rem; border-radius: 6px; cursor: pointer; transition: all 0.2s; }
                .tab-btn.active { background: white; color: #1e293b; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }

                .details-content { flex: 1; min-height: 0; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; background: white; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); }
                .timeline-container { height: 100%; overflow-y: auto; background: #f8fafc; padding: 2rem; }
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
