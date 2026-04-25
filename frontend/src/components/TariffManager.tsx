'use client';

'use client';

import React, { useState, useEffect } from 'react';
import { tradeApi } from '../api/tradeApi';
import { FeeConfiguration } from '../api/types';

// ABOUTME: TariffManager implements REQ-UI-CMN-05.
// ABOUTME: Matrix / Rules Grid for managing trade finance fee structures.

export const TariffManager: React.FC = () => {
    const [fees, setFees] = useState<FeeConfiguration[]>([]);
    const [selectedFee, setSelectedFee] = useState<FeeConfiguration | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        loadFees();
    }, []);

    const loadFees = async () => {
        try {
            const data = await tradeApi.getFeeConfigurations();
            setFees(data.feeList);
            if (data.feeList.length > 0 && !selectedFee) {
                setSelectedFee(data.feeList[0]);
            }
        } catch (err) {
            setError('Failed to load fee configurations');
        } finally {
            setLoading(false);
        }
    };

    const handleSelectFee = (fee: FeeConfiguration) => {
        setSelectedFee({ ...fee });
    };

    const handleFieldChange = (field: keyof FeeConfiguration, value: any) => {
        if (!selectedFee) return;
        setSelectedFee({
            ...selectedFee,
            [field]: value
        });
    };

    const handleSave = async (isPublish: boolean) => {
        if (!selectedFee) return;
        setSaving(true);
        try {
            await tradeApi.updateFeeConfiguration(selectedFee.feeConfigId, selectedFee);
            await loadFees();
        } catch (err) {
            setError('Failed to save tariff');
        } finally {
            setSaving(false);
        }
    };

    const formatEnum = (id: string) => id.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase());

    if (loading) return <div className="admin-loading">Loading tariffs...</div>;

    return (
        <div className="tariff-manager-layout">
            <aside className="fee-list-pane">
                <header className="pane-header">
                    <h3>Tariff & Fee Configuration</h3>
                </header>
                <div className="fee-items">
                    {fees.map(f => (
                        <div 
                            key={f.feeConfigId} 
                            className={`fee-item ${selectedFee?.feeConfigId === f.feeConfigId ? 'active' : ''}`}
                            onClick={() => handleSelectFee(f)}
                        >
                            <span className="fee-type">{f.feeTypeEnumId}</span>
                            <span className="fee-description">{formatEnum(f.feeTypeEnumId)}</span>
                            <span className="fee-id">{f.feeConfigId}</span>
                        </div>
                    ))}
                </div>
            </aside>

            <main className="fee-detail-pane">
                {selectedFee ? (
                    <>
                        <header className="pane-header detail-header">
                            <h2>{formatEnum(selectedFee.feeTypeEnumId)} Configuration</h2>
                            <div className="action-bar">
                                <button 
                                    className="secondary-btn" 
                                    onClick={() => handleSave(false)}
                                    disabled={saving}
                                >
                                    {saving ? 'Saving...' : 'Save Draft'}
                                </button>
                                <button 
                                    className="primary-btn" 
                                    onClick={() => handleSave(true)}
                                    disabled={saving}
                                >
                                    Publish New Tariff
                                </button>
                            </div>
                        </header>

                        <div className="config-form">
                            <section className="form-section">
                                <h3>Calculation Method</h3>
                                <div className="field-group">
                                    <div className="field">
                                        <label htmlFor="calculationMethod">Method</label>
                                        <select 
                                            id="calculationMethod"
                                            value={selectedFee.calculationMethodEnumId}
                                            onChange={(e) => handleFieldChange('calculationMethodEnumId', e.target.value)}
                                        >
                                            <option value="PERCENTAGE">Percentage</option>
                                            <option value="FLAT_RATE">Flat Rate</option>
                                            <option value="TIERED">Tiered</option>
                                        </select>
                                    </div>
                                    <div className="field">
                                        <label htmlFor="currencyUomId">Currency</label>
                                        <input 
                                            id="currencyUomId"
                                            type="text" 
                                            value={selectedFee.currencyUomId || ''}
                                            onChange={(e) => handleFieldChange('currencyUomId', e.target.value)}
                                            placeholder="USD"
                                        />
                                    </div>
                                </div>
                            </section>

                            <section className="form-section">
                                <h3>Base Rules</h3>
                                <div className="field-group">
                                    <div className="field">
                                        <label htmlFor="ratePercent">Rate %</label>
                                        <input 
                                            id="ratePercent"
                                            type="number" 
                                            step="0.001"
                                            value={selectedFee.ratePercent || 0}
                                            onChange={(e) => handleFieldChange('ratePercent', parseFloat(e.target.value))}
                                            disabled={selectedFee.calculationMethodEnumId === 'FLAT_RATE'}
                                        />
                                    </div>
                                    <div className="field">
                                        <label htmlFor="flatAmount">Flat Amount</label>
                                        <input 
                                            id="flatAmount"
                                            type="number" 
                                            value={selectedFee.flatAmount || 0}
                                            onChange={(e) => handleFieldChange('flatAmount', parseFloat(e.target.value))}
                                            disabled={selectedFee.calculationMethodEnumId === 'PERCENTAGE'}
                                        />
                                    </div>
                                    <div className="field">
                                        <label htmlFor="minFloorAmount">Minimum Charge</label>
                                        <input 
                                            id="minFloorAmount"
                                            type="number" 
                                            value={selectedFee.minFloorAmount || 0}
                                            onChange={(e) => handleFieldChange('minFloorAmount', parseFloat(e.target.value))}
                                        />
                                    </div>
                                    <div className="field">
                                        <label htmlFor="maxCeilingAmount">Maximum Charge</label>
                                        <input 
                                            id="maxCeilingAmount"
                                            type="number" 
                                            value={selectedFee.maxCeilingAmount || 0}
                                            onChange={(e) => handleFieldChange('maxCeilingAmount', parseFloat(e.target.value))}
                                        />
                                    </div>
                                </div>
                            </section>

                            <section className="form-section">
                                <h3>Status</h3>
                                <div className="toggle-field">
                                    <input 
                                        type="checkbox" 
                                        id="isActive"
                                        checked={selectedFee.isActive === 'Y'}
                                        onChange={(e) => handleFieldChange('isActive', e.target.checked ? 'Y' : 'N')}
                                    />
                                    <label htmlFor="isActive">Active (Enforced for all new transactions)</label>
                                </div>
                            </section>
                        </div>
                    </>
                ) : (
                    <div className="empty-selection">Select a fee type to configure</div>
                )}
            </main>

            <style jsx>{`
                .tariff-manager-layout {
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

                .fee-list-pane {
                    border-right: 1px solid #f1f5f9;
                    background: #f8fafc;
                }

                .fee-items {
                    padding: 1rem;
                    display: flex;
                    flex-direction: column;
                    gap: 0.5rem;
                }

                .fee-item {
                    padding: 1rem;
                    border-radius: 8px;
                    cursor: pointer;
                    display: flex;
                    flex-direction: column;
                    gap: 0.25rem;
                    transition: all 0.2s;
                    border: 1px solid transparent;
                }

                .fee-item:hover {
                    background: #f1f5f9;
                }

                .fee-item.active {
                    background: white;
                    border-color: #2563eb;
                    box-shadow: 0 1px 3px rgba(37, 99, 235, 0.1);
                }

                .fee-type {
                    font-weight: 600;
                    color: #1e293b;
                }

                .fee-id {
                    font-size: 0.75rem;
                    color: #64748b;
                }

                .fee-detail-pane {
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

                .action-bar {
                    display: flex;
                    gap: 1rem;
                }

                .config-form {
                    padding: 2rem;
                    display: flex;
                    flex-direction: column;
                    gap: 2.5rem;
                    overflow-y: auto;
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

                .field input, .field select {
                    padding: 0.625rem;
                    border: 1px solid #e2e8f0;
                    border-radius: 6px;
                    font-size: 0.875rem;
                    outline: none;
                    transition: border-color 0.2s;
                }

                .field input:focus, .field select:focus {
                    border-color: #2563eb;
                }

                .field input:disabled {
                    background: #f1f5f9;
                    cursor: not-allowed;
                }

                .toggle-field {
                    display: flex;
                    align-items: center;
                    gap: 1rem;
                    padding: 1rem;
                    background: #f8fafc;
                    border-radius: 8px;
                    border: 1px solid #f1f5f9;
                }

                .toggle-field label {
                    font-weight: 500;
                    color: #334155;
                    cursor: pointer;
                }

                .primary-btn {
                    background: #2563eb;
                    color: white;
                    border: none;
                    padding: 0.625rem 1.25rem;
                    border-radius: 6px;
                    font-weight: 600;
                    cursor: pointer;
                    transition: background 0.2s;
                }

                .primary-btn:hover { background: #1d4ed8; }
                .secondary-btn {
                    background: white;
                    color: #475569;
                    border: 1px solid #e2e8f0;
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
