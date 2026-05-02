'use client';

import React, { useState, useEffect } from 'react';
import { tradeApi } from '../api/tradeApi';
import { TradeParty } from '../api/types';
import { PartyModal } from './PartyModal';


// ABOUTME: PartyDirectory implements KYC and Sanctions status monitoring for v3.0.
// ABOUTME: Master-Detail view for manageable trade finance counterparties.

export const PartyDirectory: React.FC = () => {
    const [parties, setParties] = useState<TradeParty[]>([]);
    const [selectedParty, setSelectedParty] = useState<TradeParty | null>(null);
    const [search, setSearch] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [showModal, setShowModal] = useState(false);
    const [editingParty, setEditingParty] = useState<TradeParty | null>(null);
    const [isHydrated, setIsHydrated] = useState(false);



    useEffect(() => {
        setIsHydrated(true);
        loadParties();
    }, [search]);


    const loadParties = async () => {
        try {
            const data = await tradeApi.getParties(search);
            const partyList = data?.partyList || [];
            setParties(partyList);
            if (partyList.length > 0 && !selectedParty) {
                setSelectedParty(partyList[0]);
            } else if (selectedParty) {
                // Refresh selected party if it exists
                const updated = partyList.find(p => p.partyId === selectedParty.partyId);
                if (updated) setSelectedParty(updated);
            }
        } catch (err) {
            setError('Failed to load party directory');
        } finally {
            setLoading(false);
        }
    };

    const handleCreate = () => {
        setEditingParty(null);
        setShowModal(true);
    };

    const handleEdit = () => {
        setEditingParty(selectedParty);
        setShowModal(true);
    };


    const handleSelectParty = async (partyId: string) => {
        try {
            const party = await tradeApi.getParty(partyId);
            setSelectedParty(party);
        } catch (err) {
            console.error('Failed to load party details:', err);
            // Fallback to list data
            const party = (parties || []).find(p => p.partyId === partyId);
            if (party) setSelectedParty(party);
        }
    };


    const getStatusColor = (status?: string) => {
        if (!status) return '#64748b';
        const s = status.toUpperCase();
        if (s === 'ACTIVE' || s === 'PASSED' || s === 'KYC_PASSED' || s === 'SANCTION_CLEAR' || s === 'SANCTIONS_CLEAN' || s === 'CLEAR' || s === 'CLEAN') {
            return '#10b981';
        }
        if (s === 'PENDING' || s === 'KYC_PENDING' || s === 'SANCTION_PENDING') {
            return '#f59e0b';
        }
        if (s === 'REJECTED' || s === 'KYC_REJECTED' || s === 'BLOCKED' || s === 'SANCTION_BLOCKED' || s === 'FAILED') {
            return '#ef4444';
        }
        return '#64748b';
    };


    if (loading && (parties?.length || 0) === 0) return <div className="admin-loading">Loading directory...</div>;

    return (
        <div className="party-directory-layout">
            <aside className="party-list-pane">
                <header className="pane-header">
                    <div className="header-top">
                        <h3>Counterparties</h3>
                        <button className="add-party-btn" onClick={handleCreate}>+ New Party</button>
                    </div>
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
                                <div className="name-row">
                                    <h2>{selectedParty.partyName}</h2>
                                    <button className="edit-profile-btn" onClick={handleEdit}>Edit Profile</button>
                                </div>
                                <span className="id-badge">{selectedParty.partyId}</span>
                            </div>
                            <div className="risk-indicator" style={{ borderLeft: `4px solid ${getStatusColor(selectedParty.kycStatus)}` }}>
                                <span className="label">Risk Rating</span>
                                <span className="value">{selectedParty.riskRating || 'UNRATED'}</span>
                            </div>
                        </header>

                        <div className="party-content">
                            <div className="status-grid">
                                <div className="status-card">
                                    <span className="card-label">KYC Status</span>
                                    <span className="card-value" style={{ color: getStatusColor(selectedParty.kycStatus) }}>
                                        {selectedParty.kycStatus?.toUpperCase() || 'UNKNOWN'}
                                    </span>
                                    <span className="last-update">Last Updated: {selectedParty.lastKycUpdate || 'N/A'}</span>
                                </div>
                                <div className="status-card">
                                    <span className="card-label">Sanctions Status</span>
                                    <span className="card-value" style={{ color: getStatusColor(selectedParty.sanctionsStatus) }}>
                                        {selectedParty.sanctionsStatus?.replace('SANCTION_', '') || 'UNKNOWN'}
                                    </span>
                                    <span className="last-update">Real-time Check: ACTIVE</span>
                                </div>
                            </div>


                            <section className="info-section">
                                <h3>Organization Details</h3>
                                <div className="detail-row">
                                    <span className="label">Role Type</span>
                                    <span className="value">{selectedParty.partyTypeEnumId?.replace('PARTY_', '') || 'COMMERCIAL'}</span>
                                </div>
                                <div className="detail-row">
                                    <span className="label">Country of Risk</span>
                                    <span className="value">{selectedParty.countryOfRisk || 'N/A'}</span>
                                </div>
                                <div className="detail-row">
                                    <span className="label">Registered Address</span>
                                    <span className="value">{selectedParty.registeredAddress || 'Not Provided'}</span>
                                </div>
                                <div className="detail-row">
                                    <span className="label">Default Account</span>
                                    <span className="value">{selectedParty.accountNumber || 'N/A'}</span>
                                </div>
                            </section>

                            {selectedParty.partyTypeEnumId === 'PARTY_BANK' && (
                                <section className="info-section bank-details">
                                    <h3>Banking & Connectivity</h3>
                                    <div className="detail-row">
                                        <span className="label">SWIFT BIC</span>
                                        <span className="value">{selectedParty.swiftBic || 'N/A'}</span>
                                    </div>
                                    <div className="detail-row">
                                        <span className="label">Active RMA</span>
                                        <span className="value">{selectedParty.hasActiveRMA === 'Y' ? 'YES' : 'NO'}</span>
                                    </div>
                                    <div className="detail-row">
                                        <span className="label">Clearing Code</span>
                                        <span className="value">{selectedParty.clearingCode || 'N/A'}</span>
                                    </div>
                                    <div className="detail-row">
                                        <span className="label">Nostro Reference</span>
                                        <span className="value">{selectedParty.nostroAccountRef || 'None'}</span>
                                    </div>
                                </section>
                            )}

                            <div className="compliance-banner">
                                <p>Transaction processing is permitted only for parties with PASSED KYC and CLEAN Sanctions status.</p>
                            </div>

                        </div>
                    </>
                ) : (
                    <div className="empty-selection">Select a party to view compliance details</div>
                )}
            </main>

            {showModal && (
                <PartyModal 
                    party={editingParty} 
                    onClose={() => setShowModal(false)} 
                    onSuccess={loadParties}
                />
            )}


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

                .header-top {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                }

                .add-party-btn {
                    background: #2563eb;
                    color: white;
                    border: none;
                    border-radius: 6px;
                    padding: 0.4rem 0.8rem;
                    font-size: 0.75rem;
                    font-weight: 600;
                    cursor: pointer;
                    transition: background 0.2s;
                }
                .add-party-btn:hover { background: #1d4ed8; }


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
                .name-row { display: flex; align-items: center; gap: 1rem; }
                .edit-profile-btn {
                    background: white;
                    color: #2563eb;
                    border: 1px solid #dbeafe;
                    border-radius: 4px;
                    padding: 0.25rem 0.5rem;
                    font-size: 0.7rem;
                    font-weight: 600;
                    cursor: pointer;
                }
                .edit-profile-btn:hover { background: #f0f7ff; }
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

                .bank-details {
                    background: #f0f7ff;
                    padding: 1rem;
                    border-radius: 8px;
                    border: 1px solid #dbeafe;
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
