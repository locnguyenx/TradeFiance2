import { useRouter } from 'next/navigation';
import { Search, ChevronLeft, ChevronRight, Filter } from 'lucide-react';

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
    
    // Pagination
    totalCount?: number;
    pageIndex?: number;
    pageSize?: number;
    onPageChange?: (index: number) => void;
    
    // Search & Filter
    onSearchChange?: (value: string) => void;
    searchValue?: string;
    statusOptions?: { value: string, label: string }[];
    statusValue?: string;
    onStatusChange?: (value: string) => void;
}

export const RecordList: React.FC<Props> = ({ 
    title, 
    description, 
    records, 
    columns, 
    onRowClick, 
    loading,
    totalCount = 0,
    pageIndex = 0,
    pageSize = 20,
    onPageChange,
    onSearchChange,
    searchValue = '',
    statusOptions,
    statusValue = '',
    onStatusChange
}) => {
    const router = useRouter();

    if (loading && records.length === 0) return <div className="p-12 text-center text-slate-500 premium-card">Loading Portfolio Records...</div>;

    const totalPages = Math.ceil(totalCount / pageSize);

    return (
        <div className="record-list-container premium-card">
            <header className="list-header">
                <div className="title-group">
                    <h2>{title}</h2>
                    <p>{description}</p>
                </div>
                <div className="list-actions">
                    <div className="filters-row">
                        {onSearchChange && (
                            <div className="filter-search">
                                <Search size={16} />
                                <input 
                                    type="text" 
                                    placeholder="Search Instrument..." 
                                    value={searchValue}
                                    onChange={(e) => onSearchChange(e.target.value)}
                                />
                            </div>
                        )}
                        {statusOptions && onStatusChange && (
                            <div className="filter-select">
                                <Filter size={14} />
                                <select 
                                    aria-label="Status Filter"
                                    value={statusValue}
                                    onChange={(e) => onStatusChange(e.target.value)}
                                >
                                    <option value="">All Statuses</option>
                                    {statusOptions.map(opt => (
                                        <option key={opt.value} value={opt.value}>{opt.label}</option>
                                    ))}
                                </select>
                            </div>
                        )}
                    </div>
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

            {onPageChange && totalCount > 0 && (
                <div className="pagination-bar">
                    <div className="pagination-info">
                        Showing <span>{Math.min(totalCount, pageIndex * pageSize + 1)}</span> to <span>{Math.min(totalCount, (pageIndex + 1) * pageSize)}</span> of <span>{totalCount}</span> records
                    </div>
                    <div className="pagination-controls">
                        <button 
                            className="page-btn" 
                            onClick={() => onPageChange(Math.max(0, pageIndex - 1))}
                            disabled={pageIndex === 0}
                        >
                            <ChevronLeft size={16} /> Previous
                        </button>
                        <div className="page-numbers">
                            {Array.from({ length: totalPages }).map((_, i) => (
                                <button 
                                    key={i} 
                                    className={`page-num ${pageIndex === i ? 'active' : ''}`}
                                    onClick={() => onPageChange(i)}
                                >
                                    {i + 1}
                                </button>
                            )).slice(Math.max(0, pageIndex - 2), Math.min(totalPages, pageIndex + 3))}
                        </div>
                        <button 
                            className="page-btn" 
                            onClick={() => onPageChange(Math.min(totalPages - 1, pageIndex + 1))}
                            disabled={pageIndex >= totalPages - 1}
                        >
                            Next <ChevronRight size={16} />
                        </button>
                    </div>
                </div>
            )}

            <style jsx>{`
                .record-list-container { background: white; padding: 0; overflow: hidden; border: 1px solid #e2e8f0; border-radius: 12px; }
                .list-header { padding: 1.5rem; border-bottom: 1px solid #f1f5f9; display: flex; justify-content: space-between; align-items: center; }
                .list-header h2 { margin: 0; font-size: 1.25rem; font-weight: 800; color: #1e293b; }
                .list-header p { margin: 0.25rem 0 0 0; font-size: 0.875rem; color: #64748b; }
                
                .filters-row { display: flex; gap: 1rem; align-items: center; }
                
                .filter-search { display: flex; align-items: center; gap: 0.5rem; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 0.4rem 0.75rem; color: #64748b; min-width: 280px; transition: all 0.2s; }
                .filter-search:focus-within { border-color: #2563eb; background: white; box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1); }
                .filter-search input { width: 100%; background: transparent; border: none; font-size: 0.875rem; outline: none; color: #1e293b; }
                .filter-search input::placeholder { color: #94a3b8; }
                
                .filter-select { display: flex; align-items: center; gap: 0.5rem; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 0.4rem 0.75rem; color: #64748b; min-width: 160px; transition: all 0.2s; }
                .filter-select:focus-within { border-color: #2563eb; background: white; box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1); }
                .filter-select select { background: transparent; border: none; font-size: 0.8125rem; font-weight: 600; color: #1e293b; outline: none; cursor: pointer; width: 100%; }

                .table-responsive { width: 100%; overflow-x: auto; -webkit-overflow-scrolling: touch; }
                .trade-table { width: 100%; border-collapse: collapse; text-align: left; min-width: 800px; }
                .trade-table th { padding: 1rem 1.5rem; background: #f8fafc; font-size: 0.75rem; text-transform: uppercase; color: #64748b; font-weight: 700; }
                .trade-table td { padding: 1rem 1.5rem; border-bottom: 1px solid #f1f5f9; font-size: 0.875rem; color: #334155; }
                
                .clickable-row { cursor: pointer; transition: background 0.2s; }
                .clickable-row:hover { background: #f8fafc; }
                
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

                .empty-state { text-align: center; padding: 3rem; color: #94a3b8; font-style: italic; }
                .premium-card { box-shadow: 0 1px 3px rgba(0,0,0,0.05); }

                @media (max-width: 768px) {
                    .list-header { flex-direction: column; align-items: flex-start; gap: 1rem; }
                    .filter-search { width: 100%; }
                    .pagination-bar { flex-direction: column; gap: 1rem; }
                }
            `}</style>
        </div>
    );
};
