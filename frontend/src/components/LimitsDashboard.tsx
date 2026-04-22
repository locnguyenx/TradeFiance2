'use client';

import React from 'react';

// ABOUTME: Credit Facilities & Limit Dashboard implementing REQ-UI-CMN-04.
// ABOUTME: Provides real-time visibility into credit headroom and utilization by instrument type.

export const LimitsDashboard: React.FC = () => {
    const [selectedInstrument, setSelectedInstrument] = React.useState<string | null>(null);

    const facilities = [
        { id: 'FAC-IMP-1002', type: 'Import LC', limit: 5000000, used: 3000000, expiry: '2027-01-01' },
        { id: 'FAC-SG-202', type: 'Shipping Guarantee', limit: 1000000, used: 200000, expiry: '2026-06-15' },
    ];

    const transactions = [
        { ref: 'IMLC/2026/001', type: 'Import LC', amount: 500000, date: '2026-04-20' },
        { ref: 'IMLC/2026/002', type: 'Import LC', amount: 100000, date: '2026-04-21' },
        { ref: 'SG/NY/22', type: 'Shipping Guarantee', amount: 200000, date: '2026-04-18' },
    ];

    const filteredTransactions = selectedInstrument 
        ? transactions.filter(t => t.type === selectedInstrument)
        : transactions;

    const totalLimit = 6000000;
    const totalUsed = 3200000;
    const usedPercent = Math.round((totalUsed / totalLimit) * 100);

    return (
        <div className="limits-container">
            <header className="limits-header">
                <h1>Credit Facility & Limit Dashboard</h1>
            </header>

            <section className="overview-grid">
                <div className="exposure-card premium-card">
                    <span className="card-label">Total Facility Limit</span>
                    <span className="card-value">$ {(totalLimit).toLocaleString()}</span>
                </div>
                <div className="exposure-card premium-card warning">
                    <span className="card-label">Total Utilization</span>
                    <span className="card-value">$ {(totalUsed).toLocaleString()}</span>
                    <div className="mini-progress">
                        <div className="progress-fill" style={{ width: `${usedPercent}%` }}></div>
                    </div>
                    <span className="percent-tag">{usedPercent}% Utilized</span>
                </div>
                <div className="exposure-card premium-card highlight">
                    <span className="card-label">Available headroom</span>
                    <span className="card-value text-success">$ {(totalLimit - totalUsed).toLocaleString()}</span>
                </div>
            </section>

            <div className="content-split">
                <section className="facility-section premium-card">
                    <h3>Facility Utilization Details</h3>
                    <div className="facility-list">
                        {facilities.map(f => {
                            const percent = Math.round((f.used / f.limit) * 100);
                            return (
                                <div key={f.id} className="facility-row">
                                    <div className="fac-info">
                                        <span className="fac-id">{f.id}</span>
                                        <span className="fac-type">{f.type}</span>
                                    </div>
                                    <div className="fac-utilization">
                                        <div className="util-bar">
                                            <div className="util-fill" style={{ width: `${percent}%` }}></div>
                                        </div>
                                        <span className="util-text">{percent}%</span>
                                    </div>
                                    <div className="fac-limits">
                                        <span className="limit-text">$ {(f.used).toLocaleString()} / {(f.limit).toLocaleString()}</span>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </section>

                <section className="risk-section premium-card">
                    <header className="risk-header">
                        <h3>Exposure by Instrument</h3>
                        {selectedInstrument && (
                            <button className="clear-filter" onClick={() => setSelectedInstrument(null)}>
                                Close Filter [x]
                            </button>
                        )}
                    </header>
                    <div className="exposure-chart">
                        <div 
                            data-testid="exposure-Import LC"
                            className={`exposure-item clickable ${selectedInstrument === 'Import LC' ? 'active' : ''}`} 
                            onClick={() => setSelectedInstrument('Import LC')}
                        >
                            <span className="inst-label">Import LC</span>
                            <div className="inst-bar" style={{ width: '80%', background: '#3b82f6' }}></div>
                        </div>
                        <div 
                            data-testid="exposure-Shipping Guarantee"
                            className={`exposure-item clickable ${selectedInstrument === 'Shipping Guarantee' ? 'active' : ''}`} 
                            onClick={() => setSelectedInstrument('Shipping Guarantee')}
                        >
                            <span className="inst-label">Shipping Guarantee</span>
                            <div className="inst-bar" style={{ width: '40%', background: '#10b981' }}></div>
                        </div>
                        <div 
                            data-testid="exposure-Export Finance"
                            className={`exposure-item clickable ${selectedInstrument === 'Export Finance' ? 'active' : ''}`} 
                            onClick={() => setSelectedInstrument('Export Finance')}
                        >
                            <span className="inst-label">Export Finance</span>
                            <div className="inst-bar" style={{ width: '10%', background: '#94a3b8' }}></div>
                        </div>
                    </div>

                    <div className="drill-down-table">
                        <h4>{selectedInstrument || 'All'} Exposure Transactions</h4>
                        <table className="admin-table">
                            <thead>
                                <tr>
                                    <th>Transaction Ref</th>
                                    <th>Type</th>
                                    <th>Amount</th>
                                    <th>Date</th>
                                </tr>
                            </thead>
                            <tbody>
                                {filteredTransactions.map(t => (
                                    <tr key={t.ref}>
                                        <td className="font-bold">{t.ref}</td>
                                        <td>{t.type}</td>
                                        <td>$ {t.amount.toLocaleString()}</td>
                                        <td>{t.date}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </section>
            </div>

            <style jsx>{`
                .limits-container { padding: 2rem; display: flex; flex-direction: column; gap: 2rem; }
                .limits-header h1 { font-size: 1.5rem; font-weight: 700; color: #1e293b; margin: 0; }
                
                .overview-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1.5rem; }
                .exposure-card { padding: 1.5rem; display: flex; flex-direction: column; }
                .card-label { font-size: 0.875rem; color: #64748b; font-weight: 500; margin-bottom: 0.5rem; }
                .card-value { font-size: 1.75rem; font-weight: 700; color: #0f172a; }
                .text-success { color: #059669; }
                
                .mini-progress { height: 6px; background: #f1f5f9; border-radius: 3px; margin: 1rem 0 0.5rem 0; overflow: hidden; }
                .progress-fill { height: 100%; background: #f59e0b; border-radius: 3px; }
                .percent-tag { font-size: 0.75rem; font-weight: 600; color: #d97706; }

                .content-split { display: grid; grid-template-columns: 3fr 2fr; gap: 2rem; }
                .facility-section, .risk-section { padding: 1.5rem; }
                .facility-section h3, .risk-section h3 { font-size: 1.125rem; font-weight: 700; color: #334155; margin-top: 0; margin-bottom: 2rem; }

                .facility-list { display: flex; flex-direction: column; gap: 1.5rem; }
                .facility-row { display: grid; grid-template-columns: 2fr 3fr 2fr; align-items: center; gap: 1rem; padding-bottom: 1rem; border-bottom: 1px solid #f8fafc; }
                .fac-info { display: flex; flex-direction: column; }
                .fac-id { font-weight: 600; color: #2563eb; font-size: 0.875rem; }
                .fac-type { font-size: 0.75rem; color: #64748b; }
                
                .util-bar { flex: 1; height: 12px; background: #f1f5f9; border-radius: 6px; overflow: hidden; position: relative; }
                .util-fill { height: 100%; background: linear-gradient(90deg, #3b82f6, #2563eb); border-radius: 6px; }
                .fac-utilization { display: flex; align-items: center; gap: 0.75rem; }
                .util-text { font-size: 0.75rem; font-weight: 700; color: #1e293b; width: 30px; }
                .limit-text { font-size: 0.8125rem; color: #475569; text-align: right; font-weight: 500; }

                .risk-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; }
                .risk-header h3 { margin: 0; }
                .clear-filter { background: #f1f5f9; border: none; color: #475569; padding: 0.25rem 0.75rem; border-radius: 4px; font-size: 0.75rem; font-weight: 700; cursor: pointer; transition: all 0.2s; }
                .clear-filter:hover { background: #e2e8f0; color: #1e293b; }

                .exposure-chart { display: flex; flex-direction: column; gap: 1rem; margin-bottom: 2.5rem; }
                .exposure-item { display: flex; flex-direction: column; gap: 0.5rem; padding: 0.75rem; border-radius: 8px; transition: all 0.2s; }
                .exposure-item.clickable { cursor: pointer; }
                .exposure-item.clickable:hover { background: #f8fafc; transform: translateX(4px); }
                .exposure-item.active { background: #eff6ff; border-left: 4px solid #2563eb; }
                
                .inst-label { font-size: 0.8125rem; font-weight: 600; color: #475569; }
                .inst-bar { height: 8px; border-radius: 4px; }

                .drill-down-table { border-top: 1px solid #f1f5f9; padding-top: 2rem; }
                .drill-down-table h4 { margin-top: 0; font-size: 0.875rem; color: #64748b; text-transform: uppercase; margin-bottom: 1rem; }
                
                .admin-table { width: 100%; border-collapse: collapse; font-size: 0.8125rem; }
                .admin-table th { text-align: left; padding: 0.75rem; color: #64748b; font-weight: 700; border-bottom: 2px solid #f1f5f9; }
                .admin-table td { padding: 0.75rem; border-bottom: 1px solid #f1f5f9; color: #334155; }
                .font-bold { font-weight: 700; color: #2563eb; }

                .premium-card { background: white; border: 1px solid #e2e8f0; border-radius: 12px; box-shadow: 0 1px 2px rgba(0,0,0,0.05); }
                .highlight { border-top: 4px solid #10b981; }
                .warning { border-top: 4px solid #f59e0b; }
            `}</style>
        </div>
    );
};
