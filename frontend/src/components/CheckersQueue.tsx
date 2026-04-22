'use client';

import React, { useState } from 'react';
import { CheckerAuthorization } from './CheckerAuthorization';

// ABOUTME: Global Checker Queue implementing REQ-UI-CMN-02.
// ABOUTME: Central inbox for second-pair-of-eyes authorization across all Trade modules.

interface QueuedTransaction {
    id: string;
    ref: string;
    module: string;
    submitter: string;
    amount: string;
    status: string;
    priority: 'High' | 'Normal';
    slaMinutesRemaining: number;
}

const mockQueue: QueuedTransaction[] = [
    { id: 'TX001', ref: 'IMLC/2026/001', module: 'Import LC', submitter: 'J. Smith', amount: 'USD 500,000.00', status: 'Pending Authorisation', priority: 'High', slaMinutesRemaining: 45 },
    { id: 'TX002', ref: 'AMND/2026/004', module: 'Import LC', submitter: 'L. Doe', amount: 'N/A (Amendment)', status: 'Pending Authorisation', priority: 'Normal', slaMinutesRemaining: 120 },
    { id: 'TX003', ref: 'ISU/FAC/99', module: 'Facilities', submitter: 'Admin', amount: 'USD 10,000,000.00', status: 'Pending Authorisation', priority: 'High', slaMinutesRemaining: 15 },
];

export const CheckersQueue: React.FC = () => {
    const [selectedInstrumentId, setSelectedInstrumentId] = useState<string | null>(null);

    return (
        <div className="queue-container">
            <header className="queue-header">
                <div className="header-text">
                    <h1>Global Checker Queue</h1>
                    <p>You have {mockQueue.length} items requiring immediate attention.</p>
                </div>
                <div className="queue-filters">
                    <button className="filter-chip active">All Modules</button>
                    <button className="filter-chip">High Priority</button>
                    <button className="filter-chip">SLA Alerts</button>
                </div>
            </header>

            <section className="queue-table-wrapper premium-card">
                <table className="queue-table">
                    <thead>
                        <tr>
                            <th>Priority</th>
                            <th>Transaction Ref</th>
                            <th>Module</th>
                            <th>Status</th>
                            <th>Amount</th>
                            <th>Submitter</th>
                            <th>SLA</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {mockQueue.map(item => (
                            <tr key={item.id} className="clickable-row" onClick={() => setSelectedInstrumentId(item.id)}>
                                <td>
                                    <span className={`priority-badge ${item.priority.toLowerCase()}`}>
                                        {item.priority}
                                    </span>
                                </td>
                                <td className="ref-cell">{item.ref}</td>
                                <td><span className="module-tag">{item.module}</span></td>
                                <td><span className="status-tag">{item.status}</span></td>
                                <td className="amount-cell">{item.amount}</td>
                                <td>{item.submitter}</td>
                                <td>
                                    <div className={`sla-timer ${item.slaMinutesRemaining < 30 ? 'critical' : ''}`}>
                                        {Math.floor(item.slaMinutesRemaining / 60)}h {item.slaMinutesRemaining % 60}m
                                    </div>
                                </td>
                                <td>
                                    <button 
                                        className="authorize-btn"
                                        onClick={(e) => { e.stopPropagation(); setSelectedInstrumentId(item.id); }}
                                    >
                                        Authorize
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </section>

            {selectedInstrumentId && (
                <div className="auth-modal-overlay">
                    <div className="auth-modal-content premium-card">
                        <button className="close-modal" onClick={() => setSelectedInstrumentId(null)}>×</button>
                        <CheckerAuthorization instrumentId={selectedInstrumentId} />
                    </div>
                </div>
            )}

            <style jsx>{`
                .queue-container { display: flex; flex-direction: column; gap: 2rem; padding: 1rem; }
                .queue-header { display: flex; justify-content: space-between; align-items: flex-end; }
                .header-text h1 { margin: 0; font-size: 1.875rem; font-weight: 800; color: #0f172a; }
                .header-text p { margin: 0.5rem 0 0 0; color: #64748b; font-size: 0.875rem; }

                .queue-filters { display: flex; gap: 0.75rem; }
                .filter-chip { padding: 0.5rem 1rem; border-radius: 9999px; border: 1px solid #e2e8f0; background: white; color: #475569; font-size: 0.8125rem; font-weight: 600; cursor: pointer; transition: all 0.2s; }
                .filter-chip:hover { border-color: #cbd5e1; background: #f8fafc; }
                .filter-chip.active { background: #1e293b; color: white; border-color: #1e293b; }

                .queue-table-wrapper { overflow: hidden; background: white; }
                .queue-table { width: 100%; border-collapse: collapse; }
                .queue-table th { text-align: left; background: #f8fafc; padding: 1rem 1.5rem; font-size: 0.75rem; font-weight: 700; color: #64748b; text-transform: uppercase; border-bottom: 1px solid #e2e8f0; }
                .queue-table td { padding: 1.25rem 1.5rem; border-bottom: 1px solid #f1f5f9; font-size: 0.875rem; color: #1e293b; }
                
                .clickable-row { cursor: pointer; transition: background 0.2s; }
                .clickable-row:hover { background: #f8fafc; }
                
                .ref-cell { font-weight: 700; color: #2563eb; }
                .amount-cell { font-family: 'JetBrains Mono', monospace; font-weight: 600; }
                .module-tag { font-size: 0.75rem; font-weight: 700; background: #f1f5f9; color: #475569; padding: 0.25rem 0.5rem; border-radius: 4px; }
                
                .priority-badge { font-size: 0.7rem; font-weight: 800; text-transform: uppercase; padding: 0.25rem 0.5rem; border-radius: 4px; }
                .priority-badge.high { background: #fef2f2; color: #dc2626; }
                .priority-badge.normal { background: #f0f9ff; color: #0284c7; }

                .sla-timer { font-weight: 700; color: #059669; }
                .sla-timer.critical { color: #dc2626; animation: pulse 2s infinite; }

                @keyframes pulse {
                    0% { opacity: 1; }
                    50% { opacity: 0.6; }
                    100% { opacity: 1; }
                }

                .authorize-btn { background: #2563eb; color: white; border: none; padding: 0.5rem 1rem; border-radius: 6px; font-weight: 600; cursor: pointer; transition: background 0.2s; }
                .authorize-btn:hover { background: #1d4ed8; }

                .auth-modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(15, 23, 42, 0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 2rem; }
                .auth-modal-content { position: relative; width: 100%; max-width: 1200px; max-height: 90vh; overflow: hidden; background: white; }
                .close-modal { position: absolute; top: 1rem; right: 1rem; z-index: 1001; background: none; border: none; font-size: 2rem; color: #94a3b8; cursor: pointer; line-height: 1; }
                .close-modal:hover { color: #1e293b; }

                .premium-card { background: white; border: 1px solid #e2e8f0; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
            `}</style>
        </div>
    );
};
