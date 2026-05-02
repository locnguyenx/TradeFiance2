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
    Filter
} from 'lucide-react';
import Link from 'next/link';

// ABOUTME: TransactionDashboard provide a cross-product view of all trade transactions.
// ABOUTME: Implements REQ-NAV-01.3 and REQ-TXN-DASH for unified operational tracking.

export const TransactionDashboard: React.FC = () => {
    const [transactions, setTransactions] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [filterStatus, setFilterStatus] = useState('');
    const [filterPriority, setFilterPriority] = useState('');

    useEffect(() => {
        const loadTransactions = async () => {
            setLoading(true);
            try {
                const data = await tradeApi.getTransactions(filterStatus, filterPriority);
                setTransactions(data.transactionList || []);
            } catch (error) {
                console.error('Failed to load transactions:', error);
            } finally {
                setLoading(false);
            }
        };
        loadTransactions();
    }, [filterStatus, filterPriority]);

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
                <div className="search-input-wrapper">
                    <Search size={18} className="search-icon" />
                    <input type="text" placeholder="Search by instrument reference..." />
                </div>
                <div className="filter-actions">
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
                </div>
            </div>

            <div className="transaction-list premium-card">
                <table className="txn-table">
                    <thead>
                        <tr>
                            <th>Priority</th>
                            <th>Reference</th>
                            <th>Type</th>
                            <th>Maker / Date</th>
                            <th>Status</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {loading ? (
                            <tr><td colSpan={6} style={{ textAlign: 'center', padding: '3rem' }}>Fetching real-time transaction data...</td></tr>
                        ) : transactions.length === 0 ? (
                            <tr><td colSpan={6} style={{ textAlign: 'center', padding: '3rem' }}>No transactions found matching the filters.</td></tr>
                        ) : transactions.map(txn => (
                            <tr key={txn.transactionId}>
                                <td>
                                    <span className="priority-badge" style={{ backgroundColor: getPriorityColor(txn.priorityEnumId) }}>
                                        {txn.priorityEnumId?.replace('TX_PRIO_', '') || 'LOW'}
                                    </span>
                                </td>
                                <td>
                                    <div className="ref-cell">
                                        <span className="instrument-id">{txn.instrumentId}</span>
                                        <span className="txn-id">{txn.transactionId}</span>
                                    </div>
                                </td>
                                <td className="type-cell">
                                    {txn.transactionTypeEnumId?.replace('IMP_', '').replace('_', ' ')}
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
                                        <span>{txn.transactionStatusId?.replace('TX_', '')}</span>
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

            <style jsx>{`
                .txn-dashboard-container { display: flex; flex-direction: column; gap: 1.5rem; }
                .dashboard-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 0.5rem; }
                .dashboard-header h1 { font-size: 1.875rem; font-weight: 800; color: #1e293b; margin: 0 0 0.5rem 0; letter-spacing: -0.025em; }
                .subtitle { color: #64748b; font-size: 0.875rem; margin: 0; }
                
                .stat-card { background: white; padding: 1rem 1.5rem; border-radius: 12px; border: 1px solid #e2e8f0; display: flex; flex-direction: column; gap: 0.25rem; min-width: 160px; box-shadow: 0 1px 2px rgba(0,0,0,0.05); }
                .stat-label { font-size: 0.75rem; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.05em; }
                .stat-value { font-size: 1.5rem; font-weight: 800; color: #2563eb; }

                .filter-bar { background: white; padding: 0.75rem 1.25rem; display: flex; justify-content: space-between; align-items: center; gap: 1rem; }
                .search-input-wrapper { position: relative; flex: 1; max-width: 400px; }
                .search-icon { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); color: #94a3b8; }
                .search-input-wrapper input { width: 100%; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 0.5rem 1rem 0.5rem 2.5rem; font-size: 0.875rem; outline: none; transition: all 0.2s; }
                .search-input-wrapper input:focus { border-color: #2563eb; background: white; box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1); }
                
                .filter-actions { display: flex; gap: 0.75rem; }
                .filter-select { display: flex; align-items: center; gap: 0.5rem; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 0.25rem 0.75rem; color: #64748b; }
                .filter-select select { background: transparent; border: none; font-size: 0.8125rem; font-weight: 600; color: #1e293b; outline: none; cursor: pointer; }

                .transaction-list { background: white; overflow: hidden; }
                .txn-table { width: 100%; border-collapse: collapse; }
                .txn-table th { text-align: left; padding: 1rem 1.5rem; background: #f8fafc; font-size: 0.75rem; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.05em; border-bottom: 1px solid #e2e8f0; }
                .txn-table td { padding: 1.25rem 1.5rem; border-bottom: 1px solid #f1f5f9; font-size: 0.875rem; vertical-align: middle; }
                
                .priority-badge { font-size: 0.65rem; font-weight: 800; color: white; padding: 2px 8px; border-radius: 4px; text-transform: uppercase; }
                
                .ref-cell { display: flex; flex-direction: column; }
                .instrument-id { font-weight: 700; color: #2563eb; }
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
