'use client';

import React, { useState, useEffect } from 'react';
import { tradeApi } from '../api/tradeApi';
import { TradeParty } from '../api/types';

// ABOUTME: PartyDirectory implements KYC and Sanctions status monitoring for v3.0.
// ABOUTME: Master-Detail view for manageable trade finance counterparties.

export const PartyDirectory: React.FC = () => {
    const [parties, setParties] = useState<TradeParty[]>([]);
    const [selectedParty, setSelectedParty] = useState<TradeParty | null>(null);
    const [search, setSearch] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        loadParties();
    }, [search]);

    const loadParties = async () => {
        try {
            const data = await tradeApi.getParties(search);
            const partyList = data?.partyList || [];
            setParties(partyList);
            if (partyList.length > 0 && !selectedParty) {
                setSelectedParty(partyList[0]);
            }
        } catch (err) {
            setError('Failed to load party directory');
        } finally {
            setLoading(false);
        }
    };

    const handleSelectParty = (partyId: string) => {
        const party = (parties || []).find(p => p.partyId === partyId);
        if (party) setSelectedParty(party);
    };

    const getStatusColor = (status?: string) => {
        if (!status) return '#64748b';
        switch (status) {
            case 'KYC_PASSED':
            case 'SANCTIONS_CLEAN':
                return '#10b981';
            case 'KYC_PENDING':
                return '#f59e0b';
            case 'SANCTIONS_CHECK_FAILED':
            case 'KYC_REJECTED':
                return '#ef4444';
            default:
                return '#64748b';
        }
    };

    if (loading && (parties?.length || 0) === 0) return <div className="admin-loading">Loading directory...</div>;

    return (
        <div className="party-directory-layout">
            <aside className="party-list-pane">
                <header className="pane-header">
                    <h3>Counterparties</h3>
                    <div className="search-box">
                        <input 
                            type="text" 
                            placeholder="Search parties..." 
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                        />
                    </div>
                </header>
                <div className="party-items">
                    {(parties || []).map(p => (
                        <div 
                            key={p.partyId} 
                            className={`party-item ${selectedParty?.partyId === p.partyId ? 'active' : ''}`}
                            onClick={() => handleSelectParty(p.partyId)}
                        >
                            <span className="party-name">{p.partyName}</span>
                            <span className="party-id">{p.partyId}</span>
                        </div>
                    ))}
                </div>
            </aside>

            <main className="party-detail-pane">
                {selectedParty ? (
                    <>
                        <header className="pane-header detail-header">
                            <div className="title-group">
                                <h2>{selectedParty.partyName}</h2>
                                <span className="id-badge">{selectedParty.partyId}</span>
                            </div>
                            <div className="risk-indicator" style={{ borderLeft: `4px solid ${getStatusColor(selectedParty.kycStatusEnumId)}` }}>
                                <span className="label">Risk Rating</span>
                                <span className="value">{selectedParty.riskRating || 'UNRATED'}</span>
                            </div>
                        </header>

                        <div className="party-content">
                            <div className="status-grid">
                                <div className="status-card">
                                    <span className="card-label">KYC Status</span>
                                    <span className="card-value" style={{ color: getStatusColor(selectedParty.kycStatusEnumId) }}>
                                        {selectedParty.kycStatusEnumId?.replace('KYC_', '') || 'UNKNOWN'}
                                    </span>
                                    <span className="last-update">Last Updated: {selectedParty.lastKycUpdate || 'N/A'}</span>
                                </div>
                                <div className="status-card">
                                    <span className="card-label">Sanctions Status</span>
                                    <span className="card-value" style={{ color: getStatusColor(selectedParty.sanctionsStatusEnumId) }}>
                                        {selectedParty.sanctionsStatusEnumId?.replace('SANCTIONS_', '') || 'UNKNOWN'}
                                    </span>
                                    <span className="last-update">Real-time Check: ACTIVE</span>
                                </div>
                            </div>

                            <section className="info-section">
                                <h3>Organization Details</h3>
                                <div className="detail-row">
                                    <span className="label">Role Type</span>
                                    <span className="value">{selectedParty.roleTypeId}</span>
                                </div>
                                <div className="detail-row">
                                    <span className="label">Tax Identifier</span>
                                    <span className="value">Not Available in Directory</span>
                                </div>
                            </section>

                            <div className="compliance-banner">
                                <p>Transaction processing is permitted only for parties with PASSED KYC and CLEAN Sanctions status.</p>
                            </div>
                        </div>
                    </>
                ) : (
                    <div className="empty-selection">Select a party to view compliance details</div>
                )}
            </main>

            <style jsx>{`
                .party-directory-layout {
                    display: grid;
                    grid-template-columns: 320px 1fr;
                    background: white;
                    border-radius: 12px;
                    border: 1px solid #e2e8f0;
                    overflow: hidden;
                    min-height: 700px;
                    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
                }

                .pane-header {
                    padding: 1.5rem;
                    border-bottom: 1px solid #f1f5f9;
                    background: #f8fafc;
                }

                .search-box {
                    margin-top: 1rem;
                }

                .search-box input {
                    width: 100%;
                    padding: 0.5rem 0.75rem;
                    border: 1px solid #e2e8f0;
                    border-radius: 6px;
                    font-size: 0.875rem;
                }

                .party-list-pane {
                    border-right: 1px solid #f1f5f9;
                    background: #f8fafc;
                    display: flex;
                    flex-direction: column;
                }

                .party-items {
                    padding: 1rem;
                    display: flex;
                    flex-direction: column;
                    gap: 0.5rem;
                    overflow-y: auto;
                }

                .party-item {
                    padding: 1rem;
                    border-radius: 8px;
                    cursor: pointer;
                    display: flex;
                    flex-direction: column;
                    gap: 0.25rem;
                    transition: all 0.2s;
                    border: 1px solid transparent;
                }

                .party-item:hover { background: #f1f5f9; }
                .party-item.active { background: white; border-color: #2563eb; }

                .party-name { font-weight: 600; color: #1e293b; }
                .party-id { font-size: 0.75rem; color: #64748b; }

                .party-detail-pane { background: white; display: flex; flex-direction: column; }

                .detail-header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    background: white;
                }

                .title-group h2 { margin: 0; font-size: 1.5rem; color: #1e293b; }
                .id-badge { font-size: 0.75rem; font-weight: 700; color: #64748b; background: #f1f5f9; padding: 0.2rem 0.5rem; border-radius: 4px; }

                .risk-indicator {
                    padding-left: 1rem;
                    display: flex;
                    flex-direction: column;
                }

                .risk-indicator .label { font-size: 0.75rem; font-weight: 700; color: #64748b; text-transform: uppercase; }
                .risk-indicator .value { font-size: 1.125rem; font-weight: 800; color: #1e293b; }

                .party-content { padding: 2rem; display: flex; flex-direction: column; gap: 2.5rem; }

                .status-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }

                .status-card {
                    padding: 1.5rem;
                    background: #f8fafc;
                    border-radius: 12px;
                    border: 1px solid #f1f5f9;
                    display: flex;
                    flex-direction: column;
                    gap: 0.5rem;
                }

                .card-label { font-size: 0.875rem; font-weight: 600; color: #64748b; }
                .card-value { font-size: 1.25rem; font-weight: 800; }
                .last-update { font-size: 0.75rem; color: #94a3b8; }

                .info-section h3 {
                    margin: 0 0 1rem 0;
                    font-size: 0.875rem;
                    text-transform: uppercase;
                    color: #64748b;
                    border-bottom: 1px solid #f1f5f9;
                    padding-bottom: 0.5rem;
                }

                .detail-row {
                    display: flex;
                    justify-content: space-between;
                    padding: 0.75rem 0;
                    border-bottom: 1px solid #f8fafc;
                }

                .detail-row .label { font-size: 0.875rem; color: #64748b; }
                .detail-row .value { font-size: 0.875rem; font-weight: 600; color: #1e293b; }

                .compliance-banner {
                    padding: 1rem;
                    background: #f0fdf4;
                    border: 1px solid #dcfce7;
                    border-radius: 8px;
                    color: #166534;
                    font-size: 0.875rem;
                    font-weight: 500;
                    text-align: center;
                }

                .empty-selection { display: flex; justify-content: center; align-items: center; height: 100%; color: #94a3b8; font-style: italic; }
                .admin-loading { padding: 2rem; text-align: center; color: #64748b; }
            `}</style>
        </div>
    );
};
