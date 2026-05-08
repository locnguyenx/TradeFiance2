'use client';

import React from 'react';
import { useRouter } from 'next/navigation';

// ABOUTME: Generic Record List component for browsing specific instrument lifecycle entities.
// ABOUTME: Decoupled from TradeTransaction to allow direct asset-level management.

interface Column {
    key: string;
    label: string;
    render?: (value: any, row: any) => React.ReactNode;
}

interface Props {
    title: string;
    description: string;
    records: any[];
    columns: Column[];
    onRowClick?: (record: any) => void;
    loading?: boolean;
}

export const RecordList: React.FC<Props> = ({ 
    title, 
    description, 
    records, 
    columns, 
    onRowClick, 
    loading 
}) => {
    const router = useRouter();

    if (loading) return <div className="p-12 text-center text-slate-500">Loading Portfolio Records...</div>;

    return (
        <div className="record-list-container premium-card">
            <header className="list-header">
                <div className="title-group">
                    <h2>{title}</h2>
                    <p>{description}</p>
                </div>
            </header>

            <div className="table-responsive">
                <table className="trade-table">
                    <thead>
                        <tr>
                            {columns.map(col => (
                                <th key={col.key}>{col.label}</th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {records.length > 0 ? (
                            records.map((record, idx) => (
                                <tr 
                                    key={record.id || idx} 
                                    className="clickable-row" 
                                    onClick={() => onRowClick ? onRowClick(record) : null}
                                >
                                    {columns.map(col => (
                                        <td key={col.key}>
                                            {col.render ? col.render(record[col.key], record) : (record[col.key] || '---')}
                                        </td>
                                    ))}
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan={columns.length} className="empty-state">No records found.</td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>

            <style jsx>{`
                .record-list-container { background: white; padding: 0; overflow: hidden; border: 1px solid #e2e8f0; border-radius: 12px; }
                .list-header { padding: 1.5rem; border-bottom: 1px solid #f1f5f9; }
                .list-header h2 { margin: 0; font-size: 1.25rem; font-weight: 800; color: #1e293b; }
                .list-header p { margin: 0.25rem 0 0 0; font-size: 0.875rem; color: #64748b; }
                
                .table-responsive { width: 100%; overflow-x: auto; }
                .trade-table { width: 100%; border-collapse: collapse; text-align: left; }
                .trade-table th { padding: 1rem 1.5rem; background: #f8fafc; font-size: 0.75rem; text-transform: uppercase; color: #64748b; font-weight: 700; }
                .trade-table td { padding: 1rem 1.5rem; border-bottom: 1px solid #f1f5f9; font-size: 0.875rem; color: #334155; }
                
                .clickable-row { cursor: pointer; transition: background 0.2s; }
                .clickable-row:hover { background: #f8fafc; }
                
                .empty-state { text-align: center; padding: 3rem; color: #94a3b8; font-style: italic; }
                
                .premium-card { box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
            `}</style>
        </div>
    );
};
