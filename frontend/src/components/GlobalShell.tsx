'use client';

import React, { ReactNode } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';

// ABOUTME: Modernized Global application shell implementing REQ-UI-MOD-01.
// ABOUTME: Uses high-density minimalist sidebar with solid emerald active states.

export const GlobalShell: React.FC<{ children: ReactNode }> = ({ children }) => {
    const pathname = usePathname();

    const navSections = [
        {
            title: 'Workspace',
            items: [
                { label: 'Operations Dashboard', href: '/import-lc' },
                { label: 'My Tasks', href: '/approvals' },
            ]
        },
        {
            title: 'Import LC Module',
            items: [
                { label: 'New LC Issuance', href: '/issuance' },
                { label: 'Document Examination', href: '/import-lc/documents' },
            ]
        },
        {
            title: 'Master Data',
            items: [
                { label: 'Party Directory', href: '/parties' },
                { label: 'Credit Facilities', href: '/facilities' },
                { label: 'Tariff & Fee Mapping', href: '/tariffs' },
            ]
        }
    ];

    const isActive = (href: string) => pathname === href;

    return (
        <div className="global-shell">
            <aside className="global-sidebar">
                <div className="sidebar-brand">
                    <div className="brand-icon"></div>
                    <span className="brand-text">TRADEFINANCE</span>
                </div>
                
                <nav className="sidebar-nav">
                    {navSections.map(section => (
                        <div key={section.title} className="nav-section">
                            <h5 className="section-title">{section.title}</h5>
                            <ul className="nav-list">
                                {section.items.map(item => (
                                    <li key={item.label} className="nav-item-wrapper">
                                        <Link href={item.href} legacyBehavior>
                                            <a className={`nav-link ${isActive(item.href) ? 'active' : ''}`}>
                                                <span className="nav-icon"></span>
                                                <span className="nav-label">{item.label}</span>
                                            </a>
                                        </Link>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    ))}
                </nav>

                <div className="sidebar-footer">
                    <div className="user-profile-compact">
                        <div className="user-avatar">L</div>
                        <div className="user-meta">
                            <span className="name">Loc Nguyen</span>
                            <span className="role">Maker / Admin</span>
                        </div>
                    </div>
                </div>
            </aside>

            <div className="main-container">
                <header className="app-header">
                    <div className="header-breadcrumbs">
                        <span className="breadcrumb-root">Dashboard</span>
                        <span className="breadcrumb-separator">/</span>
                        <span className="breadcrumb-current">Overview</span>
                    </div>
                    <div className="header-actions">
                        <div className="search-field">
                            <input type="text" placeholder="Search..." />
                        </div>
                        <div className="sys-date">2026-04-22</div>
                    </div>
                </header>

                <main className="main-content">
                    {children}
                </main>
            </div>

            <style jsx>{`
                .global-shell { display: flex; height: 100vh; width: 100vw; overflow: hidden; background: var(--app-bg); }
                
                .global-sidebar { 
                    width: 260px; background: var(--nav-bg); border-right: 1px solid var(--border-main); 
                    display: flex; flex-direction: column; flex-shrink: 0;
                }
                
                .sidebar-brand { padding: 1.5rem; display: flex; align-items: center; gap: 0.75rem; }
                .brand-icon { width: 24px; height: 24px; background: var(--nav-active-bg); border-radius: 4px; }
                .brand-text { font-size: 1rem; font-weight: 800; color: var(--text-primary); letter-spacing: 0.05em; }

                .sidebar-nav { flex: 1; padding: 1rem 0; overflow-y: auto; }
                .nav-section { margin-bottom: 1.5rem; }
                .section-title { padding: 0 1.5rem; color: var(--text-secondary); font-size: 0.65rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.1em; margin-bottom: 0.5rem; }
                
                .nav-list { list-style: none; padding: 0 0.75rem; }
                .nav-link { 
                    display: flex; align-items: center; gap: 0.75rem; padding: 0.625rem 0.75rem; 
                    color: var(--text-primary); text-decoration: none; font-size: 0.875rem; font-weight: 500; 
                    border-radius: 6px; transition: all 150ms; 
                }
                .nav-icon { width: 16px; height: 16px; border: 1.5px solid currentColor; border-radius: 3px; opacity: 0.7; }
                
                .nav-link:hover { background: #f1f5f9; }
                .nav-link.active { background: var(--nav-active-bg); color: var(--nav-active-text); }
                .nav-link.active .nav-icon { border-color: white; opacity: 1; }

                .sidebar-footer { padding: 1rem 1.5rem; border-top: 1px solid var(--border-main); }
                .user-profile-compact { display: flex; align-items: center; gap: 0.75rem; }
                .user-avatar { width: 32px; height: 32px; background: #e2e8f0; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 600; font-size: 0.8rem; }
                .user-meta { display: flex; flex-direction: column; }
                .name { font-size: 0.8125rem; font-weight: 600; color: var(--text-primary); }
                .role { font-size: 0.6875rem; color: var(--text-secondary); }

                .main-container { flex: 1; display: flex; flex-direction: column; min-width: 0; }
                .app-header { 
                    height: 56px; background: white; border-bottom: 1px solid var(--border-main); 
                    display: flex; align-items: center; justify-content: space-between; padding: 0 1.5rem; flex-shrink: 0; 
                }
                .header-breadcrumbs { display: flex; align-items: center; gap: 0.5rem; font-size: 0.8125rem; }
                .breadcrumb-root { color: var(--text-secondary); font-weight: 500; }
                .breadcrumb-separator { color: var(--border-main); }
                .breadcrumb-current { color: var(--text-primary); font-weight: 600; }

                .header-actions { display: flex; align-items: center; gap: 1.5rem; }
                .search-field input { width: 220px; padding: 0.375rem 0.75rem; background: #f8fafc; border: 1px solid var(--border-main); border-radius: 6px; font-size: 0.8125rem; }
                .sys-date { font-size: 0.8125rem; font-weight: 600; color: var(--text-secondary); }

                .main-content { flex: 1; overflow-y: auto; padding: 1.5rem; }
            `}</style>
        </div>
    );
};
