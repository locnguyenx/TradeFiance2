'use client';

import React, { useState, useEffect } from 'react';
import { tradeApi } from '../api/tradeApi';
import { useRouter } from 'next/navigation';
import { 
    ArrowRight, 
    Clock, 
    CheckCircle, 
    AlertCircle, 
    FileText 
} from 'lucide-react';

// ABOUTME: Reusable Transaction List component for specific instrument lifecycles.
// ABOUTME: Supports filtering by transaction type (Amendments, Presentations, etc.)

interface TransactionListProps {
    transactionType: 'IMP_AMENDMENT' | 'IMP_PRESENTATION' | 'IMP_SETTLEMENT' | 'IMP_CANCEL' | 'IMP_SHIPPING_GUARANTEE';
    title: string;
    description: string;
    createUrl?: string;
    createLabel?: string;
}

export const TransactionList: React.FC<TransactionListProps> = ({ 
    transactionType, 
    title, 
    description,
    createUrl,
    createLabel 
}) => {
    const [transactions, setTransactions] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const router = useRouter();

    useEffect(() => {
        const loadTransactions = async () => {
            setLoading(true);
            try {
                // Fetch transactions of the specified type
                const data = await tradeApi.getTransactions(undefined, undefined, undefined, transactionType);
                setTransactions(data.transactionList || []);
            } catch (err: any) {
                console.error(`Failed to load ${transactionType}:`, err);
                setError(err.message || 'Failed to load records.');
            } finally {
                setLoading(false);
            }
        };
        loadTransactions();
    }, [transactionType]);

    const getStatusIcon = (status: string) => {
        switch (status) {
            case 'TX_APPROVED': return <CheckCircle size={16} className="text-green-500" />;
            case 'TX_REJECTED': return <AlertCircle size={16} className="text-red-500" />;
            case 'TX_PENDING': return <Clock size={16} className="text-orange-500" />;
            default: return <FileText size={16} className="text-slate-400" />;
        }
    };

    if (loading) return <div className="p-12 text-center text-slate-500 premium-card">Loading {title}...</div>;

    return (
        <div className="transaction-list-container">
            <header className="list-header mb-6">
                <div className="header-info">
                    <h1 className="text-2xl font-extrabold text-slate-900">{title}</h1>
                    <p className="text-slate-500">{description}</p>
                </div>
                {createUrl && (
                    <button 
                        className="primary-btn"
                        onClick={() => router.push(createUrl)}
                    >
                        {createLabel || 'Create New'}
                    </button>
                )}
            </header>

            {error && <div className="error-banner mb-4">{error}</div>}

            <div className="table-wrapper premium-card">
                <table className="txn-table">
                    <thead>
                        <tr>
                            <th>Ref No</th>
                            <th>Instrument ID</th>
                            <th>Maker</th>
                            <th>Date</th>
                            <th>Status</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {transactions.length === 0 ? (
                            <tr>
                                <td colSpan={6} className="empty-state">
                                    No {title.toLowerCase()} found.
                                </td>
                            </tr>
                        ) : (
                            transactions.map(txn => (
                                <tr key={txn.transactionId} className="hover:bg-slate-50 transition-colors">
                                    <td className="font-bold text-slate-800">{txn.transactionRef || txn.transactionId}</td>
                                    <td className="font-medium text-slate-600">{txn.instrumentId}</td>
                                    <td>{txn.makerUserId}</td>
                                    <td>{new Date(txn.transactionDate).toLocaleDateString()}</td>
                                    <td>
                                        <div className="flex items-center gap-2">
                                            {getStatusIcon(txn.transactionStatusId)}
                                            <span className="text-xs font-bold uppercase text-slate-700">
                                                {txn.transactionStatusId?.replace('TX_', '')}
                                            </span>
                                        </div>
                                    </td>
                                    <td>
                                        <button 
                                            className="view-btn"
                                            onClick={() => router.push(`/transactions/details?id=${txn.transactionId}`)}
                                        >
                                            View Details <ArrowRight size={14} />
                                        </button>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>

            <style jsx>{`
                .transaction-list-container { width: 100%; }
                .list-header { display: flex; justify-content: space-between; align-items: flex-end; }
                .premium-card { background: white; border-radius: 12px; border: 1px solid #e2e880; box-shadow: 0 1px 3px rgba(0,0,0,0.05); overflow: hidden; }
                
                .txn-table { width: 100%; border-collapse: collapse; }
                .txn-table th { text-align: left; padding: 1rem 1.5rem; background: #f8fafc; font-size: 0.75rem; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.05em; border-bottom: 1px solid #e2e8f0; }
                .txn-table td { padding: 1.25rem 1.5rem; border-bottom: 1px solid #f1f5f9; font-size: 0.875rem; color: #334155; }
                
                .primary-btn { background: #2563eb; color: white; border: none; padding: 0.75rem 1.5rem; border-radius: 8px; font-weight: 700; cursor: pointer; transition: background 0.2s; }
                .primary-btn:hover { background: #1d4ed8; }
                
                .view-btn { display: flex; align-items: center; gap: 0.5rem; color: #2563eb; background: none; border: none; padding: 0; font-weight: 700; cursor: pointer; font-size: 0.8125rem; }
                .view-btn:hover { color: #1d4ed8; text-decoration: underline; }
                
                .empty-state { text-align: center; padding: 4rem; color: #94a3b8; font-style: italic; }
                .error-banner { padding: 1rem; background: #fef2f2; border: 1px solid #fecaca; border-radius: 8px; color: #991b1b; font-weight: 600; }
            `}</style>
        </div>
    );
};
