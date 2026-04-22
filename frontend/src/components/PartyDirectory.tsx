'use client';

import React, { useState } from 'react';

// ABOUTME: Party Directory & KYC Management implementing REQ-UI-CMN-03.
// ABOUTME: Provides a split-pane interface for compliance officers to track party eligibility and AML status.

interface Party {
    id: string;
    name: string;
    role: string;
    kycStatus: 'Verified' | 'Expired' | 'Pending';
    amlStatus: 'Clear' | 'Alert' | 'Pending';
    lastVerification: string;
    riskRating: 'Low' | 'Medium' | 'High';
}

const mockParties: Party[] = [
    { id: 'P001', name: 'Global Corp', role: 'Applicant', kycStatus: 'Verified', amlStatus: 'Clear', lastVerification: '2026-01-15', riskRating: 'Low' },
    { id: 'P002', name: 'Export Ltd', role: 'Beneficiary', kycStatus: 'Verified', amlStatus: 'Clear', lastVerification: '2026-02-10', riskRating: 'Low' },
    { id: 'P003', name: 'Fast Trade SA', role: 'Applicant', kycStatus: 'Expired', amlStatus: 'Pending', lastVerification: '2025-05-20', riskRating: 'Medium' },
];

export const PartyDirectory: React.FC = () => {
    const [selectedParty, setSelectedParty] = useState<Party>(mockParties[0]);
    const [searchTerm, setSearchTerm] = useState('');
    const [activeTab, setActiveTab] = useState<'kyc' | 'credit' | 'history' | 'roles' | 'compliance'>('kyc');

    const filteredParties = mockParties.filter(p => 
        p.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
        p.id.toLowerCase().includes(searchTerm.toLowerCase())
    );

    return (
        <div className="directory-container premium-card">
            <aside className="party-sidebar" style={{ width: '380px' }}>
                <header className="sidebar-header">
                    <h2>Party Directory</h2>
                    <input 
                        type="text" 
                        placeholder="Search by Name or ID..." 
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="search-input"
                    />
                </header>
                <div className="party-list">
                    <table className="list-table">
                        <thead>
                            <tr>
                                <th>Party Name</th>
                                <th>KYC</th>
                                <th>AML</th>
                            </tr>
                        </thead>
                        <tbody>
                            {filteredParties.map(p => (
                                <tr 
                                    key={p.id} 
                                    className={selectedParty?.id === p.id ? 'selected' : ''}
                                    onClick={() => setSelectedParty(p)}
                                >
                                    <td className="party-name">
                                        <div>{p.name}</div>
                                        <span className="party-id-small">{p.id}</span>
                                    </td>
                                    <td>
                                        <span className={`status-tag ${p.kycStatus}`}>{p.kycStatus}</span>
                                    </td>
                                    <td>
                                        <span className={`aml-tag ${p.amlStatus}`}>{p.amlStatus}</span>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </aside>

            <main className="party-main-content">
                <header className="details-header">
                    <div className="header-meta">
                        <div className="badge-row">
                            <span className="category-badge">CORPORATE ENTITY</span>
                            <span className="risk-level">RISK: {selectedParty?.riskRating}</span>
                        </div>
                        <h1>{selectedParty?.name}</h1>
                        <span className="party-id-large">Identification: {selectedParty?.id}</span>
                    </div>
                    <div className="tab-nav" role="tablist">
                        {['kyc', 'compliance', 'roles', 'credit', 'history'].map((tab) => (
                            <button 
                                key={tab}
                                className={`tab-btn ${activeTab === tab ? 'active' : ''}`}
                                onClick={() => setActiveTab(tab as any)}
                            >
                                {tab.charAt(0).toUpperCase() + tab.slice(1)}
                            </button>
                        ))}
                    </div>
                </header>

                <div className="details-view">
                    {activeTab === 'kyc' && (
                        <div className="details-grid">
                            <section className="info-card">
                                <h3>KYC & Vetting Details</h3>
                                <div className="data-row">
                                    <span className="label">Verification Date</span>
                                    <span className="value">{selectedParty?.lastVerification}</span>
                                </div>
                                <div className="data-row">
                                    <span className="label">Renewal Due</span>
                                    <span className="value text-warning">2027-01-15</span>
                                </div>
                                <div className="data-row">
                                    <span className="label">Ultimate Beneficial Owner</span>
                                    <span className="value">John Doe (Validated)</span>
                                </div>
                            </section>
                        </div>
                    )}

                    {activeTab === 'compliance' && (
                        <div className="details-grid">
                            <section className="info-card">
                                <h3>Compliance Narrative & Flags</h3>
                                <div className="narrative">
                                    No adverse media found. Sanctions screening clear as of 2026-04-22.
                                    Entity has been active in Trade Finance for 5+ years without incident.
                                </div>
                                <div className="flag-list">
                                    <div className="flag-item green">UN Sanctions: CLEAR</div>
                                    <div className="flag-item green">OFAC List: CLEAR</div>
                                    <div className="flag-item yellow">PEP Connect: TRACE (Non-Controlling)</div>
                                </div>
                            </section>
                        </div>
                    )}

                    {activeTab === 'roles' && (
                        <div className="details-grid">
                            <section className="info-card">
                                <h3>Authorized Capacity</h3>
                                <div className="role-grid">
                                    <div className="role-box">
                                        <div className="role-title">Applicant</div>
                                        <div className="role-stat">ACTIVE</div>
                                    </div>
                                    <div className="role-box inactive">
                                        <div className="role-title">Beneficiary</div>
                                        <div className="role-stat">PENDING</div>
                                    </div>
                                    <div className="role-box">
                                        <div className="role-title">Payer</div>
                                        <div className="role-stat">ACTIVE</div>
                                    </div>
                                </div>
                            </section>
                        </div>
                    )}

                    {activeTab === 'credit' && (
                        <div className="details-grid">
                            <section className="info-card">
                                <h3>Allocated Facilities</h3>
                                <div className="facility-item">
                                    <div className="fac-header">
                                        <span className="fac-name">Import LC Line</span>
                                        <span className="fac-amount">$5,000,000</span>
                                    </div>
                                    <div className="fac-progress">
                                        <div className="bar" style={{ width: '65%' }}></div>
                                    </div>
                                    <div className="fac-meta">Utilization: 65% ($3,250,000)</div>
                                </div>
                            </section>
                        </div>
                    )}

                    {activeTab === 'history' && (
                        <div className="empty-history">
                            <div className="icon">📂</div>
                            <p>No transactions found for the selected period.</p>
                        </div>
                    )}
                </div>
            </main>

            <style jsx>{`
                .directory-container { display: flex; height: calc(100vh - 120px); overflow: hidden; background: white; }
                .party-sidebar { width: 350px; border-right: 1px solid #e2e8f0; display: flex; flex-direction: column; background: #f8fafc; }
                .sidebar-header { padding: 1.5rem; border-bottom: 1px solid #e2e8f0; }
                .search-input { width: 100%; padding: 0.5rem 0.75rem; border: 1px solid #cbd5e1; border-radius: 6px; margin-top: 1rem; }
                
                .party-list { flex: 1; overflow-y: auto; }
                .list-table { width: 100%; border-collapse: collapse; font-size: 0.8125rem; }
                .list-table th { text-align: left; padding: 1rem 1.5rem; font-weight: 600; color: #64748b; background: #f1f5f9; position: sticky; top: 0; }
                .list-table td { padding: 1rem 1.5rem; border-bottom: 1px solid #f1f5f9; cursor: pointer; transition: background 0.2s; }
                .list-table tr:hover { background: #eff6ff; }
                .list-table tr.selected { background: #dbeafe; border-left: 4px solid #2563eb; }
                
                .party-name { font-weight: 600; color: #1e293b; }
                .role-chip { font-size: 0.7rem; padding: 0.125rem 0.375rem; background: #e2e8f0; border-radius: 4px; color: #475569; font-weight: 700; text-transform: uppercase; }
                .status-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 0.5rem; }
                .status-dot.Verified { background: #10b981; }
                .status-dot.Expired { background: #ef4444; }
                .status-dot.Pending { background: #f59e0b; }

                .party-main-content { flex: 1; padding: 2.5rem; overflow-y: auto; background: white; display: flex; flex-direction: column; }
                .details-header { margin-bottom: 2.5rem; padding-bottom: 1.5rem; border-bottom: 1px solid #f1f5f9; display: flex; justify-content: space-between; align-items: flex-start; }
                .header-meta h1 { font-size: 1.875rem; font-weight: 800; color: #0f172a; margin: 0; }
                .party-id { color: #64748b; font-size: 0.875rem; font-family: monospace; }

                .tab-nav { display: flex; gap: 0.5rem; background: #f8fafc; padding: 0.25rem; border-radius: 8px; border: 1px solid #e2e8f0; }
                .tab-btn { padding: 0.5rem 1rem; border: none; background: none; border-radius: 6px; font-size: 0.875rem; font-weight: 600; color: #64748b; cursor: pointer; transition: all 0.2s; }
                .tab-btn:hover { color: #1e293b; background: #f1f5f9; }
                .tab-btn.active { background: white; color: #2563eb; box-shadow: 0 1px 2px rgba(0,0,0,0.05); }

                .details-view { flex: 1; }
                .details-grid { display: grid; grid-template-columns: 1fr; gap: 2rem; }
                .kyc-section, .limits-section { padding: 1.5rem; }
                .kyc-section h3, .limits-section h3 { margin-top: 0; font-size: 1rem; margin-bottom: 1.5rem; color: #334155; }
                
                .property { display: flex; justify-content: space-between; margin-bottom: 1rem; font-size: 0.875rem; }
                .property-label { color: #64748b; font-weight: 500; }
                
                .status-badge { font-weight: 700; font-size: 0.75rem; text-transform: uppercase; padding: 0.25rem 0.5rem; border-radius: 4px; }
                .status-badge.Clear { background: #ecfdf5; color: #059669; }
                .status-badge.Alert { background: #fef2f2; color: #dc2626; }
                
                .risk-Low { color: #059669; font-weight: 700; }
                .risk-Medium { color: #d97706; font-weight: 700; }
                .risk-High { color: #dc2626; font-weight: 700; }

                .facility-item { display: flex; justify-content: space-between; padding: 0.75rem; background: #f8fafc; border-radius: 8px; margin-bottom: 0.75rem; }
                .facility-ref { font-weight: 600; color: #2563eb; font-size: 0.8125rem; }
                .facility-amount { font-weight: 700; color: #1e293b; }
            `}</style>
        </div>
    );
};
