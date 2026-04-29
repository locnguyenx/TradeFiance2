'use client';
import React, { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { CheckerAuthorization } from './CheckerAuthorization';
import { QueueItem } from '../api/types';
import { tradeApi } from '../api/tradeApi';
import { useRouter } from 'next/navigation';

// ABOUTME: Global Checker Queue implementing REQ-UI-CMN-02.
// ABOUTME: Central inbox for second-pair-of-eyes authorization across all Trade modules.

interface CheckersQueueProps {
    items?: QueueItem[];
    userTier?: string;
}

export const CheckersQueue: React.FC<CheckersQueueProps> = ({ items: initialItems, userTier = 'TIER_1' }) => {
    const [items, setItems] = useState<QueueItem[]>(initialItems || []);
    const [loading, setLoading] = useState(!initialItems);
    const [filter, setFilter] = useState<'ALL' | 'HIGH' | 'SLA'>('ALL');
    const [selectedTransactionId, setSelectedTransactionId] = useState<string | null>(null);
    const [mounted, setMounted] = useState(false);
    const router = useRouter();

    useEffect(() => {
        setMounted(true);
        if (!initialItems) {
            setLoading(true);
            tradeApi.getApprovals().then(res => {
                setItems(res.approvalsList || []);
                setLoading(false);
            });
        }
    }, [initialItems]);

    const filteredItems = items.filter(item => {
        if (filter === 'HIGH') return item.priorityEnumId === 'URGENT' || item.priorityEnumId === 'EXPRESS';
        if (filter === 'SLA') return (item.timeInQueue || '').includes('h') && parseInt(item.timeInQueue || '0') > 4;
        return true;
    });

    const priorityWeight: Record<string, number> = {
        'URGENT': 4,
        'HIGH': 3,
        'MEDIUM': 2,
        'LOW': 1
    };

    const sortedItems = [...filteredItems].sort((a, b) => {
        const weightA = priorityWeight[a.priorityEnumId] || 0;
        const weightB = priorityWeight[b.priorityEnumId] || 0;
        if (weightB !== weightA) return weightB - weightA;
        return (b.timeInQueue || '').localeCompare(a.timeInQueue || '');
    });

    const slaAlertCount = items.filter(item => (item.timeInQueue || '').includes('h') && parseInt(item.timeInQueue || '0') > 4).length;

    if (loading) return <div className="p-8 text-center">Loading Approvals Queue...</div>;

    return (
        <div className="queue-container">
            <header className="queue-header">
                <div className="header-text">
                    <h1>Global Checker Queue</h1>
                    <div className="kpi-banner">
                        <span className="kpi-tag tier-tag">Your Authority: {userTier.replace('_', ' ')}</span>
                        <span className="kpi-tag sla-tag">{slaAlertCount} SLA Alerts Pending</span>
                    </div>
                </div>
                <div className="queue-filters">
                    <button className={`filter-chip ${filter === 'ALL' ? 'active' : ''}`} onClick={() => setFilter('ALL')}>All Modules</button>
                    <button className={`filter-chip ${filter === 'HIGH' ? 'active' : ''}`} onClick={() => setFilter('HIGH')}>High Priority</button>
                    <button className={`filter-chip ${filter === 'SLA' ? 'active' : ''}`} onClick={() => setFilter('SLA')}>SLA Alerts</button>
                </div>
            </header>

            <section className="queue-table-wrapper premium-card">
                <table className="queue-table">
                    <thead>
                        <tr>
                            <th>Priority</th>
                            <th>Transaction Ref</th>
                            <th>Module</th>
                            <th>Action</th>
                            <th>Amount</th>
                            <th>Maker</th>
                            <th>Time in Queue</th>
                            <th>Details</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {sortedItems.map(item => (
                            <tr key={item.transactionId} className="clickable-row" onClick={() => router.push(`/transactions/details?id=${item.transactionId}`)}>
                                <td>
                                    <span className={`priority-badge ${(item.priorityEnumId || 'normal').toLowerCase()}`}>
                                        {item.priorityEnumId}
                                    </span>
                                </td>
                                <td className="ref-cell">
                                    {item.transactionRef}
                                    {item.transactionStatusId === 'TXN_PARTIAL_APPROVED' && (
                                        <div className="status-label partial">PARTIAL APPROVED</div>
                                    )}
                                </td>
                                <td><span className="module-tag">{item.module}</span></td>
                                <td><span className="action-tag">{item.action}</span></td>
                                <td className="amount-cell">{(item.baseEquivalentAmount || 0).toLocaleString()}</td>
                                <td>{item.makerUserId}</td>
                                <td>
                                    <div className={`sla-timer`}>
                                        {item.timeInQueue}
                                    </div>
                                </td>
                                <td>
                                    <button 
                                        className="btn-icon secondary"
                                        title="View Details"
                                        onClick={(e) => { e.stopPropagation(); router.push(`/transactions/details?id=${item.transactionId}`); }}
                                    >
                                        👁️
                                    </button>
                                </td>
                                <td>
                                    <button 
                                        className="authorize-btn"
                                        onClick={(e) => { e.stopPropagation(); setSelectedTransactionId(item.transactionId); }}
                                    >
                                        Authorize
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </section>

            {mounted && selectedTransactionId && createPortal(
                <div className="auth-modal-overlay" onClick={() => setSelectedTransactionId(null)}>
                    <div className="auth-modal-content premium-card" onClick={e => e.stopPropagation()}>
                        <button className="close-modal" onClick={() => setSelectedTransactionId(null)}>✕</button>
                        <CheckerAuthorization transactionId={selectedTransactionId} />
                    </div>
                </div>,
                document.body
            )}

            <style jsx>{`
                .queue-container { display: flex; flex-direction: column; gap: 2rem; padding: 1rem; }
                .queue-header { display: flex; justify-content: space-between; align-items: flex-end; }
                .header-text h1 { margin: 0; font-size: 1.875rem; font-weight: 800; color: #0f172a; }
                .kpi-banner { display: flex; gap: 1rem; margin-top: 0.5rem; }
                .kpi-tag { font-size: 0.75rem; font-weight: 700; padding: 0.25rem 0.75rem; border-radius: 9999px; }
                .tier-tag { background: #eff6ff; color: #1e40af; border: 1px solid #bfdbfe; }
                .sla-tag { background: #fff1f2; color: #9f1239; border: 1px solid #fecdd3; }
                
                .queue-filters { display: flex; gap: 0.75rem; }
                .filter-chip { padding: 0.5rem 1rem; border-radius: 9999px; border: 1px solid #e2e8f0; background: white; color: #475569; font-size: 0.8125rem; font-weight: 600; cursor: pointer; transition: all 0.2s; }
                .filter-chip:hover { border-color: #cbd5e1; background: #f8fafc; }
                .filter-chip.active { background: #1e293b; color: white; border-color: #1e293b; }

                .queue-table-wrapper { overflow-x: auto; background: white; border-radius: 12px; }
                .queue-table { width: 100%; border-collapse: collapse; }
                .queue-table th { text-align: left; background: #f8fafc; padding: 1rem 1.5rem; font-size: 0.75rem; font-weight: 700; color: #64748b; text-transform: uppercase; border-bottom: 1px solid #e2e8f0; }
                .queue-table td { padding: 1.25rem 1.5rem; border-bottom: 1px solid #f1f5f9; font-size: 0.875rem; color: #1e293b; }
                
                .clickable-row { cursor: pointer; transition: background 0.2s; }
                .clickable-row:hover { background: #f8fafc; }
                
                .ref-cell { font-weight: 700; color: #2563eb; display: flex; flex-direction: column; gap: 0.25rem; }
                .status-label { font-size: 0.625rem; font-weight: 800; padding: 0.125rem 0.375rem; border-radius: 2px; width: fit-content; }
                .status-label.partial { background: #fef3c7; color: #92400e; border: 1px solid #fde68a; }
                
                .amount-cell { font-family: 'JetBrains Mono', monospace; font-weight: 600; }
                .module-tag { font-size: 0.75rem; font-weight: 700; background: #f1f5f9; color: #475569; padding: 0.25rem 0.5rem; border-radius: 4px; }
                .action-tag { font-size: 0.75rem; font-weight: 500; color: #64748b; }
                
                .priority-badge { font-size: 0.7rem; font-weight: 800; text-transform: uppercase; padding: 0.25rem 0.5rem; border-radius: 4px; }
                .priority-badge.urgent { background: #fef2f2; color: #dc2626; border: 1px solid #fecaca; }
                .priority-badge.express { background: #fff7ed; color: #ea580c; border: 1px solid #ffedd5; }
                .priority-badge.normal { background: #f0f9ff; color: #0284c7; border: 1px solid #e0f2fe; }

                .sla-timer { font-weight: 700; color: #475569; }

                .authorize-btn { background: #2563eb; color: white; border: none; padding: 0.5rem 1rem; border-radius: 6px; font-weight: 600; cursor: pointer; transition: background 0.2s; white-space: nowrap; }
                .authorize-btn:hover { background: #1d4ed8; }

                .btn-icon { background: #f1f5f9; border: 1px solid #e2e8f0; border-radius: 6px; padding: 0.5rem; cursor: pointer; font-size: 1rem; transition: all 0.2s; }
                .btn-icon:hover { background: #e2e8f0; border-color: #cbd5e1; }
                .btn-icon.secondary { color: #64748b; }

                .auth-modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(15, 23, 42, 0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 2rem; width: 100vw; height: 100vh; }
                .auth-modal-content { position: relative; width: 100%; max-width: 1200px; max-height: 90vh; overflow: hidden; background: white; display: flex; flex-direction: column; }
                .close-modal { position: absolute; top: 1rem; right: 1rem; z-index: 1001; background: none; border: none; font-size: 2rem; color: #94a3b8; cursor: pointer; line-height: 1; }
                .close-modal:hover { color: #1e293b; }

                .premium-card { background: white; border: 1px solid #e2e8f0; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
            `}</style>
        </div>
    );
};
