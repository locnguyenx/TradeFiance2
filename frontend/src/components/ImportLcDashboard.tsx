'use client';

import React, { useEffect, useState } from 'react';
import { tradeApi, ImportLc, Kpis } from '../api/tradeApi';

// ABOUTME: Import LC Dashboard implementing REQ-UI-IMP-02.
// ABOUTME: High-density data grid for operational tracking with SLA timer and KPI cards.

export const ImportLcDashboard: React.FC = () => {
    const [lcs, setLcs] = useState<ImportLc[]>([]);
    const [kpis, setKpis] = useState<Kpis | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [openMenuId, setOpenMenuId] = useState<string | null>(null);

    const toggleMenu = (id: string, e: React.MouseEvent) => {
        e.stopPropagation();
        setOpenMenuId(openMenuId === id ? null : id);
    };

    useEffect(() => {
        const closeMenu = () => setOpenMenuId(null);
        window.addEventListener('click', closeMenu);
        return () => window.removeEventListener('click', closeMenu);
    }, []);

    useEffect(() => {
        Promise.all([tradeApi.getImportLcs(), tradeApi.getKpis()])
            .then(([lcData, kpiData]) => {
                setLcs(lcData.lcList);
                setKpis(kpiData);
                setLoading(false);
            })
            .catch(err => {
                setError("System Temporarily Unavailable. Please contact Trade Support.");
                setLoading(false);
            });
    }, []);

    if (loading) return (
        <div className="skeleton-dashboard">
            <div className="skeleton-card"></div>
            <div className="skeleton-table"></div>
        </div>
    );

    if (error) return <div className="error-view premium-card">{error}</div>;

    return (
        <div className="dashboard-container">
            <section className="kpi-grid">
                <div className="kpi-item premium-card">
                    <span className="kpi-label">Drafts Awaiting My Submission</span>
                    <span className="kpi-value">{kpis?.pendingDrafts || 0}</span>
                </div>
                <div className="kpi-item premium-card urgent">
                    <span className="kpi-label">LCs Expiring within 7 Days</span>
                    <span className="kpi-value">{kpis?.expiringSoon || 0}</span>
                </div>
                <div className="kpi-item premium-card warning">
                    <span className="kpi-label">Discrepant Presentations Awaiting Waiver</span>
                    <span className="kpi-value">{kpis?.discrepantDocs || 0}</span>
                </div>
            </section>

            <section className="transaction-section premium-card">
                <header className="table-header">
                    <h2>Active Transaction Data Table</h2>
                    <div className="table-filters">
                        <select aria-label="Status Filter" className="filter-select">
                            <option>Status: All</option>
                            <option>Draft</option>
                            <option>Issued</option>
                            <option>Docs Received</option>
                        </select>
                    </div>
                </header>

                <div className="table-responsive">
                    <table className="trade-table">
                        <thead>
                            <tr>
                                <th>Ref No</th>
                                <th>Applicant</th>
                                <th>Beneficiary</th>
                                <th>CCY</th>
                                <th>Amount</th>
                                <th>Expiry Date</th>
                                <th>Status</th>
                                <th>SLA Timer</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {lcs.length > 0 ? (
                                lcs.map(lc => (
                                    <tr key={lc.instrumentId}>
                                        <td className="font-bold">{lc.transactionRef}</td>
                                        <td>{lc.applicantName || '---'}</td>
                                        <td>{lc.beneficiaryName || '---'}</td>
                                        <td>{lc.currency || 'USD'}</td>
                                        <td>{(lc.amount || 0).toLocaleString()}</td>
                                        <td>{lc.expiryDate || '---'}</td>
                                        <td>
                                            <span className={`status-tag ${lc.businessStateId}`}>
                                                {lc.businessStateId?.replace('LC_', '')}
                                            </span>
                                        </td>
                                        <td className={lc.slaDaysRemaining && lc.slaDaysRemaining < 3 ? 'text-urgent' : ''}>
                                            {lc.slaDaysRemaining ? `${lc.slaDaysRemaining} days` : '---'}
                                        </td>
                                        <td>
                                            <div className="action-wrapper">
                                                <button 
                                                    data-testid={`row-action-${lc.instrumentId}`}
                                                    className="action-trigger"
                                                    onClick={(e) => toggleMenu(lc.instrumentId, e)}
                                                >
                                                    •••
                                                </button>
                                                {openMenuId === lc.instrumentId && (
                                                    <div className="dropdown-menu premium-card">
                                                        <button className="menu-item" onClick={() => window.location.href=`/import-lc/amendments?id=${lc.instrumentId}`}>
                                                            New Amendment
                                                        </button>
                                                        <button className="menu-item" onClick={() => window.location.href=`/import-lc/presentations?id=${lc.instrumentId}`}>
                                                            Present Documents
                                                        </button>
                                                        <button className="menu-item" onClick={() => window.location.href=`/import-lc/settlement?id=${lc.instrumentId}`}>
                                                            Initiate Settlement
                                                        </button>
                                                        <button className="menu-item" onClick={() => window.location.href=`/import-lc/shipping-guarantees?id=${lc.instrumentId}`}>
                                                            Shipping Guarantee
                                                        </button>
                                                        <button className="menu-item" onClick={() => window.location.href=`/import-lc/cancellations?id=${lc.instrumentId}`}>
                                                            Request Cancellation
                                                        </button>
                                                    </div>
                                                )}
                                            </div>
                                        </td>
                                    </tr>
                                ))
                            ) : (
                                <tr>
                                    <td colSpan={9} className="empty-state">No active transactions found.</td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </section>

            <style jsx>{`
                .dashboard-container { display: flex; flex-direction: column; gap: 2rem; padding: 1rem; }
                .kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 1.5rem; }
                .kpi-item { display: flex; flex-direction: column; padding: 1.5rem; justify-content: center; }
                .kpi-label { font-size: 0.875rem; color: #64748b; font-weight: 500; margin-bottom: 0.5rem; }
                .kpi-value { font-size: 2rem; font-weight: 700; color: #1e293b; }
                .urgent .kpi-value { color: #dc2626; }
                .warning .kpi-value { color: #d97706; }
                
                .premium-card { background: white; border: 1px solid #e2e8f0; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
                .table-header { display: flex; justify-content: space-between; align-items: center; padding: 1.5rem; border-bottom: 1px solid #f1f5f9; }
                .table-header h2 { font-size: 1.25rem; font-weight: 700; color: #1e293b; }
                
                .trade-table { width: 100%; border-collapse: collapse; font-size: 0.875rem; }
                .trade-table th { background: #f8fafc; text-align: left; padding: 1rem; color: #475569; font-weight: 600; }
                .trade-table td { padding: 1rem; border-bottom: 1px solid #f1f5f9; color: #334155; }
                .font-bold { font-weight: 600; color: #2563eb; }
                .status-tag { padding: 0.25rem 0.625rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 600; text-transform: uppercase; }
                .LC_ISSUED { background: #dcfce7; color: #166534; }
                .LC_DRAFT { background: #f1f5f9; color: #475569; }
                .text-urgent { color: #dc2626; font-weight: 700; }
                
                .action-wrapper { position: relative; }
                .action-trigger { background: none; border: none; cursor: pointer; font-size: 1.25rem; color: #94a3b8; padding: 0.5rem; transition: color 0.2s; }
                .action-trigger:hover { color: #2563eb; }
                
                .dropdown-menu { 
                    position: absolute; right: 0; top: 100%; z-index: 10; min-width: 180px; 
                    background: white; padding: 0.5rem; margin-top: 0.25rem;
                    display: flex; flex-direction: column; overflow: hidden;
                }
                .menu-item { 
                    text-align: left; padding: 0.625rem 1rem; background: none; border: none; 
                    cursor: pointer; font-size: 0.875rem; color: #334155; border-radius: 6px;
                    transition: all 0.2s;
                }
                .menu-item:hover { background: #f1f5f9; color: #2563eb; }
                
                .empty-state { text-align: center; padding: 3rem; color: #94a3b8; }
                
                .filter-select { padding: 0.5rem; border-radius: 6px; border: 1px solid #cbd5e1; font-size: 0.875rem; outline: none; }
            `}</style>
        </div>
    );
};
