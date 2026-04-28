'use client';

import React, { useState, useEffect } from 'react';
import { tradeApi } from '../api/tradeApi';
import { TradeTransaction } from '../api/types';

// ABOUTME: Unified narrative timeline merging business transactions and system events (REQ-UTN-01).
// ABOUTME: Provides in-timeline actionability for workflow nodes.

interface TimelineEvent {
    id: string;
    type: 'TRANSACTION' | 'AUDIT' | 'SYSTEM';
    timestamp: string;
    title: string;
    user: string;
    status: string;
    statusId: string;
    data?: any;
    priority?: string;
    canAction?: boolean;
}

interface InstrumentTimelineProps {
    instrumentId: string;
    onActionComplete?: () => void;
}

export const InstrumentTimeline: React.FC<InstrumentTimelineProps> = ({ instrumentId, onActionComplete }) => {
    const [events, setEvents] = useState<TimelineEvent[]>([]);
    const [loading, setLoading] = useState(true);

    const loadTimeline = async () => {
        setLoading(true);
        try {
            const [txns, audits] = await Promise.all([
                tradeApi.getInstrumentTransactions(instrumentId),
                tradeApi.getAuditLogs(instrumentId)
            ]);

            const merged: TimelineEvent[] = [
                ...txns.transactionList.map(t => ({
                    id: t.transactionId,
                    type: 'TRANSACTION' as const,
                    timestamp: t.makerTimestamp,
                    title: t.transactionTypeEnumId.replace('TXNT_', '').replace('_', ' '),
                    user: t.makerUserId,
                    status: t.transactionStatusId.replace('TXN_', '').replace('_', ' '),
                    statusId: t.transactionStatusId,
                    priority: t.priorityEnumId,
                    canAction: t.transactionStatusId === 'TXN_SUBMITTED' || t.transactionStatusId === 'TXN_PARTIAL_APPROVED',
                    data: t
                })),
                ...(audits.auditLogList || []).map((a: any) => ({
                    id: a.auditLogId,
                    type: (a.actionName.includes('SWIFT') || a.actionName.includes('ACK')) ? 'SYSTEM' as const : 'AUDIT' as const,
                    timestamp: a.timestamp,
                    title: a.actionName,
                    user: a.changedByUserId,
                    status: 'RECORDED',
                    statusId: 'AUDIT_RECORDED',
                    data: a.deltaJson ? JSON.parse(a.deltaJson) : null
                }))
            ];

            // Sort reverse chronological
            merged.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
            setEvents(merged);
        } catch (e) {
            console.error("Timeline load failed", e);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadTimeline();
    }, [instrumentId]);

    const handleAuthorize = async (txnId: string) => {
        await tradeApi.authorize(txnId);
        loadTimeline();
        if (onActionComplete) onActionComplete();
    };

    if (loading) return <div className="p-4 text-center text-slate-400">Loading narrative...</div>;

    return (
        <div className="timeline-wrapper">
            <div className="timeline-line"></div>
            {events.map((event, idx) => (
                <div key={event.id} className={`timeline-node ${event.type.toLowerCase()}`}>
                    <div className="node-marker">
                        {event.type === 'TRANSACTION' && <span className="icon">💰</span>}
                        {event.type === 'SYSTEM' && <span className="icon">⚙️</span>}
                        {event.type === 'AUDIT' && <span className="icon">🔍</span>}
                    </div>
                    
                    <div className="node-content premium-card">
                        <header className="node-header">
                            <div className="flex justify-between items-start">
                                <div>
                                    <h5 className="node-title">{event.title}</h5>
                                    <p className="node-meta">
                                        {new Date(event.timestamp).toLocaleString()} • {event.user}
                                    </p>
                                </div>
                                <div className="node-badges">
                                    <span className={`status-badge ${event.statusId.toLowerCase()}`}>
                                        {event.status}
                                    </span>
                                    {event.priority && event.priority !== 'NORMAL' && (
                                        <span className={`priority-badge ${event.priority.toLowerCase()}`}>
                                            {event.priority}
                                        </span>
                                    )}
                                </div>
                            </div>
                        </header>

                        {event.canAction && (
                            <div className="node-actions mt-4 p-3 border-t border-slate-100 flex gap-2">
                                <button className="btn-sm primary" onClick={() => handleAuthorize(event.id)}>Authorize</button>
                                <button className="btn-sm secondary">Reject</button>
                            </div>
                        )}

                        {event.type === 'AUDIT' && event.data && (
                            <div className="node-details mt-2">
                                <pre className="text-[10px] bg-slate-50 p-2 rounded overflow-x-auto">
                                    {JSON.stringify(event.data, null, 2)}
                                </pre>
                            </div>
                        )}
                    </div>
                </div>
            ))}

            <style jsx>{`
                .timeline-wrapper { position: relative; padding: 1rem 0 1rem 2rem; display: flex; flex-direction: column; gap: 2rem; }
                .timeline-line { position: absolute; left: 0.75rem; top: 0; bottom: 0; width: 2px; background: #e2e8f0; }
                
                .timeline-node { position: relative; }
                .node-marker { position: absolute; left: -1.875rem; width: 1.5rem; height: 1.5rem; background: white; border: 2px solid #e2e8f0; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 0.75rem; z-index: 1; }
                
                .node-content { background: white; padding: 1.25rem; border-radius: 12px; transition: all 0.2s; border: 1px solid #e2e8f0; }
                .node-content:hover { border-color: #cbd5e1; box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1); }
                
                .transaction .node-marker { border-color: #2563eb; }
                .system .node-marker { border-color: #10b981; }
                .audit .node-marker { border-color: #94a3b8; }
                
                .node-title { margin: 0; font-size: 0.9375rem; font-weight: 700; color: #1e293b; text-transform: capitalize; }
                .node-meta { margin: 0.25rem 0 0 0; font-size: 0.75rem; color: #64748b; font-weight: 500; }
                
                .status-badge { font-size: 0.625rem; font-weight: 800; padding: 0.125rem 0.5rem; border-radius: 4px; text-transform: uppercase; }
                .txn_submitted { background: #fffbeb; color: #92400e; }
                .txn_approved { background: #dcfce7; color: #166534; }
                .audit_recorded { background: #f1f5f9; color: #475569; }
                
                .priority-badge { font-size: 0.625rem; font-weight: 800; padding: 0.125rem 0.5rem; border-radius: 4px; background: #fee2e2; color: #991b1b; margin-left: 0.5rem; }
                
                .btn-sm { font-size: 0.75rem; font-weight: 700; padding: 0.375rem 0.75rem; border-radius: 4px; border: none; cursor: pointer; }
                .btn-sm.primary { background: #2563eb; color: white; }
                .btn-sm.secondary { background: #f1f5f9; color: #475569; }
                
                .premium-card { background: white; border: 1px solid #e2e8f0; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
            `}</style>
        </div>
    );
};
