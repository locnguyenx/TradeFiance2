"use client";

import React, { ReactNode, useState } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { 
    LayoutDashboard, 
    CheckCircle, 
    PlusCircle, 
    FileSearch, 
    FileEdit, 
    FileText, 
    CreditCard, 
    ShieldAlert, 
    XCircle, 
    Users, 
    BarChart4, 
    Database, 
    Settings2, 
    ShieldCheck, 
    History,
    Globe,
    Search
} from 'lucide-react';

// ABOUTME: Modernized Global application shell implementing REQ-UI-MOD-01.
// ABOUTME: Features Global Contextual Search (REQ-UTN-02).

interface NavItem {
    id: string;
    label: string;
    icon: React.ReactNode;
    path: string;
}

interface NavSection {
    group: string;
    items: NavItem[];
}

export const GlobalShell: React.FC<{ children: ReactNode }> = ({ children }) => {
    const pathname = usePathname();
    const router = useRouter();
    const [searchQuery, setSearchQuery] = useState('');
    const [searchContext, setSearchContext] = useState<'INSTRUMENT' | 'TRANSACTION'>('INSTRUMENT');

    const navSections: NavSection[] = [
        { group: 'OPERATIONS', items: [
            { id: 'transactions', label: 'Transaction Dashboard', icon: <LayoutDashboard size={18} />, path: '/transactions' },
            { id: 'approvals', label: 'My Tasks', icon: <CheckCircle size={18} />, path: '/approvals' },
            { id: 'documents', label: 'Document Examination', icon: <FileSearch size={18} />, path: '/import-lc/documents' },
        ]},
        { group: 'IMPORT LC', items: [
            { id: 'dashboard', label: 'Import LC Dashboard', icon: <Globe size={18} />, path: '/import-lc' },
            { id: 'issuance', label: 'New LC Issuance', icon: <PlusCircle size={18} />, path: '/issuance' },
            { id: 'amendments', label: 'LC Amendments', icon: <FileEdit size={18} />, path: '/import-lc/amendments' },
            { id: 'presentations', label: 'Presentations', icon: <FileText size={18} />, path: '/import-lc/presentations' },
            { id: 'settlement', label: 'Settlements', icon: <CreditCard size={18} />, path: '/import-lc/settlement' },
            { id: 'guarantees', label: 'Shipping Guarantees', icon: <ShieldAlert size={18} />, path: '/import-lc/shipping-guarantees' },
            { id: 'cancellations', label: 'Cancellations', icon: <XCircle size={18} />, path: '/import-lc/cancellations' },
        ]},
        { group: 'MASTER DATA', items: [
            { id: 'parties', label: 'Party & KYC Directory', icon: <Users size={18} />, path: '/parties' },
            { id: 'facilities', label: 'Credit Facilities (Limits)', icon: <BarChart4 size={18} />, path: '/facilities' },
            { id: 'tariffs', label: 'Tariff & Fee Configuration', icon: <Database size={18} />, path: '/tariffs' },
            { id: 'product', label: 'Product Configuration', icon: <Settings2 size={18} />, path: '/admin/product' },
        ]},
        { group: 'ADMINISTRATION', items: [
            { id: 'tiers', label: 'User Authority Tiers', icon: <ShieldCheck size={18} />, path: '/admin/tiers' },
            { id: 'logs', label: 'Audit Logs', icon: <History size={18} />, path: '/admin/logs' },
        ]}
    ];

    const isActive = (href: string) => {
        const currentPath = (pathname || '/').replace(/\/$/, '') || '/';
        const targetPath = href.replace(/\/$/, '') || '/';
        
        if (targetPath === '/import-lc' && currentPath === '/') return true;
        return currentPath === targetPath || (targetPath !== '/' && currentPath.startsWith(targetPath));
    };

    const handleSearch = (e: React.FormEvent) => {
        e.preventDefault();
        if (!searchQuery) return;

        if (searchContext === 'INSTRUMENT') {
            router.push(`/import-lc/details?id=${searchQuery}`);
        } else {
            // Transaction search might lead to a specific task or deep link
            router.push(`/approvals?transactionId=${searchQuery}`);
        }
        setSearchQuery('');
    };

    return (
        <div className="global-shell">
            <aside className="left-menu">
                <div className="sidebar-brand" style={{ padding: '0 0 2rem 0', color: 'white', fontWeight: 800, display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                    <div style={{ background: 'var(--nav-active-bg)', padding: '6px', borderRadius: '6px', display: 'flex' }}>
                        <Globe size={20} color="white" />
                    </div>
                    <span style={{ fontSize: '1.1rem', letterSpacing: '0.05em' }}>TRADEFINANCE</span>
                </div>
                
                <nav className="sidebar-nav">
                    {navSections.map(section => (
                        <div key={section.group} className="nav-section">
                            <li className="nav-header" style={{ listStyle: 'none' }}>{section.group}</li>
                            <ul className="nav-list" style={{ padding: 0 }}>
                                {section.items.map(item => (
                                    <li key={item.id} className={isActive(item.path) ? 'active' : ''}>
                                        <Link href={item.path} style={{ color: 'inherit', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                                            <span className="nav-icon">{item.icon}</span>
                                            <span className="nav-label">{item.label}</span>
                                        </Link>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    ))}
                </nav>

                <div className="sidebar-footer" style={{ marginTop: 'auto', padding: '1rem', borderTop: '1px solid rgba(255,255,255,0.1)', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                    <div className="user-avatar" style={{ width: '32px', height: '32px', borderRadius: '50%', background: 'var(--nav-active-bg)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.75rem', fontWeight: 700 }}>LN</div>
                    <div className="user-info">
                        <div className="user-name" style={{ fontSize: '0.875rem', fontWeight: 600 }}>Loc Nguyen</div>
                        <div className="user-role" style={{ fontSize: '0.7rem', opacity: 0.7 }}>Trade Officer</div>
                    </div>
                </div>
            </aside>

            <div className="main-wrapper">
                <header className="top-banner">
                    <div className="header-breadcrumbs">
                        <strong>Trade Finance</strong> / {pathname === '/import-lc' ? 'Dashboard' : (pathname || '').split('/').filter(p => !['import-lc', 'admin'].includes(p) && p !== '').map(p => p.charAt(0).toUpperCase() + p.slice(1)).join(' / ') || 'Home'}
                    </div>

                    <form className="global-search-container" onSubmit={handleSearch}>
                        <div className="search-box">
                            <div className="context-toggle">
                                <button 
                                    type="button"
                                    className={`context-btn ${searchContext === 'INSTRUMENT' ? 'active' : ''}`}
                                    onClick={() => setSearchContext('INSTRUMENT')}
                                >
                                    Inst
                                </button>
                                <button 
                                    type="button"
                                    className={`context-btn ${searchContext === 'TRANSACTION' ? 'active' : ''}`}
                                    onClick={() => setSearchContext('TRANSACTION')}
                                >
                                    Txn
                                </button>
                            </div>
                            <input 
                                type="text" 
                                placeholder={`Search by ${searchContext === 'INSTRUMENT' ? 'Instrument ID' : 'Transaction Ref'}...`}
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                            />
                            <div className="search-icon">
                                <Search size={16} />
                            </div>
                        </div>
                    </form>

                    <div className="sys-date">2026-04-28</div>
                </header>

                <main className="content">
                    {children}
                </main>
            </div>

            <style jsx>{`
                .global-search-container { position: absolute; left: 50%; transform: translateX(-50%); width: 100%; max-width: 480px; }
                .search-box { display: flex; align-items: center; background: #f1f5f9; border-radius: 999px; padding: 4px; border: 1px solid #e2e8f0; transition: all 0.2s; }
                .search-box:focus-within { background: white; border-color: #2563eb; box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1); }
                
                .context-toggle { display: flex; background: #e2e8f0; border-radius: 999px; padding: 2px; margin-right: 8px; }
                .context-btn { border: none; background: transparent; font-size: 10px; font-weight: 800; text-transform: uppercase; padding: 4px 10px; border-radius: 999px; cursor: pointer; color: #64748b; }
                .context-btn.active { background: white; color: #1e293b; box-shadow: 0 1px 2px rgba(0,0,0,0.1); }
                
                .search-box input { flex: 1; border: none; background: transparent; font-size: 0.8125rem; font-weight: 500; outline: none; padding: 4px 8px; color: #1e293b; }
                .search-icon { padding: 0 12px; color: #94a3b8; }

                .top-banner { position: relative; }
            `}</style>
        </div>
    );
};
