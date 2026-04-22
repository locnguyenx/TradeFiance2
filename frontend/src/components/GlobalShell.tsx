import React, { ReactNode } from 'react';

// ABOUTME: GlobalShell component provides the main layout and navigation for the Trade Finance application.
// ABOUTME: Strictly follows REQ-UI-CMN-01 navigation structure.

export const GlobalShell: React.FC<{ children: ReactNode }> = ({ children }) => {
    return (
        <div className="global-shell">
            <header className="top-banner">
                <span>Business Date: 2026-04-21 | Role: Maker</span>
                <span>Global Search</span>
            </header>
            <nav className="left-menu">
                <ul>
                    <li className="nav-header">Workspace</li>
                    <li>Dashboard</li>
                    <li>My Approvals</li>
                    
                    <li className="nav-header">Trade Modules</li>
                    <li>Import LC</li>
                    <li>Export LC (Phase 2)</li>
                    <li>Collections (Phase 2)</li>
                    
                    <li className="nav-header">Master Data</li>
                    <li>Party &amp; KYC Directory</li>
                    <li>Credit Facilities</li>
                    <li>Tariff &amp; Fee Configuration</li>
                    <li>Product Configuration</li>
                    
                    <li className="nav-header">System Admin</li>
                    <li>User Authority Tiers</li>
                    <li>Audit Logs</li>
                </ul>
            </nav>
            <main className="content">{children}</main>
        </div>
    );
};
