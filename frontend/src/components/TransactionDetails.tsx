'use client';

import React, { useState, useEffect } from 'react';
import { tradeApi } from '../api/tradeApi';
import { TradeInstrument, ImportLetterOfCredit, TradeTransaction } from '../api/types';
import { InstrumentDetails } from './InstrumentDetails';
import { Clock, CheckCircle, AlertCircle, User, Calendar, Hash } from 'lucide-react';

// ABOUTME: TransactionDetails provide a high-fidelity readonly view of a specific transaction.
// ABOUTME: Emphasizes delta analysis and maker/checker audit trail.

interface Props {
    transactionId: string;
}

export const TransactionDetails: React.FC<Props> = ({ transactionId }) => {
    const [transaction, setTransaction] = useState<TradeTransaction | null>(null);
    const [instrument, setInstrument] = useState<(TradeInstrument & ImportLetterOfCredit) | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const loadDetail = async () => {
            setLoading(true);
            try {
                const txn = await tradeApi.getTransaction(transactionId);
                setTransaction(txn);
                const inst = await tradeApi.getImportLc(txn.instrumentId);
                setInstrument(inst as any);
            } catch (err) {
                console.error("Error loading transaction detail", err);
            } finally {
                setLoading(false);
            }
        };
        loadDetail();
    }, [transactionId]);

    if (loading) return <div className="p-12 text-center text-slate-500">Loading Transaction Details...</div>;
    if (!transaction || !instrument) return <div className="p-12 text-center text-red-500 font-bold">Transaction record not found.</div>;

    const getStatusStyle = (status: string) => {
        switch (status) {
            case 'TX_APPROVED': return { color: '#059669', bg: '#dcfce7', label: 'Approved', icon: <CheckCircle size={14} /> };
            case 'TX_REJECTED': return { color: '#dc2626', bg: '#fee2e2', label: 'Rejected', icon: <AlertCircle size={14} /> };
            case 'TX_PENDING': return { color: '#d97706', bg: '#fffbeb', label: 'Pending', icon: <Clock size={14} /> };
            default: return { color: '#475569', bg: '#f1f5f9', label: status.replace('TX_', ''), icon: <Clock size={14} /> };
        }
    };

    const statusStyle = getStatusStyle(transaction.transactionStatusId);

    return (
        <div className="transaction-details-container">
            <div className="top-row">
                <header className="details-header premium-card">
                    <div className="header-main">
                        <div className="title-group">
                            <div className="flex items-center gap-3">
                                <h1>Transaction Details</h1>
                                <span className="status-badge" style={{ color: statusStyle.color, backgroundColor: statusStyle.bg }}>
                                    {statusStyle.icon}
                                    {statusStyle.label}
                                </span>
                            </div>
                            <div className="header-data-grid">
                                <div className="data-point">
                                    <label>Transaction Ref</label>
                                    <span className="font-mono font-bold text-blue-600">{transaction.transactionId}</span>
                                </div>
                                <div className="data-point">
                                    <label>Instrument ID</label>
                                    <span className="font-semibold text-slate-700">{instrument.instrumentId}</span>
                                </div>
                                <div className="data-point">
                                    <label>Transaction Type</label>
                                    <span className="type-tag">{transaction.transactionTypeEnumId?.replace('TXN_', '').replace('_', ' ')}</span>
                                </div>
                                <div className="data-point">
                                    <label>Initiation Date</label>
                                    <span className="font-medium text-slate-600">{new Date(transaction.transactionDate).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })}</span>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="audit-belt">
                        <div className="audit-item">
                            <User size={14} />
                            <label>Maker:</label>
                            <span>{transaction.makerUserId}</span>
                        </div>
                        <div className="audit-item">
                            <Calendar size={14} />
                            <label>Initiated At:</label>
                            <span>{new Date(transaction.transactionDate).toLocaleTimeString()}</span>
                        </div>
                    </div>
                </header>

                <div className="workflow-card premium-card">
                    <div className="section-header">
                        <h2>Workflow Status</h2>
                    </div>
                    <div className="workflow-steps">
                        <div className="step-item completed">
                            <div className="step-marker"><CheckCircle size={12} /></div>
                            <div className="step-content">
                                <span className="step-label">Drafted</span>
                                <span className="step-sub">{transaction.makerUserId}</span>
                            </div>
                        </div>
                        <div className={`step-item ${transaction.transactionStatusId !== 'TX_PENDING' ? 'completed' : 'active'}`}>
                            <div className="step-marker">
                                {transaction.transactionStatusId === 'TX_PENDING' ? <div className="pulse" /> : <CheckCircle size={12} />}
                            </div>
                            <div className="step-content">
                                <span className="step-label">Checker Review</span>
                                <span className="step-sub">{transaction.transactionStatusId === 'TX_PENDING' ? 'Pending Action' : 'Completed'}</span>
                            </div>
                        </div>
                        <div className={`step-item ${transaction.transactionStatusId === 'TX_APPROVED' ? 'completed' : ''}`}>
                            <div className="step-marker">
                                {transaction.transactionStatusId === 'TX_APPROVED' ? <CheckCircle size={12} /> : null}
                            </div>
                            <div className="step-content">
                                <span className="step-label">Final Authorization</span>
                                <span className="step-sub">{transaction.transactionStatusId === 'TX_APPROVED' ? transaction.checkerUserId : 'Waiting'}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div className="details-layout">
                <div className="main-content">
                    {transaction.rejectionReason && (
                        <div className="rejection-box mb-6">
                            <label className="text-red-800 font-bold text-xs uppercase mb-1 block">Rejection Reason</label>
                            <p>{transaction.rejectionReason}</p>
                        </div>
                    )}

                    <section className="section-card snapshot-card premium-card">
                        <div className="section-header">
                            <h2>Instrument Snapshot</h2>
                            <span className="text-xs text-slate-400">Values for this transaction</span>
                        </div>
                        
                        <div className="snapshot-grid">
                            <div className="snap-item">
                                <label>Transaction Amount</label>
                                <div className="amt-val">
                                    <span className="ccy">{transaction.proposedCurrencyUomId || instrument.currencyUomId}</span>
                                    <span className={`amt ${transaction.proposedAmount !== instrument.amount ? 'is-delta' : ''}`}>
                                        {transaction.proposedAmount?.toLocaleString(undefined, {minimumFractionDigits: 2}) || instrument.amount?.toLocaleString(undefined, {minimumFractionDigits: 2})}
                                    </span>
                                </div>
                                {transaction.proposedAmount !== instrument.amount && (
                                    <span className="delta-label">Proposed Change</span>
                                )}
                            </div>
                            <div className="snap-item">
                                <label>Instrument Expiry</label>
                                <div className={`val ${transaction.proposedExpiryDate !== instrument.expiryDate ? 'is-delta' : ''}`}>
                                    {transaction.proposedExpiryDate || instrument.expiryDate}
                                </div>
                                {transaction.proposedExpiryDate !== instrument.expiryDate && (
                                    <span className="delta-label">Proposed Change</span>
                                )}
                            </div>
                        </div>
                    </section>

                    <section className="section-card master-data-card premium-card mt-6">
                        <div className="instrument-full-view">
                            <h3 className="text-sm font-bold text-slate-800 mb-4 px-2">Instrument Master Data</h3>
                            <InstrumentDetails instrument={instrument} />
                        </div>
                    </section>
                </div>
            </div>

            <style jsx>{`
                .transaction-details-container { display: flex; flex-direction: column; gap: 1.5rem; }
                .top-row { display: grid; grid-template-columns: 1fr 350px; gap: 1.5rem; align-items: stretch; }
                .details-header { padding: 1.5rem 2rem; background: white; display: flex; flex-direction: column; justify-content: space-between; }
                .header-main { display: flex; justify-content: space-between; align-items: flex-start; }
                h1 { font-size: 1.75rem; font-weight: 800; color: #1e293b; letter-spacing: -0.025em; margin: 0; }
                
                .header-data-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 1rem 2rem; margin-top: 1.25rem; }
                .data-point { display: flex; flex-direction: column; gap: 0.125rem; }
                .data-point label { font-size: 0.625rem; font-weight: 700; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.025em; }
                .data-point span { font-size: 0.875rem; }
                .type-tag { font-weight: 800; color: #2563eb; text-transform: uppercase; font-size: 0.75rem !important; background: #eff6ff; padding: 2px 8px; border-radius: 4px; display: inline-block; width: fit-content; }
                
                .status-badge { display: flex; align-items: center; gap: 0.375rem; padding: 0.25rem 0.75rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 800; text-transform: uppercase; }
                
                .audit-belt { display: flex; gap: 2rem; padding-top: 1.25rem; border-top: 1px solid #f1f5f9; }
                .audit-item { display: flex; align-items: center; gap: 0.5rem; color: #64748b; font-size: 0.8125rem; }
                .audit-item label { font-weight: 600; color: #94a3b8; }
                .audit-item span { color: #1e293b; font-weight: 500; }

                .details-layout { width: 100%; }
                .section-card { background: white; padding: 1.5rem; }
                .section-header { margin-bottom: 1.25rem; }
                .section-header h2 { font-size: 0.75rem; font-weight: 800; color: #64748b; text-transform: uppercase; letter-spacing: 0.05em; margin: 0; }
                
                .snapshot-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }
                .snap-item label { display: block; font-size: 0.65rem; font-weight: 700; color: #94a3b8; text-transform: uppercase; margin-bottom: 0.5rem; }
                .amt-val { display: flex; align-items: baseline; gap: 0.375rem; }
                .ccy { font-weight: 700; color: #94a3b8; font-size: 0.875rem; }
                .amt { font-size: 1.25rem; font-weight: 800; color: #2563eb; font-family: 'JetBrains Mono', monospace; }
                .val { font-size: 1rem; font-weight: 700; color: #1e293b; }

                .amt.is-delta { color: #2563eb; background: #eff6ff; padding: 2px 6px; border-radius: 4px; }
                .val.is-delta { color: #2563eb; background: #eff6ff; padding: 2px 6px; border-radius: 4px; display: inline-block; }
                .delta-label { display: block; font-size: 0.625rem; font-weight: 700; color: #3b82f6; text-transform: uppercase; margin-top: 0.25rem; }

                .rejection-box { background: #fff1f2; border: 1px solid #fecdd3; border-radius: 8px; padding: 1rem 1.25rem; color: #9f1239; font-size: 0.875rem; line-height: 1.5; }

                .workflow-card { background: white; padding: 1.5rem; }
                .workflow-steps { display: flex; flex-direction: column; gap: 0.75rem; position: relative; }
                .workflow-steps::before { content: ''; position: absolute; left: 10px; top: 10px; bottom: 10px; width: 2px; background: #f1f5f9; }
                
                .step-item { display: flex; gap: 1rem; position: relative; z-index: 1; }
                .step-marker { width: 22px; height: 22px; border-radius: 50%; background: white; border: 2px solid #e2e8f0; display: flex; align-items: center; justify-content: center; color: #94a3b8; }
                .step-item.completed .step-marker { background: #dcfce7; border-color: #10b981; color: #10b981; }
                .step-item.active .step-marker { border-color: #2563eb; }
                
                .step-label { display: block; font-size: 0.8125rem; font-weight: 700; color: #1e293b; }
                .step-sub { display: block; font-size: 0.75rem; color: #64748b; }
                
                .pulse { width: 8px; height: 8px; background: #2563eb; border-radius: 50%; animation: pulse 1.5s infinite; }
                @keyframes pulse { 0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(37, 99, 235, 0.7); } 70% { transform: scale(1); box-shadow: 0 0 0 10px rgba(37, 99, 235, 0); } 100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(37, 99, 235, 0); } }

                .mt-6 { margin-top: 1.5rem; }
                .premium-card { border: 1px solid #e2e8f0; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
            `}</style>
        </div>
    );
};
