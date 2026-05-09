'use client';

import React, { useState, useEffect } from 'react';
import { tradeApi } from '../api/tradeApi';
import { 
    Clock, 
    AlertTriangle, 
    CheckCircle, 
    FileText, 
    ArrowRight,
    Search,
    Filter,
    ChevronLeft,
    ChevronRight
} from 'lucide-react';
import Link from 'next/link';

// ABOUTME: TransactionDashboard provide a cross-product view of all trade transactions.
// ABOUTME: Implements REQ-NAV-01.3 and REQ-TXN-DASH for unified operational tracking.

export const TransactionDashboard: React.FC = () => {
    const [transactions, setTransactions] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [filterStatus, setFilterStatus] = useState('');
    const [filterPriority, setFilterPriority] = useState('');
    const [filterMaker, setFilterMaker] = useState('');
    const [filterType, setFilterType] = useState('');
    const [filterInstSearch, setFilterInstSearch] = useState('');
    const [filterTxnSearch, setFilterTxnSearch] = useState('');
    const [makers, setMakers] = useState<string[]>([]);

    const [pageIndex, setPageIndex] = useState(0);
    const [pageSize, setPageSize] = useState(20);
    const [totalCount, setTotalCount] = useState(0);
    const [debouncedInstSearch, setDebouncedInstSearch] = useState(filterInstSearch);
    const [debouncedTxnSearch, setDebouncedTxnSearch] = useState(filterTxnSearch);

    // Debounce search terms
    useEffect(() => {
        const timer = setTimeout(() => {
            setDebouncedInstSearch(filterInstSearch);
            setDebouncedTxnSearch(filterTxnSearch);
        }, 500);
        return () => clearTimeout(timer);
    }, [filterInstSearch, filterTxnSearch]);

    useEffect(() => {
        const loadMakers = async () => {
            try {
                const authData = await tradeApi.getUserAuthorityProfiles();
                const uniqueMakers = Array.from(new Set(authData.profileList.map(p => p.userId)));
                setMakers(uniqueMakers);
            } catch (error) {
                console.error('Failed to load makers:', error);
            }
        };
        loadMakers();
    }, []);

    useEffect(() => {
        const loadTransactions = async () => {
            setLoading(true);
            try {
                const txnData = await tradeApi.getTransactions(
                    filterStatus, 
                    filterPriority, 
                    filterMaker, 
                    filterType, 
                    debouncedInstSearch, 
                    debouncedTxnSearch, 
                    pageIndex, 
                    pageSize
                );
                setTransactions(txnData.transactionList || []);
                setTotalCount(txnData.transactionCount || 0);
            } catch (error) {
                console.error('Failed to load transactions:', error);
            } finally {
                setLoading(false);
            }
        };
        loadTransactions();
    }, [filterStatus, filterPriority, filterMaker, filterType, debouncedInstSearch, debouncedTxnSearch, pageIndex, pageSize]);

    // Reset pageIndex when filters change
    useEffect(() => {
        setPageIndex(0);
    }, [filterStatus, filterPriority, filterMaker, filterType, debouncedInstSearch, debouncedTxnSearch]);

    const typeLabels: Record<string, string> = {
        'IMP_NEW': 'Issuance',
        'IMP_AMENDMENT': 'Amendment',
        'IMP_PRESENTATION': 'Presentation',
        'IMP_SETTLEMENT': 'Settlement',
        'IMP_CANCELLATION': 'Cancellation',
        'IMP_SG_ISSUANCE': 'Shipping Guarantee'
    };

    const statusLabels: Record<string, string> = {
        'TX_DRAFT': 'Draft',
        'TX_PENDING': 'Pending',
        'TX_APPROVED': 'Approved',
        'TX_REJECTED': 'Rejected'
    };

    const getPriorityColor = (priority: string) => {
        switch (priority) {
            case 'TX_PRIO_URGENT': return '#ef4444';
            case 'TX_PRIO_HIGH': return '#f97316';
            case 'TX_PRIO_MEDIUM': return '#3b82f6';
            case 'TX_PRIO_LOW': return '#94a3b8';
            default: return '#64748b';
        }
    };

    const getStatusIcon = (status: string) => {
        switch (status) {
            case 'TX_APPROVED': return <CheckCircle size={16} color="#10b981" />;
            case 'TX_REJECTED': return <AlertTriangle size={16} color="#ef4444" />;
            case 'TX_PENDING': return <Clock size={16} color="#f97316" />;
            default: return <FileText size={16} color="#64748b" />;
        }
    };

    return (
        <div className="txn-dashboard-container">
            <header className="dashboard-header">
                <div className="header-content">
                    <h1>Operations Dashboard</h1>
                    <p className="subtitle">Unified operational view of all maker/checker activities across instrument lifecycles.</p>
                </div>
                <div className="header-stats">
                    <div className="stat-card">
                        <span className="stat-label">Pending Approval</span>
                        <span className="stat-value">{transactions.filter(t => t.transactionStatusId === 'TX_PENDING').length}</span>
                    </div>
                </div>
            </header>

            <div className="filter-bar premium-card">
                <div className="filter-row search-row">
                    <div className="filter-search">
                        <Search size={16} />
                        <input 
                            type="text" 
                            placeholder="Instrument (Ref / ID)..." 
                            value={filterInstSearch}
                            onChange={(e) => setFilterInstSearch(e.target.value)}
                        />
                    </div>
                    <div className="filter-search">
                        <Search size={16} />
                        <input 
                            type="text" 
                            placeholder="Transaction (Ref / ID)..." 
                            value={filterTxnSearch}
                            onChange={(e) => setFilterTxnSearch(e.target.value)}
                        />
                    </div>
                </div>
                <div className="filter-row action-row">
                    <div className="filter-select">
                        <Filter size={14} />
                        <select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
                            <option value="">All Statuses</option>
                            <option value="TX_DRAFT">Draft</option>
                            <option value="TX_PENDING">Pending</option>
                            <option value="TX_APPROVED">Approved</option>
                            <option value="TX_REJECTED">Rejected</option>
                        </select>
                    </div>
                    <div className="filter-select">
                        <Filter size={14} />
                        <select value={filterPriority} onChange={(e) => setFilterPriority(e.target.value)}>
                            <option value="">All Priorities</option>
                            <option value="TX_PRIO_URGENT">Urgent</option>
                            <option value="TX_PRIO_HIGH">High</option>
                            <option value="TX_PRIO_MEDIUM">Medium</option>
                            <option value="TX_PRIO_LOW">Low</option>
                        </select>
                    </div>
                    <div className="filter-select">
                        <Filter size={14} />
                        <select value={filterMaker} onChange={(e) => setFilterMaker(e.target.value)}>
                            <option value="">All Makers</option>
                            {makers.map(m => (
                                <option key={m} value={m}>{m}</option>
                            ))}
                        </select>
                    </div>
                    <div className="filter-select">
                        <Filter size={14} />
                        <select value={filterType} onChange={(e) => setFilterType(e.target.value)}>
                            <option value="">All Types</option>
                            <option value="IMP_NEW">Issuance</option>
                            <option value="IMP_AMENDMENT">Amendment</option>
                            <option value="IMP_PRESENTATION">Presentation</option>
                            <option value="IMP_SETTLEMENT">Settlement</option>
                            <option value="IMP_CANCELLATION">Cancellation</option>
                            <option value="IMP_SG_ISSUANCE">Shipping Guarantee</option>
                        </select>
                    </div>
                </div>
            </div>

            <div className="transaction-list premium-card">
                <div className="table-responsive">
                    <table className="txn-table">
                        <thead>
                            <tr>
                                <th>Priority</th>
                                <th>Instrument (Ref / ID)</th>
                                <th>Transaction (Ref / ID)</th>
                                <th>Type</th>
                                <th>Maker / Date</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {loading ? (
                                <tr><td colSpan={7} style={{ textAlign: 'center', padding: '3rem' }}>Fetching real-time transaction data...</td></tr>
                            ) : transactions.length === 0 ? (
                                <tr><td colSpan={7} style={{ textAlign: 'center', padding: '3rem' }}>No transactions found matching the filters.</td></tr>
                            ) : transactions.map(txn => (
                                <tr key={txn.transactionId}>
                                    <td>
                                        <span className="priority-badge" style={{ backgroundColor: getPriorityColor(txn.priorityEnumId) }}>
                                            {txn.priorityEnumId?.replace('TX_PRIO_', '') || 'LOW'}
                                        </span>
                                    </td>
                                    <td>
                                        <div className="ref-cell">
                                            <span className="instrument-ref-text">{txn.instrumentRef}</span>
                                            <span className="instrument-id" title="Instrument ID">{txn.instrumentId}</span>
                                        </div>
                                    </td>
                                    <td>
                                        <div className="ref-cell">
                                            <span className="transaction-ref-text">{txn.transactionRef || 'N/A'}</span>
                                            <span className="txn-id" title="Transaction ID">{txn.transactionId}</span>
                                        </div>
                                    </td>
                                    <td className="type-cell">
                                        {typeLabels[txn.transactionTypeEnumId] || txn.transactionTypeEnumId?.replace('IMP_', '').replace('_', ' ')}
                                    </td>
                                    <td>
                                        <div className="maker-info">
                                            <span className="maker-id">{txn.makerUserId}</span>
                                            <span className="txn-date">{new Date(txn.transactionDate).toLocaleDateString()}</span>
                                        </div>
                                    </td>
                                    <td>
                                        <div className="status-cell">
                                            {getStatusIcon(txn.transactionStatusId)}
                                            <span>{statusLabels[txn.transactionStatusId] || txn.transactionStatusId?.replace('TX_', '')}</span>
                                        </div>
                                    </td>
                                    <td>
                                        <Link href={`/transactions/details?id=${txn.transactionId}`} className="view-link">
                                            View Detail <ArrowRight size={14} />
                                        </Link>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>

                <div className="pagination-bar">
                    <div className="pagination-info">
                        Showing <span>{Math.min(totalCount, pageIndex * pageSize + 1)}</span> to <span>{Math.min(totalCount, (pageIndex + 1) * pageSize)}</span> of <span>{totalCount}</span> transactions
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
            </div>

            <style jsx>{`
                .txn-dashboard-container { display: flex; flex-direction: column; gap: 1.5rem; }
                .dashboard-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 0.5rem; }
                .dashboard-header h1 { font-size: 1.875rem; font-weight: 800; color: #1e293b; margin: 0 0 0.5rem 0; letter-spacing: -0.025em; }
                .subtitle { color: #64748b; font-size: 0.875rem; margin: 0; }
                
                .stat-card { background: white; padding: 1rem 1.5rem; border-radius: 12px; border: 1px solid #e2e8f0; display: flex; flex-direction: column; gap: 0.25rem; min-width: 160px; box-shadow: 0 1px 2px rgba(0,0,0,0.05); }
                .stat-label { font-size: 0.75rem; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.05em; }
                .stat-value { font-size: 1.5rem; font-weight: 800; color: #2563eb; }

                .pagination-bar { padding: 1rem 1.5rem; display: flex; justify-content: space-between; align-items: center; border-top: 1px solid #e2e8f0; background: #f8fafc; }
                .pagination-info { font-size: 0.8125rem; color: #64748b; }
                .pagination-info span { font-weight: 700; color: #1e293b; }
                .pagination-controls { display: flex; align-items: center; gap: 0.75rem; }
                .page-btn { display: flex; align-items: center; gap: 0.25rem; padding: 0.5rem 0.75rem; background: white; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 0.8125rem; font-weight: 600; color: #475569; cursor: pointer; transition: all 0.2s; }
                .page-btn:hover:not(:disabled) { border-color: #cbd5e1; background: #f1f5f9; }
                .page-btn:disabled { opacity: 0.5; cursor: not-allowed; }
                .page-numbers { display: flex; gap: 0.25rem; }
                .page-num { width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; background: white; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 0.8125rem; font-weight: 600; color: #475569; cursor: pointer; transition: all 0.2s; }
                .page-num:hover { border-color: #cbd5e1; background: #f1f5f9; }
                .page-num.active { background: #2563eb; color: white; border-color: #2563eb; }

                .filter-bar { background: white; padding: 1.25rem; display: flex; flex-direction: column; gap: 1.25rem; }
                .filter-row { display: flex; gap: 1rem; flex-wrap: wrap; }
                .search-row { max-width: 800px; }
                
                .filter-search { display: flex; align-items: center; gap: 0.5rem; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 0.5rem 1rem; color: #64748b; flex: 1; min-width: 250px; transition: all 0.2s; }
                .filter-search:focus-within { border-color: #2563eb; background: white; box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1); }
                .filter-search input { width: 100%; background: transparent; border: none; font-size: 0.875rem; outline: none; color: #1e293b; }
                .filter-search input::placeholder { color: #94a3b8; }
                
                .filter-select { display: flex; align-items: center; gap: 0.5rem; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 0.5rem 1rem; color: #64748b; min-width: 160px; transition: all 0.2s; }
                .filter-select:focus-within { border-color: #2563eb; background: white; box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1); }
                .filter-select select { background: transparent; border: none; font-size: 0.8125rem; font-weight: 600; color: #1e293b; outline: none; cursor: pointer; width: 100%; }

                .transaction-list { background: white; overflow: hidden; }
                .table-responsive { width: 100%; overflow-x: auto; -webkit-overflow-scrolling: touch; }
                .txn-table { width: 100%; border-collapse: collapse; min-width: 1000px; }
                .txn-table th { text-align: left; padding: 1rem 1.5rem; background: #f8fafc; font-size: 0.75rem; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.05em; border-bottom: 1px solid #e2e8f0; }
                .txn-table td { padding: 1.25rem 1.5rem; border-bottom: 1px solid #f1f5f9; font-size: 0.875rem; vertical-align: middle; }
                
                .priority-badge { font-size: 0.65rem; font-weight: 800; color: white; padding: 2px 8px; border-radius: 4px; text-transform: uppercase; }
                
                .instrument-ref-text { font-weight: 700; color: #0f172a; }
                .transaction-ref-text { font-weight: 700; color: #2563eb; font-size: 0.8125rem; }
                .ref-cell { display: flex; flex-direction: column; }
                .instrument-id { font-size: 0.7rem; color: #94a3b8; font-family: monospace; }
                .txn-id { font-size: 0.7rem; color: #94a3b8; font-family: monospace; }
                
                .type-cell { font-weight: 600; color: #1e293b; text-transform: capitalize; }
                .maker-info { display: flex; flex-direction: column; }
                .maker-id { font-weight: 500; color: #475569; }
                .txn-date { font-size: 0.75rem; color: #94a3b8; }
                
                .status-cell { display: flex; align-items: center; gap: 0.5rem; font-weight: 700; font-size: 0.75rem; color: #1e293b; text-transform: uppercase; }
                
                .view-link { display: flex; align-items: center; gap: 0.5rem; color: #2563eb; font-weight: 600; text-decoration: none; font-size: 0.8125rem; transition: color 0.1s; }
                .view-link:hover { color: #1d4ed8; }
                .premium-card { background: white; border: 1px solid #e2e8f0; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
            `}</style>
        </div>
    );
};
