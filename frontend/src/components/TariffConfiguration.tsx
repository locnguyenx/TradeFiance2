'use client';

import React, { useState } from 'react';

// ABOUTME: Tariff & Fee Configuration implementing REQ-UI-CMN-05.
// ABOUTME: Manages complex pricing rules, MT610 SWIFT mappings, and multi-tier fee structures.

export const TariffConfiguration: React.FC = () => {
    const [activeSection, setActiveSection] = useState('Issuance Commission');

    return (
        <div className="tariff-container premium-card">
            <aside className="tariff-nav">
                <header className="nav-header">
                    <h3>Service Tariffs</h3>
                </header>
                <nav>
                    {['Issuance Commission', 'Amendment Fee', 'SWIFT Cable Charge', 'Claiming Bank Fees'].map(tab => (
                        <button 
                            key={tab} 
                            className={`nav-item ${activeSection === tab ? 'active' : ''}`}
                            onClick={() => setActiveSection(tab)}
                        >
                            {tab}
                        </button>
                    ))}
                </nav>
                <div className="status-footer">
                    <div className="approval-status pending">
                        <span className="dot"></span>
                        Pending Approval: 2 updates
                    </div>
                </div>
            </aside>

            <main className="tariff-content">
                <header className="content-header">
                    <div className="title-area">
                        <h1>{activeSection}</h1>
                        <p className="effective-date">Effective Date: 2026-06-01 (Current)</p>
                    </div>
                    <div className="action-area">
                        <button className="primary-btn">Publish New Tariff</button>
                    </div>
                </header>

                <section className="config-grid">
                    <div className="config-card premium-card">
                        <h3>Base Rule Set</h3>
                        <div className="form-group">
                            <label>Default Rate (%)</label>
                            <input type="number" defaultValue="0.125" step="0.001" />
                        </div>
                        <div className="form-group">
                            <label>Minimum Charge (USD)</label>
                            <input type="number" defaultValue="50.00" />
                        </div>
                        <div className="form-group">
                            <label>Charge Method</label>
                            <select defaultValue="PERCENTAGE">
                                <option value="PERCENTAGE">Percentage of Principal</option>
                                <option value="FLAT">Flat Fee per Instrument</option>
                            </select>
                        </div>
                    </div>

                    <div className="config-card premium-card">
                        <h3>Swift / MT610 Charge Mapping</h3>
                        <div className="mapping-item">
                            <label>SWIFT Field 71B Mapping</label>
                            <input type="text" defaultValue="/COMM/" placeholder="e.g. /COMM/ or /CABLE/" />
                        </div>
                        <div className="mapping-item">
                            <label>Charge Code</label>
                            <select defaultValue="ISSU">
                                <option value="ISSU">ISSU - Issuance</option>
                                <option value="AMND">AMND - Amendment</option>
                                <option value="CORR">CORR - Correspondence</option>
                            </select>
                        </div>
                    </div>
                </section>

                <section className="tier-section premium-card">
                    <h3>Exception / Tier Pricing Grid</h3>
                    <div className="table-wrapper">
                        <table className="tier-table">
                            <thead>
                                <tr>
                                    <th>Customer Risk Tier</th>
                                    <th>Volume Bracket</th>
                                    <th>Override Rate (%)</th>
                                    <th>Min Cap</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td><span className="tier-badge vip">VIP Corporate</span></td>
                                    <td>&gt; $10M / Year</td>
                                    <td>0.100</td>
                                    <td>$25.00</td>
                                    <td><button className="text-btn">Edit</button></td>
                                </tr>
                                <tr>
                                    <td><span className="tier-badge sme">SME Standard</span></td>
                                    <td>N/A</td>
                                    <td>0.125</td>
                                    <td>$50.00</td>
                                    <td><button className="text-btn">Edit</button></td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                    <button className="secondary-btn">+ Add Tier Exception</button>
                </section>
            </main>

            <style jsx>{`
                .tariff-container { display: flex; height: calc(100vh - 120px); overflow: hidden; background: white; border: none; }
                .tariff-nav { width: 280px; background: #f8fafc; border-right: 1px solid #e2e8f0; display: flex; flex-direction: column; }
                .nav-header { padding: 1.5rem; border-bottom: 1px solid #e2e8f0; }
                .nav-header h3 { margin: 0; font-size: 0.875rem; color: #64748b; text-transform: uppercase; letter-spacing: 0.05em; }
                
                .nav-item { width: 100%; text-align: left; padding: 1rem 1.5rem; border: none; background: none; font-size: 0.875rem; font-weight: 500; color: #475569; cursor: pointer; transition: all 0.2s; border-left: 3px solid transparent; }
                .nav-item:hover { background: #eff6ff; color: #2563eb; }
                .nav-item.active { background: #dbeafe; color: #2563eb; border-left-color: #2563eb; font-weight: 600; }

                .status-footer { margin-top: auto; padding: 1.5rem; border-top: 1px solid #e2e8f0; }
                .approval-status { font-size: 0.75rem; font-weight: 600; display: flex; align-items: center; gap: 0.5rem; }
                .approval-status.pending { color: #d97706; }
                .dot { width: 8px; height: 8px; border-radius: 50%; background: currentColor; }

                .tariff-content { flex: 1; padding: 2.5rem; overflow-y: auto; background: white; }
                .content-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 2.5rem; }
                .title-area h1 { margin: 0; font-size: 1.875rem; font-weight: 800; color: #0f172a; }
                .effective-date { margin: 0.5rem 0 0 0; color: #64748b; font-size: 0.875rem; }

                .config-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 2rem; margin-bottom: 2rem; }
                .config-card { padding: 1.5rem; }
                .config-card h3 { margin-top: 0; font-size: 1rem; color: #334155; margin-bottom: 1.5rem; }

                .form-group, .mapping-item { display: flex; flex-direction: column; gap: 0.5rem; margin-bottom: 1.25rem; }
                .form-group label, .mapping-item label { font-size: 0.75rem; font-weight: 700; color: #64748b; text-transform: uppercase; }
                .form-group input, .form-group select, .mapping-item input, .mapping-item select { padding: 0.625rem; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 0.875rem; }

                .tier-section { padding: 1.5rem; }
                .tier-section h3 { margin-top: 0; font-size: 1rem; color: #334155; margin-bottom: 1.5rem; }
                .tier-table { width: 100%; border-collapse: collapse; margin-bottom: 1.5rem; }
                .tier-table th { text-align: left; font-size: 0.75rem; color: #64748b; padding: 0.75rem; border-bottom: 2px solid #f1f5f9; text-transform: uppercase; }
                .tier-table td { padding: 1rem 0.75rem; border-bottom: 1px solid #f1f5f9; font-size: 0.875rem; color: #1e293b; }
                
                .tier-badge { font-size: 0.7rem; font-weight: 700; padding: 0.125rem 0.375rem; border-radius: 4px; }
                .tier-badge.vip { background: #dcfce7; color: #15803d; }
                .tier-badge.sme { background: #f1f5f9; color: #475569; }

                .primary-btn { background: #2563eb; color: white; border: none; padding: 0.75rem 1.5rem; border-radius: 6px; font-weight: 600; cursor: pointer; }
                .secondary-btn { background: white; color: #2563eb; border: 1px solid #2563eb; padding: 0.5rem 1rem; border-radius: 6px; font-weight: 600; cursor: pointer; }
                .text-btn { background: none; border: none; color: #2563eb; font-weight: 600; cursor: pointer; padding: 0; font-size: 0.8125rem; }
                
                .premium-card { background: white; border: 1px solid #e2e8f0; border-radius: 12px; box-shadow: 0 1px 2px rgba(0,0,0,0.05); }
            `}</style>
        </div>
    );
};
