'use client';

import React, { useState, useEffect } from 'react';
import { tradeApi } from '../api/tradeApi';
import { 
    History, 
    User, 
    Activity, 
    ArrowRight,
    Terminal
} from 'lucide-react';
import Link from 'next/link';

// ABOUTME: GlobalTransactionLog provides a high-fidelity audit trail for all trade transactions.
// ABOUTME: Implements REQ-NAV-01.3 with priority-aware sorting and user attribution.

export const GlobalTransactionLog: React.FC = () => {
    const [auditLogs, setAuditLogs] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [filterPriority, setFilterPriority] = useState('');

    useEffect(() => {
        const loadLogs = async () => {
            setLoading(true);
            try {
                const data = await tradeApi.getGlobalAuditLogs(filterPriority);
                setAuditLogs(data.auditLogList || []);
            } catch (error) {
                console.error('Failed to load global audit logs:', error);
            } finally {
                setLoading(false);
            }
        };
        loadLogs();
    }, [filterPriority]);

    const getPriorityBadgeStyle = (priorityId: string) => {
        switch (priorityId) {
            case 'TX_PRIO_URGENT': return { bg: '#fee2e2', text: '#ef4444', label: 'Urgent' };
            case 'TX_PRIO_HIGH': return { bg: '#ffedd5', text: '#f97316', label: 'High' };
            default: return { bg: '#f1f5f9', text: '#64748b', label: priorityId?.replace('TX_PRIO_', '') || 'Low' };
        }
    };

    return (
        <div className="global-audit-container">
            <header className="audit-header">
                <div className="header-info">
                    <h2>Global Transaction Log</h2>
                    <p>Cross-instrument audit trail of all operational changes and authorizations.</p>
                </div>
                <div className="audit-filters">
                    <select value={filterPriority} onChange={(e) => setFilterPriority(e.target.value)} className="audit-select">
                        <option value="">All Priorities</option>
                        <option value="TX_PRIO_URGENT">Urgent Only</option>
                        <option value="TX_PRIO_HIGH">High Priority</option>
                    </select>
                </div>
            </header>

            <div className="audit-list">
                {loading ? (
                    <div className="loading-state">Synchronizing global audit trail...</div>
                ) : auditLogs.length === 0 ? (
                    <div className="empty-state">No audit records found.</div>
                ) : (
                    auditLogs.map((log, index) => {
                        const priority = getPriorityBadgeStyle(log.priorityEnumId);
                        return (
                            <div key={log.auditId || index} className="audit-item premium-card">
                                <div className="audit-metadata">
                                    <div className="timestamp-group">
                                        <History size={14} />
                                        <span>{new Date(log.timestamp).toLocaleString()}</span>
                                    </div>
                                    <span className="priority-tag" style={{ background: priority.bg, color: priority.text }}>
                                        {priority.label}
                                    </span>
                                </div>
                                
                                <div className="audit-main">
                                    <div className="action-identity">
                                        <div className="user-info">
                                            <div className="user-avatar">{log.userId?.charAt(0) || 'U'}</div>
                                            <span className="user-id">{log.userId}</span>
                                        </div>
                                        <div className="action-type">
                                            <Activity size={14} />
                                            <span>{log.actionEnumId?.replace('AUDIT_', '') || 'UPDATE'}</span>
                                        </div>
                                    </div>

                                    <div className="instrument-link">
                                        <span className="ref-label">Reference:</span>
                                        <span className="ref-value">{log.transactionRef || log.instrumentId}</span>
                                        <Link href={`/import-lc/details?id=${log.instrumentId}`} className="deep-link">
                                            <ArrowRight size={14} />
                                        </Link>
                                    </div>
                                </div>

                                <div className="audit-payload">
                                    <div className="payload-header">
                                        <Terminal size={12} />
                                        <span>Delta Transformation (Proposed vs. Current)</span>
                                    </div>
                                    <pre className="payload-json">{log.snapshotDeltaJSON || log.deltaJson}</pre>
                                </div>
                            </div>
                        );
                    })
                )}
            </div>

            <style jsx>{`
                .global-audit-container { display: flex; flex-direction: column; gap: 1.5rem; }
                .audit-header { display: flex; justify-content: space-between; align-items: flex-start; }
                .header-info h2 { font-size: 1.25rem; font-weight: 700; color: #1e293b; margin: 0 0 0.25rem 0; }
                .header-info p { font-size: 0.8125rem; color: #64748b; margin: 0; }
                
                .audit-select { background: white; border: 1px solid #e2e8f0; border-radius: 6px; padding: 0.5rem 1rem; font-size: 0.8125rem; font-weight: 600; color: #1e293b; outline: none; }
                
                .audit-list { display: flex; flex-direction: column; gap: 1rem; }
                .audit-item { background: white; padding: 1.25rem; display: flex; flex-direction: column; gap: 1rem; }
                
                .audit-metadata { display: flex; justify-content: space-between; align-items: center; }
                .timestamp-group { display: flex; align-items: center; gap: 0.5rem; font-size: 0.75rem; color: #94a3b8; font-weight: 500; }
                .priority-tag { font-size: 0.65rem; font-weight: 800; padding: 2px 8px; border-radius: 4px; text-transform: uppercase; }
                
                .audit-main { display: flex; justify-content: space-between; align-items: center; border-top: 1px solid #f1f5f9; border-bottom: 1px solid #f1f5f9; padding: 0.75rem 0; }
                .action-identity { display: flex; align-items: center; gap: 1.5rem; }
                .user-info { display: flex; align-items: center; gap: 0.5rem; }
                .user-avatar { width: 24px; height: 24px; border-radius: 50%; background: #e0e7ff; color: #4338ca; display: flex; align-items: center; justify-content: center; font-size: 0.7rem; font-weight: 800; }
                .user-id { font-size: 0.875rem; font-weight: 600; color: #1e293b; }
                .action-type { display: flex; align-items: center; gap: 0.5rem; font-size: 0.75rem; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.05em; }
                
                .instrument-link { display: flex; align-items: center; gap: 0.5rem; }
                .ref-label { font-size: 0.75rem; color: #94a3b8; }
                .ref-value { font-size: 0.875rem; font-weight: 700; color: #2563eb; }
                .deep-link { padding: 4px; border-radius: 4px; color: #94a3b8; transition: all 0.2s; }
                .deep-link:hover { color: #2563eb; background: #eff6ff; }
                
                .audit-payload { background: #f8fafc; border-radius: 8px; padding: 1rem; border: 1px solid #f1f5f9; }
                .payload-header { display: flex; align-items: center; gap: 0.5rem; font-size: 0.7rem; font-weight: 700; color: #64748b; text-transform: uppercase; margin-bottom: 0.5rem; }
                .payload-json { margin: 0; font-family: 'JetBrains Mono', monospace; font-size: 0.75rem; color: #334155; white-space: pre-wrap; overflow-x: auto; }
                
                .loading-state, .empty-state { text-align: center; padding: 3rem; color: #64748b; font-size: 0.875rem; background: #f8fafc; border-radius: 12px; border: 1px dashed #e2e8f0; }
                .premium-card { background: white; border: 1px solid #e2e8f0; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
            `}</style>
        </div>
    );
};
