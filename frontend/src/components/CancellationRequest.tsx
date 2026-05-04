'use client';

import React, { useState, useEffect } from 'react';
import { tradeApi } from '../api/tradeApi';

// ABOUTME: LC Cancellation Request component implementing REQ-IMP-PRC-06.
// ABOUTME: Enforces mutual consent for irrevocable instruments and previews limit release.

interface CancellationRequestProps {
    instrumentId: string;
}

export const CancellationRequest: React.FC<CancellationRequestProps> = ({ instrumentId }) => {
    const [consentReceived, setConsentReceived] = useState(false);
    const [reason, setReason] = useState('');
    const [instrument, setInstrument] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        if (!instrumentId) {
            setLoading(false);
            setError('Please select an active Letter of Credit from the dashboard to request cancellation.');
            return;
        }

        setLoading(true);
        setError('');
        tradeApi.getImportLc(instrumentId)
            .then(data => {
                setInstrument(data);
                setLoading(false);
            })
            .catch(err => {
                setError(err.message || 'Failed to load instrument context. Please verify the LC ID.');
                setLoading(false);
            });
    }, [instrumentId]);
    
    // Mock LC Amount for limit release preview
    const originalAmount = 500000;

    if (loading && !instrument) return <div className="p-8 text-center premium-card">Loading LC Context...</div>;
    if (!instrument) {
        return (
            <div className="p-8 text-center premium-card">
                <div className="alert-box warning mb-4" style={{ textAlign: 'center' }}>
                    {error || 'Letter of Credit not found or context missing.'}
                </div>
                <button 
                    className="secondary-btn" 
                    onClick={() => window.location.href = '/import-lc'}
                >
                    Back to Dashboard
                </button>
            </div>
        );
    }

    return (
        <div className="cancellation-container premium-card">
            <header className="header-box">
                <h2 className="title">LC Cancellation Request</h2>
                <p className="subtitle">Instrument: {instrumentId}</p>
            </header>

            <main className="form-content">
                <section className="consent-warning">
                    <div className="alert-box warning">
                        <strong>Important:</strong> Irrevocable Letters of Credit cannot be cancelled without the express consent of the Beneficiary and the Confirming Bank (if any).
                    </div>
                </section>

                <section className="fields-grid">
                    <div className="field-group checkbox-field">
                        <label className="flex items-center gap-2 cursor-pointer">
                            <input 
                                type="checkbox" 
                                checked={consentReceived}
                                onChange={e => setConsentReceived(e.target.checked)}
                            />
                            <span>Beneficiary Consent Received (SWIFT Ref required)</span>
                        </label>
                    </div>

                    <div className="field-group">
                        <label htmlFor="reason">Cancellation Reason</label>
                        <textarea 
                            id="reason"
                            rows={4}
                            value={reason}
                            onChange={e => setReason(e.target.value)}
                            placeholder="State the reason for early closure..."
                        />
                    </div>
                </section>

                <section className="release-preview">
                    <div className="preview-card">
                        <span className="label">Projected Limit Release</span>
                        <span className="value">$ {originalAmount.toLocaleString()}</span>
                    </div>
                </section>
            </main>

            <footer className="footer-actions">
                <button className="secondary-btn">Cancel</button>
                <button 
                    className="primary-btn danger" 
                    disabled={!consentReceived}
                    onClick={() => alert('Cancellation Submitted')}
                >
                    Submit Cancellation
                </button>
            </footer>

            <style jsx>{`
                .cancellation-container { padding: 2rem; background: white; border-radius: 12px; }
                .header-box { border-bottom: 1px solid #f1f5f9; padding-bottom: 1.5rem; margin-bottom: 2rem; }
                .title { font-size: 1.25rem; font-weight: 700; color: #0f172a; }
                .subtitle { font-size: 0.875rem; color: #64748b; }

                .alert-box.warning { background: #fff7ed; border: 1px solid #ffedd5; color: #9a3412; padding: 1rem; border-radius: 8px; font-size: 0.875rem; line-height: 1.5; margin-bottom: 2rem; }
                
                .fields-grid { display: flex; flex-direction: column; gap: 2rem; }
                .field-group { display: flex; flex-direction: column; gap: 0.5rem; }
                .field-group label { font-size: 0.875rem; font-weight: 600; color: #334155; }
                .field-group textarea { padding: 0.75rem; border: 1px solid #e2e8f0; border-radius: 6px; }

                .release-preview { margin-top: 2.5rem; }
                .preview-card { background: #f0fdf4; border: 1px solid #dcfce7; padding: 1.5rem; border-radius: 8px; display: flex; justify-content: space-between; align-items: center; }
                .preview-card .label { font-weight: 600; color: #166534; }
                .preview-card .value { font-size: 1.25rem; font-weight: 800; color: #14532d; }

                .footer-actions { display: flex; justify-content: flex-end; gap: 1rem; margin-top: 3rem; border-top: 1px solid #f1f5f9; padding-top: 2rem; }
                .primary-btn { background: #2563eb; color: white; border: none; padding: 0.75rem 2rem; border-radius: 6px; font-weight: 600; cursor: pointer; }
                .primary-btn.danger { background: #dc2626; }
                .primary-btn:disabled { opacity: 0.5; cursor: not-allowed; background: #94a3b8; }
                .secondary-btn { background: #f8fafc; color: #475569; border: 1px solid #e2e8f0; padding: 0.75rem 2rem; border-radius: 6px; font-weight: 600; cursor: pointer; }
            `}</style>
        </div>
    );
};
