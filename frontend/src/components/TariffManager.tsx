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
            const feeList = data?.feeList || [];
            setFees(feeList);
            if (feeList.length > 0 && !selectedFee) {
                setSelectedFee(feeList[0]);
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
        
        // Handle numeric conversion and NaN
        let processedValue = value;
        if (field === 'ratePercent' || field === 'flatAmount' || field === 'baseValue' || field === 'minFloorAmount' || field === 'maxCeilingAmount') {
            processedValue = value === '' ? 0 : parseFloat(value);
            if (isNaN(processedValue)) processedValue = 0;
        }

        const update: Partial<FeeConfiguration> = { [field]: processedValue };
        
        // Synchronize baseValue with rate/amount fields
        if (field === 'ratePercent' || field === 'flatAmount') {
            update.baseValue = processedValue;
        } else if (field === 'baseValue') {
            if (selectedFee.calculationTypeEnumId === 'PERCENTAGE') update.ratePercent = processedValue;
            if (selectedFee.calculationTypeEnumId === 'FLAT_RATE') update.flatAmount = processedValue;
        }
        
        const newSelectedFee = {
            ...selectedFee,
            ...update
        };
        setSelectedFee(newSelectedFee);

        // Update main list for immediate reactive feedback
        if (fees) {
            setFees(fees.map(f => f.feeConfigurationId === selectedFee.feeConfigurationId ? newSelectedFee : f));
        }
    };

    const handleSave = async (isPublish: boolean) => {
        if (!selectedFee) return;
        setSaving(true);
        try {
            await tradeApi.updateFeeConfiguration(selectedFee.feeConfigurationId, selectedFee);
            await loadFees();
        } catch (err) {
            setError('Failed to save tariff');
        } finally {
            setSaving(false);
        }
    };

    const formatEnum = (id: string) => (id || '').replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase());

    if (loading && (fees?.length || 0) === 0) return <div className="admin-loading">Loading tariffs...</div>;

    return (
        <div className="tariff-manager-layout">
            <aside className="fee-list-pane">
                <header className="pane-header">
                    <h3>Tariff Matrix</h3>
                </header>
                <div className="fee-items">
                    {(fees || []).map((f, index) => (
                        <div 
                            key={f.feeConfigurationId || `tariff-${index}`} 
                            className={`fee-item ${selectedFee?.feeConfigurationId === f.feeConfigurationId ? 'active' : ''}`}
                            onClick={() => handleSelectFee(f)}
                        >
                            <div className="fee-item-header">
                                <span className="fee-description">{formatEnum(f.feeEventEnumId)}</span>
                                <span className={`status-pill ${f.isActive === 'Y' ? 'active' : 'inactive'}`}>
                                    {f.isActive === 'Y' ? 'Active' : 'Draft'}
                                </span>
                            </div>
                            <div className="fee-item-details">
                                <span className="fee-rate">
                                    {(() => {
                                        const val = f.baseValue ?? f.ratePercent ?? f.flatAmount ?? 0;
                                        return f.calculationTypeEnumId === 'PERCENTAGE' ? `${val}%` : 
                                               f.calculationTypeEnumId === 'FLAT_RATE' ? `${f.currencyUomId || 'USD'} ${val}` : 'Tiered';
                                    })()}
                                </span>
                                <span className="fee-id">{f.feeConfigurationId}</span>
                            </div>
                        </div>
                    ))}
                </div>
            </aside>

            <main className="fee-detail-pane">
                {selectedFee ? (
                    <>
                        <header className="pane-header detail-header">
                            <h2>{formatEnum(selectedFee.feeEventEnumId)} Configuration</h2>
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
                                        <label htmlFor="calculationType">Method</label>
                                        <select 
                                            id="calculationType"
                                            value={selectedFee.calculationTypeEnumId}
                                            onChange={(e) => handleFieldChange('calculationTypeEnumId', e.target.value)}
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
                                            value={selectedFee.ratePercent ?? selectedFee.baseValue ?? ''}
                                            onChange={(e) => handleFieldChange('ratePercent', e.target.value)}
                                            disabled={selectedFee.calculationTypeEnumId === 'FLAT_RATE'}
                                        />
                                    </div>
                                    <div className="field">
                                        <label htmlFor="flatAmount">Flat Amount</label>
                                        <input 
                                            id="flatAmount"
                                            type="number" 
                                            value={selectedFee.flatAmount ?? selectedFee.baseValue ?? ''}
                                            onChange={(e) => handleFieldChange('flatAmount', e.target.value)}
                                            disabled={selectedFee.calculationTypeEnumId === 'PERCENTAGE'}
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
                /* ... styles as seen in original ... */
                .tariff-manager-layout { display: grid; grid-template-columns: 350px 1fr; background: white; border-radius: 12px; border: 1px solid #e2e8f0; overflow: hidden; height: calc(100vh - 180px); min-height: 600px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1); }
                .pane-header { padding: 1.25rem 1.5rem; border-bottom: 1px solid #f1f5f9; background: #f8fafc; flex-shrink: 0; }
                .pane-header h3, .pane-header h2 { margin: 0; color: #1e293b; font-size: 1rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.025em; }
                .fee-list-pane { border-right: 1px solid #f1f5f9; background: #f8fafc; display: flex; flex-direction: column; }
                .fee-items { padding: 1rem; display: flex; flex-direction: column; gap: 0.75rem; overflow-y: auto; flex: 1; }
                .fee-item { padding: 1rem; border-radius: 10px; cursor: pointer; display: flex; flex-direction: column; gap: 0.5rem; transition: all 0.2s; border: 1px solid transparent; background: white; box-shadow: 0 1px 2px rgba(0,0,0,0.05); }
                .fee-item:hover { transform: translateY(-1px); box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1); }
                .fee-item.active { border-color: #2563eb; box-shadow: 0 0 0 1px #2563eb, 0 4px 6px -1px rgba(37, 99, 235, 0.1); background: #f0f7ff; }
                .fee-item-header { display: flex; justify-content: space-between; align-items: flex-start; }
                .fee-description { font-weight: 600; color: #1e293b; font-size: 0.9375rem; }
                .status-pill { font-size: 0.7rem; font-weight: 700; padding: 0.125rem 0.5rem; border-radius: 9999px; text-transform: uppercase; }
                .status-pill.active { background: #dcfce7; color: #15803d; }
                .status-pill.inactive { background: #f1f5f9; color: #475569; }
                .fee-item-details { display: flex; justify-content: space-between; align-items: center; }
                .fee-rate { font-size: 0.875rem; color: #4b5563; font-weight: 500; }
                .fee-id { font-size: 0.75rem; color: #94a3b8; font-family: monospace; }
                .fee-detail-pane { background: white; display: flex; flex-direction: column; overflow: hidden; }
                .detail-header { display: flex; justify-content: space-between; align-items: center; background: white; border-bottom: 1px solid #f1f5f9; }
                .action-bar { display: flex; gap: 0.75rem; }
                .config-form { padding: 2rem; display: flex; flex-direction: column; gap: 2rem; overflow-y: auto; flex: 1; }
                .form-section h3 { margin: 0 0 1.25rem 0; font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.1em; color: #64748b; border-bottom: 1px solid #f1f5f9; padding-bottom: 0.5rem; font-weight: 700; }
                .field-group { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 1.5rem; }
                .field { display: flex; flex-direction: column; gap: 0.375rem; }
                .field label { font-size: 0.75rem; font-weight: 700; color: #475569; text-transform: uppercase; }
                .field input, .field select { padding: 0.625rem; border: 1px solid #e2e8f0; border-radius: 8px; font-size: 0.875rem; outline: none; transition: all 0.2s; background: #fff; }
                .field input:focus, .field select:focus { border-color: #2563eb; box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1); }
                .field input:disabled { background: #f8fafc; cursor: not-allowed; color: #94a3b8; }
                .toggle-field { display: flex; align-items: center; gap: 0.75rem; padding: 1rem; background: #f8fafc; border-radius: 10px; border: 1px solid #e2e8f0; transition: all 0.2s; }
                .toggle-field:hover { background: #f1f5f9; }
                .toggle-field label { font-size: 0.875rem; font-weight: 600; color: #334155; cursor: pointer; }
                .primary-btn { background: #2563eb; color: white; border: none; padding: 0.625rem 1.25rem; border-radius: 8px; font-weight: 600; cursor: pointer; transition: all 0.2s; font-size: 0.875rem; }
                .primary-btn:hover { background: #1d4ed8; box-shadow: 0 4px 6px -1px rgba(37, 99, 235, 0.2); }
                .primary-btn:disabled { background: #94a3b8; cursor: not-allowed; }
                .secondary-btn { background: white; color: #475569; border: 1px solid #e2e8f0; padding: 0.625rem 1.25rem; border-radius: 8px; font-weight: 600; cursor: pointer; transition: all 0.2s; font-size: 0.875rem; }
                .secondary-btn:hover { background: #f8fafc; border-color: #cbd5e1; }
                .admin-loading { padding: 4rem; text-align: center; color: #64748b; font-weight: 500; }
                .empty-selection { padding: 4rem; text-align: center; color: #94a3b8; font-style: italic; }
            `}</style>
        </div>
    );
};
