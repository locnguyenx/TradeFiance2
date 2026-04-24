'use client';

'use client';

import React, { useState, useEffect } from 'react';
import { tradeApi } from '../api/tradeApi';
import { UserAuthorityProfile } from '../api/types';

// ABOUTME: UserAuthorityManager implements REQ-UI-CMN-02/06 logic.
// ABOUTME: Manages user delegation tiers, approval limits, and suspension status.

export const UserAuthorityManager: React.FC = () => {
    const [profiles, setProfiles] = useState<UserAuthorityProfile[]>([]);
    const [selectedProfile, setSelectedProfile] = useState<UserAuthorityProfile | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        loadProfiles();
    }, []);

    const loadProfiles = async () => {
        try {
            const data = await tradeApi.getUserAuthorityProfiles();
            setProfiles(data.profileList);
            if (data.profileList.length > 0 && !selectedProfile) {
                setSelectedProfile(data.profileList[0]);
            }
        } catch (err) {
            setError('Failed to load authority profiles');
        } finally {
            setLoading(false);
        }
    };

    const handleSelectProfile = (profile: UserAuthorityProfile) => {
        setSelectedProfile({ ...profile });
    };

    const handleFieldChange = (field: keyof UserAuthorityProfile, value: any) => {
        if (!selectedProfile) return;
        setSelectedProfile({
            ...selectedProfile,
            [field]: value
        });
    };

    const handleSave = async () => {
        if (!selectedProfile) return;
        setSaving(true);
        try {
            await tradeApi.updateUserAuthorityProfile(selectedProfile.authorityProfileId, selectedProfile);
            await loadProfiles();
        } catch (err) {
            setError('Failed to save authority profile');
        } finally {
            setSaving(false);
        }
    };

    if (loading) return <div className="admin-loading">Loading authority profiles...</div>;

    return (
        <div className="authority-manager-layout">
            <aside className="user-list-pane">
                <header className="pane-header">
                    <h3>Users</h3>
                </header>
                <div className="user-items">
                    {profiles.map(p => (
                        <div 
                            key={p.authorityProfileId} 
                            className={`user-item ${selectedProfile?.authorityProfileId === p.authorityProfileId ? 'active' : ''}`}
                            onClick={() => handleSelectProfile(p)}
                        >
                            <span className="user-id">{p.userId}</span>
                            <span className="user-tier">{p.authorityTierEnumId}</span>
                        </div>
                    ))}
                </div>
            </aside>

            <main className="authority-detail-pane">
                {selectedProfile ? (
                    <>
                        <header className="pane-header detail-header">
                            <h2>User Authority: {selectedProfile.userId}</h2>
                            <div className="action-bar">
                                <button 
                                    className="primary-btn" 
                                    onClick={handleSave}
                                    disabled={saving}
                                >
                                    {saving ? 'Saving...' : 'Save Changes'}
                                </button>
                            </div>
                        </header>

                        <div className="config-form">
                            <section className="form-section">
                                <h3>Delegation & Limits</h3>
                                <div className="field-group">
                                    <div className="field">
                                        <label htmlFor="authorityTier">Authority Tier</label>
                                        <select 
                                            id="authorityTier"
                                            value={selectedProfile.authorityTierEnumId}
                                            onChange={(e) => handleFieldChange('authorityTierEnumId', e.target.value)}
                                        >
                                            <option value="TIER_1">Tier 1 - Maker</option>
                                            <option value="TIER_2">Tier 2 - Junior Checker</option>
                                            <option value="TIER_3">Tier 3 - Senior Checker</option>
                                            <option value="TIER_4">Tier 4 - Executive / Dual-Auth</option>
                                        </select>
                                    </div>
                                    <div className="field">
                                        <label htmlFor="maxApprovalAmount">Max Approval Limit</label>
                                        <div className="amount-input-group">
                                            <span className="currency-addon">{selectedProfile.currencyUomId}</span>
                                            <input 
                                                id="maxApprovalAmount"
                                                type="number" 
                                                value={selectedProfile.maxApprovalAmount}
                                                onChange={(e) => handleFieldChange('maxApprovalAmount', parseFloat(e.target.value))}
                                            />
                                        </div>
                                    </div>
                                </div>
                            </section>

                            <section className="form-section">
                                <h3>Account Security</h3>
                                <div className="toggle-field">
                                    <input 
                                        type="checkbox" 
                                        id="isSuspended"
                                        checked={selectedProfile.isSuspended === 'Y'}
                                        onChange={(e) => handleFieldChange('isSuspended', e.target.checked ? 'Y' : 'N')}
                                    />
                                    <label htmlFor="isSuspended">Account Suspended (Bridges all approval actions)</label>
                                </div>
                            </section>

                            <div className="tier-info-banner">
                                <h4>Tier Info</h4>
                                <p>Tiers defined by REQ-COM-AUTH-01. Tier 4 requires Dual-Authorization for certain transaction thresholds.</p>
                            </div>
                        </div>
                    </>
                ) : (
                    <div className="empty-selection">Select a user to manage authority</div>
                )}
            </main>

            <style jsx>{`
                .authority-manager-layout {
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

                .pane-header h3, .pane-header h2 {
                    margin: 0;
                    color: #1e293b;
                    font-size: 1.125rem;
                }

                .user-list-pane {
                    border-right: 1px solid #f1f5f9;
                    background: #f8fafc;
                }

                .user-items {
                    padding: 1rem;
                    display: flex;
                    flex-direction: column;
                    gap: 0.5rem;
                }

                .user-item {
                    padding: 1rem;
                    border-radius: 8px;
                    cursor: pointer;
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    transition: all 0.2s;
                    border: 1px solid transparent;
                }

                .user-item:hover {
                    background: #f1f5f9;
                }

                .user-item.active {
                    background: white;
                    border-color: #2563eb;
                    box-shadow: 0 1px 3px rgba(37, 99, 235, 0.1);
                }

                .user-id {
                    font-weight: 600;
                    color: #1e293b;
                }

                .user-tier {
                    font-size: 0.75rem;
                    font-weight: 700;
                    background: #eff6ff;
                    color: #1d4ed8;
                    padding: 0.25rem 0.5rem;
                    border-radius: 4px;
                }

                .authority-detail-pane {
                    background: white;
                    display: flex;
                    flex-direction: column;
                }

                .detail-header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    background: white;
                }

                .config-form {
                    padding: 2rem;
                    display: flex;
                    flex-direction: column;
                    gap: 2.5rem;
                }

                .form-section h3 {
                    margin: 0 0 1.25rem 0;
                    font-size: 0.875rem;
                    text-transform: uppercase;
                    letter-spacing: 0.05em;
                    color: #64748b;
                    border-bottom: 1px solid #f1f5f9;
                    padding-bottom: 0.5rem;
                }

                .field-group {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                    gap: 2rem;
                }

                .field {
                    display: flex;
                    flex-direction: column;
                    gap: 0.5rem;
                }

                .field label {
                    font-size: 0.875rem;
                    font-weight: 600;
                    color: #475569;
                }

                .field select, .field input {
                    padding: 0.625rem;
                    border: 1px solid #e2e8f0;
                    border-radius: 6px;
                    font-size: 0.875rem;
                    outline: none;
                }

                .amount-input-group {
                    display: flex;
                    border: 1px solid #e2e8f0;
                    border-radius: 6px;
                    overflow: hidden;
                }

                .currency-addon {
                    background: #f1f5f9;
                    padding: 0 0.75rem;
                    display: flex;
                    align-items: center;
                    font-size: 0.75rem;
                    font-weight: 700;
                    color: #64748b;
                    border-right: 1px solid #e2e8f0;
                }

                .amount-input-group input {
                    border: none;
                    flex: 1;
                    padding: 0.625rem;
                }

                .toggle-field {
                    display: flex;
                    align-items: center;
                    gap: 1rem;
                    padding: 1rem;
                    background: #fff1f2;
                    border-radius: 8px;
                    border: 1px solid #fecdd3;
                }

                .toggle-field label {
                    font-weight: 600;
                    color: #9f1239;
                    cursor: pointer;
                }

                .tier-info-banner {
                    padding: 1.5rem;
                    background: #f0f9ff;
                    border: 1px solid #bae6fd;
                    border-radius: 8px;
                    margin-top: 2rem;
                }

                .tier-info-banner h4 {
                    margin: 0 0 0.5rem 0;
                    color: #0369a1;
                }

                .tier-info-banner p {
                    margin: 0;
                    font-size: 0.875rem;
                    color: #0c4a6e;
                }

                .primary-btn {
                    background: #2563eb;
                    color: white;
                    border: none;
                    padding: 0.625rem 1.25rem;
                    border-radius: 6px;
                    font-weight: 600;
                    cursor: pointer;
                }
                .admin-loading { padding: 2rem; text-align: center; color: #64748b; }
            `}</style>
        </div>
    );
};
