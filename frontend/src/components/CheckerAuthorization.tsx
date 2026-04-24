'use client';

import React, { useState, useEffect } from 'react';
import { tradeApi } from '../api/tradeApi';
import { TradeInstrument } from '../api/types';

// ABOUTME: High-fidelity Checker Authorization Workspace (REQ-UI-IMP-05).
// ABOUTME: Features "Exposure Widget" progress bars and "Compliance Deck" for risk analysis.

interface Props {
    instrumentId: string;
}

export const CheckerAuthorization: React.FC<Props> = ({ instrumentId }) => {
    const [instrument, setInstrument] = useState<TradeInstrument | null>(null);
    const [authResult, setAuthResult] = useState<boolean | null>(null);
    const [showRejectionModal, setShowRejectionModal] = useState(false);
    const [rejectionReason, setRejectionReason] = useState('');
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        setLoading(true);
        tradeApi.getInstrument(instrumentId).then(data => {
            setInstrument(data);
            setLoading(false);
        });
    }, [instrumentId]);

    const handleApprove = async () => {
        setLoading(true);
        try {
            const result = await tradeApi.authorize(instrumentId);
            setAuthResult(result.isAuthorized);
            // Re-fetch instrument to see updated status (e.g. from PARTIAL to ISSUED)
            const updated = await tradeApi.getInstrument(instrumentId);
            setInstrument(updated);
            if (result.isAuthorized) {
                alert(updated.lifecycleStatusId === 'INST_ISSUED' 
                    ? 'Final Authorization Complete. Instrument Issued.' 
                    : 'Partial Authorization Recorded (1/2). Dual Checker required.');
            }
        } catch (e) {
            console.error(e);
        } finally {
            setLoading(false);
        }
    };

    const handleReject = async () => {
        const result = await tradeApi.rejectToMaker(instrumentId, rejectionReason);
        if (result.success || !result.error) {
            setShowRejectionModal(false);
            // Notify success
        }
    };

    if (loading) return <div className="p-8 text-center">Loading Instrument Data...</div>;
    if (!instrument) return <div className="p-8 text-center text-danger">Instrument not found</div>;

    return (
        <div className="auth-workspace premium-card">
            {authResult === false && (
                <div className="alert-ribbon error">
                    Self-Authorization Forbidden (4-Eyes Principle Violation)
                </div>
            )}

            {instrument.lifecycleStatusId === 'INST_PARTIAL_APPROVAL' && (
                <div className="alert-ribbon warning">
                     Dual Checker Progress: First Authorization Recorded. A second, different checker is required for Tier 4 transactions ({instrument.currencyUomId || 'USD'} {instrument.baseEquivalentAmount.toLocaleString()}).
                </div>
            )}
            
            {instrument.baseEquivalentAmount > 500000 && instrument.lifecycleStatusId === 'INST_PENDING_APPROVAL' && (
                <div className="alert-ribbon info">
                    Tier 4 Transaction Detected: Dual Checker Authorization will be enforced.
                </div>
            )}
            
            <header className="auth-header">
                <div className="flex justify-between items-center w-full">
                    <div className="status-group">
                        <span className="badge primary">Instrument: {instrumentId}</span>
                        <span className="badge secondary">Status: {instrument.lifecycleStatusId.replace('INST_', '').replace('_', ' ')}</span>
                    </div>
                    <div className="action-buttons">
                        <button className="btn secondary" onClick={() => setShowRejectionModal(true)}>Reject</button>
                        <button className="btn warning">Query Compliance</button>
                        <button className="btn primary" onClick={handleApprove}>Authorize</button>
                    </div>
                </div>
            </header>

            <div className="workspace-split">
                <aside className="risk-pane">
                    <section className="risk-section">
                        <h4>Facility Exposure Widget</h4>
                        <div className="exposure-box">
                            <div className="flex justify-between text-xs font-bold mb-2">
                                <span>Total Approved: $10.0M</span>
                                <span>60% Used</span>
                            </div>
                            <div className="progress-container">
                                <div className="bar firm" style={{ width: '40%' }}></div>
                                <div className="bar contingent" style={{ width: '15%' }}></div>
                                <div className="bar orange-reserve" style={{ width: '5%' }}></div>
                            </div>
                            <div className="legend">
                                <span className="dot firm"></span> Firm
                                <span className="dot contingent"></span> Contingent
                                <span className="dot reserve"></span> This Txn
                            </div>
                        </div>
                    </section>

                    <section className="risk-section">
                        <h4>Compliance Deck</h4>
                        <div className="compliance-grid">
                            <div className="stat-row">
                                <span className="label">AML / KYC</span>
                                <span className="val success">CLEARED</span>
                            </div>
                            <div className="stat-row">
                                <span className="label">Sanctions</span>
                                <span className="val success">CLEARED</span>
                            </div>
                            <div className="stat-row">
                                <span className="label">UCP SLA</span>
                                <span className="val warning">T-3 DAYS</span>
                            </div>
                        </div>
                        <div className="compliance-detail">
                            <p>Global Screening performed at 2026-04-22T09:00:00Z. No hits on OSFI, OFAC, or HMT.</p>
                        </div>
                    </section>
                </aside>

                <main className="details-pane">
                    <header className="sub-header">
                        <h4>Instrument Data (Highlighting Deltas)</h4>
                    </header>
                    <div className="data-grid">
                        <div className="data-field">
                            <label>Applicant</label>
                            <p>Global Corp, New York</p>
                        </div>
                        <div className="data-field delta">
                            <label>Amount</label>
                            <div className="delta-box">
                                <span className="old-val">USD {(instrument.snapshotAmount || 0).toLocaleString(undefined, {minimumFractionDigits: 2})}</span>
                                <span className="arrow">→</span>
                                <span className="new-val">USD {(instrument.effectiveAmount || instrument.baseEquivalentAmount || 0).toLocaleString(undefined, {minimumFractionDigits: 2})}</span>
                            </div>
                        </div>
                        <div className="data-field">
                            <label>Beneficiary</label>
                            <p>Export Ltd, Singapore</p>
                        </div>
                        <div className="data-field delta">
                            <label>Expiry Date</label>
                            <div className="delta-box">
                                <span className="old-val">{instrument.snapshotExpiryDate || 'N/A'}</span>
                                <span className="arrow">→</span>
                                <span className="new-val">{instrument.effectiveExpiryDate || 'N/A'}</span>
                            </div>
                        </div>
                        <div className="data-field">
                            <label>Port of Loading</label>
                            <p>Port of New York, USA</p>
                        </div>
                    </div>
                </main>
            </div>

            {showRejectionModal && (
                <div className="modal-overlay">
                    <div className="modal premium-card">
                        <h3>Rejection Reason</h3>
                        <p className="text-xs text-slate-500 mb-4">Required for audit trail and maker correction.</p>
                        <textarea 
                            value={rejectionReason}
                            onChange={(e) => setRejectionReason(e.target.value)}
                            placeholder="Specify the reason for returning this transaction..." 
                        />
                        <div className="flex justify-end gap-2 mt-4">
                            <button className="btn secondary" onClick={() => setShowRejectionModal(false)}>Cancel</button>
                            <button className="btn danger" onClick={handleReject}>Confirm Rejection</button>
                        </div>
                    </div>
                </div>
            )}

            <style jsx>{`
                .auth-workspace { display: flex; flex-direction: column; min-height: 600px; overflow: hidden; background: white; }
                .auth-header { padding: 1.5rem; border-bottom: 1px solid #f1f5f9; background: #f8fafc; }
                .workspace-split { display: flex; flex: 1; min-height: 0; }
                
                .alert-ribbon { padding: 0.75rem 1.5rem; font-size: 0.8125rem; font-weight: 700; text-align: center; }
                .alert-ribbon.error { background: #fee2e2; color: #991b1b; }
                .alert-ribbon.warning { background: #fffbeb; color: #92400e; }

                .risk-pane { width: 320px; border-right: 1px solid #f1f5f9; background: white; padding: 1.5rem; display: flex; flex-direction: column; gap: 2rem; }
                .details-pane { flex: 1; display: flex; flex-direction: column; }
                .sub-header { padding: 1rem 1.5rem; border-bottom: 1px solid #f1f5f9; }
                
                h4 { font-size: 0.75rem; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.05em; font-weight: 800; margin-bottom: 1.25rem; }

                /* Exposure Widget */
                .progress-container { height: 12px; background: #f1f5f9; border-radius: 6px; overflow: hidden; display: flex; }
                .bar { height: 100%; }
                .firm { background: #1e3a8a; }
                .contingent { background: #3b82f6; }
                .orange-reserve { background: #f59e0b; }
                .legend { margin-top: 1rem; display: flex; gap: 0.75rem; font-size: 0.65rem; font-weight: 700; color: #64748b; }
                .dot { width: 8px; height: 8px; border-radius: 2px; display: inline-block; }

                /* Compliance Deck */
                .compliance-grid { display: flex; flex-direction: column; gap: 0.75rem; margin-bottom: 1rem; }
                .stat-row { display: flex; justify-content: space-between; font-size: 0.8125rem; }
                .stat-row .label { color: #64748b; }
                .val { font-weight: 800; }
                .val.success { color: #059669; }
                .val.warning { color: #d97706; }
                .compliance-detail { font-size: 0.65rem; color: #94a3b8; line-height: 1.4; border-top: 1px solid #f1f5f9; padding-top: 0.75rem; }

                /* Data Grid & Delta */
                .data-grid { padding: 2rem; display: grid; grid-template-columns: 1fr 1fr; gap: 2rem; overflow-y: auto; }
                .data-field label { display: block; font-size: 0.75rem; font-weight: 700; color: #94a3b8; text-transform: uppercase; margin-bottom: 0.5rem; }
                .data-field p { font-size: 0.9375rem; color: #1e293b; font-weight: 500; }
                .delta { grid-column: span 2; background: #fffbeb; padding: 1rem; border-radius: 8px; border-left: 4px solid #f59e0b; }
                .delta-box { display: flex; align-items: center; gap: 1rem; }
                .old-val { color: #94a3b8; text-decoration: line-through; }
                .arrow { color: #f59e0b; font-weight: 800; }
                .new-val { color: #1e293b; font-weight: 800; }

                /* Buttons */
                .btn { padding: 0.6rem 1.25rem; border-radius: 6px; font-weight: 700; font-size: 0.8125rem; cursor: pointer; border: none; }
                .btn.primary { background: #2563eb; color: white; }
                .btn.secondary { background: #f1f5f9; color: #475569; }
                .btn.warning { background: #fff7ed; color: #9a3412; }
                .btn.danger { background: #dc2626; color: white; }
                .btn:hover { opacity: 0.9; }

                .badge { padding: 0.25rem 0.5rem; border-radius: 4px; font-size: 0.75rem; font-weight: 800; margin-right: 0.5rem; }
                .badge.primary { background: #1e293b; color: white; }
                .badge.secondary { background: #e2e8f0; color: #475569; }

                .modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(15, 23, 42, 0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 2rem; }
                .modal { width: 450px; padding: 2rem; background: white; border-radius: 12px; }
                .modal h3 { margin: 0 0 0.5rem 0; font-size: 1.125rem; font-weight: 800; color: #0f172a; }
                .modal textarea { width: 100%; height: 120px; margin-top: 1rem; padding: 0.75rem; border: 1px solid #e2e8f0; border-radius: 8px; resize: none; font-size: 0.8125rem; }
            `}</style>
        </div>
    );
};
