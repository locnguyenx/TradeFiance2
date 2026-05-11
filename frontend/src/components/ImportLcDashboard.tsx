'use client';

import React, { useEffect, useState } from 'react';
import { tradeApi, Kpis } from '../api/tradeApi';
import { TradeInstrument, ImportLetterOfCredit } from '../api/types';
import { useRouter } from 'next/navigation';
import { Search, ChevronLeft, ChevronRight } from 'lucide-react';

// ABOUTME: Import LC Dashboard implementing REQ-UI-IMP-02.
// ABOUTME: High-density data grid for operational tracking with SLA timer and KPI cards.

export const ImportLcDashboard: React.FC = () => {
    const [lcs, setLcs] = useState<(TradeInstrument & ImportLetterOfCredit)[]>([]);
    const [kpis, setKpis] = useState<Kpis | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const router = useRouter();
    const [openMenuId, setOpenMenuId] = useState<string | null>(null);
    const [filterStatus, setFilterStatus] = useState<string>('');
    const [filterMaker, setFilterMaker] = useState<string>('');
    const [filterType, setFilterType] = useState<string>('');
    const [filterInstSearch, setFilterInstSearch] = useState<string>('');
    const [makers, setMakers] = useState<string[]>([]);

    const [pageIndex, setPageIndex] = useState(0);
    const [pageSize, setPageSize] = useState(20);
    const [totalCount, setTotalCount] = useState(0);
    const [debouncedInstSearch, setDebouncedInstSearch] = useState(filterInstSearch);

    // Debounce search term
    useEffect(() => {
        const timer = setTimeout(() => setDebouncedInstSearch(filterInstSearch), 500);
        return () => clearTimeout(timer);
    }, [filterInstSearch]);

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
        setLoading(true);
        const params: any = {
            pageIndex,
            pageSize
        };
        if (filterStatus) params.businessStateId = filterStatus;
        if (filterMaker) params.makerUserId = filterMaker;
        if (filterType) params.transactionTypeEnumId = filterType;
        if (debouncedInstSearch) params.instrumentSearch = debouncedInstSearch;

        Promise.all([
            tradeApi.getImportLcs(params), 
            tradeApi.getKpis(),
            tradeApi.getUserAuthorityProfiles()
        ])
            .then(([lcData, kpiData, authData]) => {
                setLcs(lcData?.lcList || []);
                setTotalCount(lcData?.lcListCount || 0);
                setKpis(kpiData || null);
                
                const uniqueMakers = Array.from(new Set(authData.profileList.map(p => p.userId)));
                setMakers(uniqueMakers);
                
                setLoading(false);
            })
            .catch(err => {
                console.error("Dashboard Fetch Error:", err);
                setError("System Temporarily Unavailable. Please contact Trade Support.");
                setLoading(false);
            });
    }, [filterStatus, filterMaker, filterType, debouncedInstSearch, pageIndex, pageSize]);

    // Reset pageIndex when filters change
    useEffect(() => {
        setPageIndex(0);
    }, [filterStatus, filterMaker, filterType, debouncedInstSearch]);

    if (loading && lcs.length === 0) return (
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
                    <div className="title-group">
                        <h2>Import LC Portfolio</h2>
                        <p className="subtitle">Manage and track your active Import Letters of Credit.</p>
                    </div>
                    <div className="table-filters-container">
                        <div className="search-filter">
                            <div className="filter-search">
                                <Search size={16} />
                                <input 
                                    type="text" 
                                    placeholder="Search by Instrument ID or Ref..." 
                                    value={filterInstSearch}
                                    onChange={(e) => setFilterInstSearch(e.target.value)}
                                />
                            </div>
                        </div>
                        <div className="table-filters">
                            <select 
                                aria-label="Status Filter" 
                                className="filter-select"
                                value={filterStatus}
                                onChange={(e) => setFilterStatus(e.target.value)}
                            >
                                <option value="">Status: All</option>
                                <option value="LC_DRAFT">Draft</option>
                                <option value="LC_PENDING">Pending Approval</option>
                                <option value="LC_ISSUED">Issued</option>
                                <option value="LC_AMENDMENT_PENDING">Amendment Pending</option>
                                <option value="LC_AMENDED">Amended</option>
                                <option value="LC_DOC_RECEIVED">Docs Received</option>
                                <option value="LC_SETTLED">Settled</option>
                                <option value="LC_CLOSED">Closed</option>
                                <option value="LC_CANCELLED">Cancelled</option>
                                <option value="LC_EXPIRED">Expired</option>
                            </select>

                            <select 
                                aria-label="Maker Filter" 
                                className="filter-select"
                                value={filterMaker}
                                onChange={(e) => setFilterMaker(e.target.value)}
                            >
                                <option value="">Maker: All</option>
                                {makers.map(m => (
                                    <option key={m} value={m}>{m}</option>
                                ))}
                            </select>

                            <select 
                                aria-label="Type Filter" 
                                className="filter-select"
                                value={filterType}
                                onChange={(e) => setFilterType(e.target.value)}
                            >
                                <option value="">Type: All</option>
                                <option value="IMP_NEW">Issuance</option>
                                <option value="IMP_AMENDMENT">Amendment</option>
                                <option value="IMP_PRESENTATION">Presentation</option>
                                <option value="IMP_SETTLEMENT">Settlement</option>
                                <option value="IMP_CANCEL">Cancellation</option>
                            </select>
                        </div>
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
                                <th>Outstanding</th>
                                <th>Drawn</th>
                                <th>Expiry Date</th>
                                <th>Status</th>
                                <th>SLA Timer</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {lcs.length > 0 ? (
                                lcs.map(lc => {
                                        const displayAmount = (lc.effectiveAmount || lc.amount || 0);
                                        const displayExpiry = lc.effectiveExpiryDate || lc.expiryDate || '---';
                                        const isAmended = (lc.effectiveAmount !== undefined && lc.effectiveAmount !== lc.amount) || 
                                                        (lc.effectiveExpiryDate !== undefined && lc.effectiveExpiryDate !== lc.expiryDate);
                                        
                                        return (
                                            <tr key={lc.instrumentId} className="clickable-row" onClick={() => router.push(`/import-lc/details?id=${lc.instrumentId}`)}>
                                                <td className="font-bold">{lc.instrumentRef}</td>
                                                <td>{lc.applicantPartyName || lc.applicantName || '---'}</td>
                                                <td>{lc.beneficiaryPartyName || lc.beneficiaryName || '---'}</td>
                                                <td>{lc.currency || 'USD'}</td>
                                                <td>
                                                    {(displayAmount ?? 0).toLocaleString()}
                                                    {isAmended && <span className="amended-indicator" title="Amended Value">★</span>}
                                                </td>
                                                <td>{(lc.effectiveOutstandingAmount ?? 0).toLocaleString()}</td>
                                                <td>{(lc.cumulativeDrawnAmount ?? 0).toLocaleString()}</td>
                                                <td>
                                                    {displayExpiry}
                                                </td>
                                                <td>
                                                    <div className="status-container">
                                                        <span className={`status-tag ${lc.businessStateId}`}>
                                                            {lc.businessStateId?.replace('LC_', '')}
                                                        </span>
                                                        {isAmended && <span className="sub-status">Amended</span>}
                                                    </div>
                                                </td>
                                                <td className={(lc.slaDaysRemaining ?? 0) < 3 ? 'text-urgent' : ''}>
                                                    {lc.slaDaysRemaining !== undefined && lc.slaDaysRemaining !== null ? `${lc.slaDaysRemaining} days` : '---'}
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
                                                                {lc.businessStateId === 'LC_DRAFT' && (
                                                                    <button className="menu-item font-bold text-primary" onClick={(e) => { e.stopPropagation(); router.push(`/issuance?id=${lc.instrumentId}`); }}>
                                                                        Edit Draft
                                                                    </button>
                                                                )}
                                                                <button className="menu-item" onClick={(e) => { e.stopPropagation(); router.push(`/import-lc/details?id=${lc.instrumentId}`); }}>
                                                                    View Details
                                                                </button>
                                                                <hr style={{ margin: '0.25rem 0', border: 'none', borderTop: '1px solid #f1f5f9' }} />
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
                                        );
                                })
                            ) : (
                                <tr>
                                    <td colSpan={11} className="empty-state">No active transactions found.</td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>

                <div className="pagination-bar">
                    <div className="pagination-info">
                        Showing <span>{Math.min(totalCount, pageIndex * pageSize + 1)}</span> to <span>{Math.min(totalCount, (pageIndex + 1) * pageSize)}</span> of <span>{totalCount}</span> LCs
                    </div>
                    <div className="pagination-controls">
                        <button 
                            className="page-btn" 
                            onClick={() => setPageIndex(p => Math.max(0, p - 1))}
                            disabled={pageIndex === 0}
                        >
                            <ChevronLeft size={16} /> Previous
                        </button>
                        <div className="page-numbers">
                            {Array.from({ length: Math.ceil(totalCount / pageSize) }).map((_, i) => (
                                <button 
                                    key={i} 
                                    className={`page-num ${pageIndex === i ? 'active' : ''}`}
                                    onClick={() => setPageIndex(i)}
                                >
                                    {i + 1}
                                </button>
                            )).slice(Math.max(0, pageIndex - 2), Math.min(Math.ceil(totalCount / pageSize), pageIndex + 3))}
                        </div>
                        <button 
                            className="page-btn" 
                            onClick={() => setPageIndex(p => Math.min(Math.ceil(totalCount / pageSize) - 1, p + 1))}
                            disabled={pageIndex >= Math.ceil(totalCount / pageSize) - 1}
                        >
                            Next <ChevronRight size={16} />
                        </button>
                    </div>
                </div>
            </section>
            <style jsx>{`
                .dashboard-container { padding: 2rem; }
                .kpi-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem; margin-bottom: 2rem; }
                .premium-card { background: white; border-radius: 12px; padding: 1.5rem; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
                .kpi-item { display: flex; flex-direction: column; }
                .kpi-label { font-size: 0.875rem; color: #64748b; }
                .kpi-value { font-size: 1.5rem; font-weight: 700; color: #1e293b; }
                
                .transaction-section { padding: 0; }
                .table-header { padding: 1.5rem; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f1f5f9; }
                .table-responsive { overflow-x: auto; }
                .trade-table { width: 100%; border-collapse: collapse; text-align: left; }
                .trade-table th { padding: 1rem 1.5rem; background: #f8fafc; font-size: 0.75rem; text-transform: uppercase; color: #64748b; }
                .trade-table td { padding: 1rem 1.5rem; border-bottom: 1px solid #f1f5f9; font-size: 0.875rem; }
                
                .status-container { display: flex; flex-direction: column; gap: 0.25rem; }
                .status-tag { padding: 0.25rem 0.625rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 600; text-transform: uppercase; width: fit-content; }
                .LC_ISSUED { background: #dcfce7; color: #166534; }
                .LC_DRAFT { background: #f1f5f9; color: #475569; }
                .LC_PENDING { background: #fef9c3; color: #854d0e; }
                .LC_AMENDMENT_PENDING { background: #ffedd5; color: #9a3412; }
                .LC_AMENDED { background: #dbeafe; color: #1e40af; }
                .LC_DOC_RECEIVED { background: #f3e8ff; color: #6b21a8; }
                .LC_SETTLED { background: #dcfce7; color: #166534; }
                .LC_CLOSED { background: #f1f5f9; color: #475569; }
                .LC_CANCELLED { background: #fee2e2; color: #991b1b; }
                .LC_EXPIRED { background: #fee2e2; color: #991b1b; }
                .LC_HOLD { background: #fef2f2; color: #991b1b; border: 1px solid #fee2e2; }
                
                .amended-indicator { margin-left: 0.5rem; color: #2563eb; font-size: 1rem; cursor: help; }
                .sub-status { font-size: 0.7rem; color: #2563eb; font-weight: 700; text-transform: uppercase; margin-left: 0.5rem; }
                
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
                
                .filter-select { padding: 0.5rem; border-radius: 6px; border: 1px solid #cbd5e1; font-size: 0.875rem; outline: none; background: white; cursor: pointer; }
                .table-filters { display: flex; gap: 0.75rem; }
                .table-filters-container { display: flex; flex-direction: column; gap: 1rem; align-items: flex-end; }
                .filter-search { display: flex; align-items: center; gap: 0.5rem; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 0.5rem 1rem; color: #64748b; width: 100%; min-width: 250px; transition: all 0.2s; }
                .filter-search:focus-within { border-color: #2563eb; background: white; box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1); }
                .filter-search input { width: 100%; background: transparent; border: none; font-size: 0.875rem; outline: none; color: #1e293b; }
                .filter-search input::placeholder { color: #94a3b8; }

                .pagination-bar { padding: 1rem 1.5rem; display: flex; justify-content: space-between; align-items: center; border-top: 1px solid #f1f5f9; background: #f8fafc; }
                .pagination-info { font-size: 0.875rem; color: #64748b; }
                .pagination-info span { font-weight: 700; color: #1e293b; }
                .pagination-controls { display: flex; align-items: center; gap: 0.75rem; }
                .page-btn { display: flex; align-items: center; gap: 0.25rem; padding: 0.4rem 0.75rem; background: white; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 0.875rem; font-weight: 500; color: #475569; cursor: pointer; transition: all 0.2s; }
                .page-btn:hover:not(:disabled) { background: #f1f5f9; border-color: #94a3b8; }
                .page-btn:disabled { opacity: 0.5; cursor: not-allowed; }
                .page-numbers { display: flex; gap: 0.25rem; }
                .page-num { width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; background: white; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 0.875rem; font-weight: 500; color: #475569; cursor: pointer; }
                .page-num.active { background: #2563eb; color: white; border-color: #2563eb; }
                .page-num:hover:not(.active) { background: #f1f5f9; }

                @media (max-width: 1024px) {
                    .table-header { flex-direction: column; align-items: flex-start; gap: 1.5rem; }
                    .table-filters-container { align-items: flex-start; width: 100%; }
                    .table-filters { width: 100%; flex-wrap: wrap; }
                    .search-filter { width: 100%; }
                    .filter-search { min-width: 100%; }
                }
            `}</style>
        </div>
    );
};
