'use client';

import React, { ReactNode } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
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
    Ship
} from 'lucide-react';

// ABOUTME: Modernized Global application shell implementing REQ-UI-MOD-01.
// ABOUTME: Uses lucide-react icons for high-density minimalist sidebar with grouped modules.

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

    const navSections: NavSection[] = [
        { group: 'OPERATIONS', items: [
            { id: 'dashboard', label: 'Operations Dashboard', icon: <LayoutDashboard size={18} />, path: '/import-lc' },
            { id: 'approvals', label: 'My Tasks (Approvals)', icon: <CheckCircle size={18} />, path: '/approvals' },
            { id: 'issuance', label: 'New LC Issuance', icon: <PlusCircle size={18} />, path: '/issuance' },
            { id: 'documents', label: 'Document Examination', icon: <FileSearch size={18} />, path: '/import-lc/documents' },
        ]},
        { group: 'LIFECYCLE MANAGEMENT', items: [
            { id: 'amendments', label: 'LC Amendments', icon: <FileEdit size={18} />, path: '/import-lc/amendments' },
            { id: 'presentations', label: 'Presentations', icon: <FileText size={18} />, path: '/import-lc/presentations' },
            { id: 'settlement', label: 'Settlements', icon: <CreditCard size={18} />, path: '/import-lc/settlement' },
            { id: 'guarantees', label: 'Shipping Guarantees', icon: <ShieldAlert size={18} />, path: '/import-lc/shipping-guarantees' },
            { id: 'cancellations', label: 'Cancellations', icon: <XCircle size={18} />, path: '/import-lc/cancellations' },
        ]},
        { group: 'MASTER DATA', items: [
            { id: 'parties', label: 'Party Directory', icon: <Users size={18} />, path: '/parties' },
            { id: 'facilities', label: 'Credit Facilities', icon: <BarChart4 size={18} />, path: '/facilities' },
            { id: 'tariffs', label: 'Tariff & Fee Mapping', icon: <Database size={18} />, path: '/tariffs' },
            { id: 'product', label: 'Product Config', icon: <Settings2 size={18} />, path: '/admin/product' },
        ]},
        { group: 'ADMINISTRATION', items: [
            { id: 'tiers', label: 'Authority Tiers', icon: <ShieldCheck size={18} />, path: '/admin/tiers' },
            { id: 'logs', label: 'System Audit Logs', icon: <History size={18} />, path: '/admin/logs' },
        ]}
    ];

    const isActive = (href: string) => {
        const currentPath = (pathname || '/').replace(/\/$/, '') || '/';
        const targetPath = href.replace(/\/$/, '') || '/';
        
        if (targetPath === '/import-lc' && currentPath === '/') return true;
        return currentPath === targetPath || (targetPath !== '/' && currentPath.startsWith(targetPath));
    };

    return (
        <div className="global-shell">
            <aside className="left-menu">
                <div className="sidebar-brand" style={{ padding: '0 0 2rem 0', color: 'white', fontWeight: 800, display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                    <div style={{ background: 'var(--nav-active-bg)', padding: '6px', borderRadius: '6px', display: 'flex' }}>
                        <Globe size={20} color="white" />
                    </div>
                    <span style={{ fontSize: '1.1rem', letterSpacing: '0.05em' }}>TRADE FINANCE</span>
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
            </aside>

            <div className="main-wrapper">
                <header className="top-banner">
                    <div className="header-breadcrumbs">
                        <strong>Trade Finance</strong> / {pathname === '/import-lc' ? 'Dashboard' : (pathname || '').split('/').filter(p => !['import-lc', 'admin'].includes(p) && p !== '').map(p => p.charAt(0).toUpperCase() + p.slice(1)).join(' / ') || 'Home'}
                    </div>
                    <div className="sys-date">2026-04-22</div>
                </header>

                <main className="content">
                    {children}
                </main>
            </div>

        </div>
    );
};
