'use client';

import React, { useState, useEffect } from 'react';
import { tradeApi } from '../api/tradeApi';
import { TradeInstrument, ImportLetterOfCredit } from '../api/types';
import { isValidZChars } from '../utils/SwiftUtils';

// ABOUTME: LC Amendment Stepper implementing REQ-IMP-PRC-02.
// ABOUTME: Allows makers to capture "Delta" changes to active LCs and preview SWIFT MT 707.

const steps = [
    'Instrument Context',
    'Financial Amendment',
    'Terms & Shipping',
    'MT 707 Preview',
    'Review & Submit',
];

interface AmendmentStepperProps {
    lcId: string;
}

export const AmendmentStepper: React.FC<AmendmentStepperProps> = ({ lcId }) => {
    const [stepIndex, setStepIndex] = useState(0);
    const [instrument, setInstrument] = useState<(TradeInstrument & ImportLetterOfCredit) | null>(null);
    const [delta, setDelta] = useState({
        amountAdjustment: '0',
        newExpiryDate: '',
        amendmentNarrative: '',
        amendmentTypeEnumId: 'FINANCIAL',
        beneficiaryConsentRequired: true,
    });

    const [error, setError] = useState('');
    const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
    const [loading, setLoading] = useState(true);
    const [status, setStatus] = useState<'IDLE' | 'SUBMITTED'>('IDLE');

    useEffect(() => {
        setLoading(true);
        tradeApi.getImportLc(lcId).then(data => {
            setInstrument(data);
            setLoading(false);
        });
    }, [lcId]);

    if (loading && !instrument) return <div className="p-8 text-center">Loading LC Context...</div>;
    if (!instrument) return <div className="p-8 text-center text-danger">LC Not Found</div>;

    const originalAmount = instrument.effectiveAmount || instrument.amount || 0;
    const newTotalLiability = originalAmount + (parseFloat(delta.amountAdjustment) || 0);

    const handleNext = () => {
        setError('');
        
        // Validation for Step 1 (Financial)
        if (stepIndex === 1) {
            if (delta.newExpiryDate && instrument.expiryDate) {
                if (new Date(delta.newExpiryDate) < new Date(instrument.expiryDate)) {
                    setError('Expiry date must be an extension of the current expiry date.');
                    return;
                }
            }
        }

        setStepIndex(prev => Math.min(prev + 1, 4));
    }
    const handleBack = () => {
        setError('');
        setStepIndex(prev => Math.max(prev - 1, 0));
    }

    const handleSubmit = async () => {
        setError('');
        setLoading(true);
        try {
            const result = await tradeApi.createLcAmendment(lcId, {
                ...delta,
                amountAdjustment: parseFloat(delta.amountAdjustment) || 0,
                instrumentId: lcId
            });
            if (result.errors || result.error) {
                setError(result.errors?.[0] || result.error || 'Failed to submit amendment');
            } else {
                setStatus('SUBMITTED');
                alert('Amendment submitted successfully');
            }
        } catch (e: any) {
            setError(e.message || 'An unexpected error occurred');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="stepper-container premium-card">
            <header className="stepper-header">
                <div className="context-banner">Amending Instrument: {lcId}</div>
                <div className="liability-banner">New Total Liability: {instrument.currencyUomId || 'USD'} {newTotalLiability.toLocaleString()}</div>
            </header>

            <div className="stepper-progress">
                {steps.map((label, i) => (
                    <div key={label} className={`step-item ${i <= stepIndex ? 'active' : ''}`}>
                        <div className="step-number">{i + 1}</div>
                        <div className="step-label">{label}</div>
                    </div>
                ))}
            </div>

            <main className="stepper-content">
                {error && <div className="error-banner mb-2">{error}</div>}
                {stepIndex === 0 && (
                    <section className="context-section">
                        <h3>Current LC Context</h3>
                        <div className="context-grid">
                            <div className="context-item">
                                <span className="label">Current Amount</span>
                                <span className="value">{instrument.currencyUomId || 'USD'} {originalAmount.toLocaleString()}</span>
                            </div>
                            <div className="context-item">
                                <span className="label">Current Expiry</span>
                                <span className="value">{instrument.expiryDate}</span>
                            </div>
                            <div className="context-item">
                                <span className="label">Beneficiary ID</span>
                                <span className="value">{instrument.beneficiaryPartyId}</span>
                            </div>
                        </div>
                    </section>
                )}

                {stepIndex === 1 && (
                    <section className="form-grid">
                        <div className="field-group full-width">
                            <label htmlFor="amendmentTypeEnumId" className="required-label">Amendment Type</label>
                            <select 
                                id="amendmentTypeEnumId"
                                value={delta.amendmentTypeEnumId}
                                onChange={e => setDelta({...delta, amendmentTypeEnumId: e.target.value})}
                            >
                                <option value="FINANCIAL">Financial (Amount/Expiry)</option>
                                <option value="NARRATIVE">Narrative / Terms Only</option>
                            </select>
                        </div>
                        <div className="field-group">
                            <label htmlFor="amountAdjustment">Amount Adjustment (Delta)</label>
                            <input 
                                id="amountAdjustment"
                                type="number"
                                placeholder="e.g. +50000"
                                value={delta.amountAdjustment}
                                onChange={e => setDelta({...delta, amountAdjustment: e.target.value})}
                            />
                            <p className="helper-text">Current Effective Amount: {instrument.currencyUomId || 'USD'} {originalAmount.toLocaleString()}</p>
                        </div>
                        <div className="field-group">
                            <label htmlFor="newExpiryDate">New Expiry Date</label>
                            <input 
                                id="newExpiryDate"
                                type="date"
                                value={delta.newExpiryDate}
                                onChange={e => setDelta({...delta, newExpiryDate: e.target.value})}
                            />
                            <p className="helper-text">Original: {instrument.expiryDate}</p>
                        </div>
                    </section>
                )}

                {stepIndex === 2 && (
                    <section className="form-grid">
                        <div className="field-group full-width">
                            <label htmlFor="shippingChanges" className="required-label">Shipping & Terms Changes (Tag 77A)</label>
                            <textarea 
                                id="shippingChanges"
                                className={fieldErrors.amendmentNarrative ? 'is-invalid' : ''}
                                rows={5}
                                placeholder="Describe changes to Ports, Latest Shipment Date, etc."
                                value={delta.amendmentNarrative}
                                onChange={e => {
                                    const val = e.target.value;
                                    setDelta({...delta, amendmentNarrative: val});
                                    if (!isValidZChars(val)) {
                                        setFieldErrors(prev => ({...prev, amendmentNarrative: 'Invalid characters detected (Internal SWIFT Z charset required)'}));
                                    } else {
                                        setFieldErrors(prev => {
                                            const updated = { ...prev };
                                            delete updated.amendmentNarrative;
                                            return updated;
                                        });
                                    }
                                }}
                            />
                            {fieldErrors.amendmentNarrative && <p className="error-text text-xs mt-1">{fieldErrors.amendmentNarrative}</p>}
                        </div>
                    </section>
                )}

                {stepIndex === 3 && (
                    <section className="swift-preview">
                        <h3>MT 707 Preview</h3>
                        <div className="swift-block" data-testid="swift-block">
                            <pre>
{`:20: ${lcId.trim()}
:32B: ${instrument.currencyUomId || 'USD'}${delta.amountAdjustment}
:77A: AMENDMENT TO LC
${delta.newExpiryDate ? `:31E: ${delta.newExpiryDate}` : ''}`}
                            </pre>
                        </div>
                    </section>
                )}

                {stepIndex === 4 && (
                    <section className="review-section">
                        <div className="review-card">
                            <h4>Review Amendment</h4>
                            <p>Adjustment: {instrument.currencyUomId || 'USD'} {parseFloat(delta.amountAdjustment).toLocaleString()}</p>
                            <p>New Total: {instrument.currencyUomId || 'USD'} {newTotalLiability.toLocaleString()}</p>
                        </div>
                        <div className="consent-check">
                            <label htmlFor="consentRequired">
                                <input 
                                    type="checkbox" 
                                    id="consentRequired"
                                    checked={delta.beneficiaryConsentRequired}
                                    onChange={e => setDelta({...delta, beneficiaryConsentRequired: e.target.checked})}
                                />
                                Advise Beneficiary Consent Required
                            </label>
                        </div>
                        {delta.beneficiaryConsentRequired && (
                            <div className="status-badge consent-pending mt-4">
                                <span className="pulse-dot"></span>
                                Awaiting Beneficiary Consent (Conditional on Approval)
                            </div>
                        )}
                        {parseFloat(delta.amountAdjustment) > 500000 && (
                            <div className="warning-banner mt-4">
                                <strong>⚠️ TIER 4 AUTHORIZATION REQUIRED</strong>
                                <p>Amendment increases liability by {instrument.currencyUomId} {parseFloat(delta.amountAdjustment).toLocaleString()}, triggering Tier 4 compliance threshold.</p>
                            </div>
                        )}
                    </section>
                )}
            </main>

            <footer className="stepper-footer">
                <div className="left-actions">
                    <button className="secondary-btn">Cancel</button>
                </div>
                <div className="right-actions">
                    {stepIndex > 0 && <button data-testid="back-button" onClick={handleBack}>Back</button>}
                    {stepIndex < 4 ? (
                        <button data-testid="next-button" className="primary-btn" onClick={handleNext}>Next</button>
                    ) : (
                        <button 
                            data-testid="submit-button" 
                            className="primary-btn" 
                            onClick={handleSubmit}
                            disabled={loading || status === 'SUBMITTED' || Object.keys(fieldErrors).length > 0}
                        >
                            {loading ? 'Submitting...' : 'Submit Amendment'}
                        </button>
                    )}
                </div>
            </footer>

            <style jsx>{`
                .stepper-container { padding: 2rem; background: white; border-radius: 12px; }
                .stepper-header { display: flex; justify-content: space-between; margin-bottom: 2rem; padding: 1rem; background: #f8fafc; border-radius: 8px; font-weight: 700; color: #1e293b; }
                .stepper-progress { display: flex; justify-content: space-between; margin-bottom: 3rem; }
                .step-item { flex: 1; display: flex; flex-direction: column; align-items: center; opacity: 0.3; }
                .step-item.active { opacity: 1; }
                .step-number { width: 28px; height: 28px; border-radius: 50%; background: #2563eb; color: white; display: flex; align-items: center; justify-content: center; margin-bottom: 0.5rem; font-size: 0.75rem; }
                .step-label { font-size: 0.75rem; font-weight: 600; }
                
                .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 2rem; }
                .field-group { display: flex; flex-direction: column; gap: 0.5rem; }
                .field-group label { font-weight: 600; font-size: 0.875rem; color: #334155; }
                .field-group input { padding: 0.75rem; border: 1px solid #e2e8f0; border-radius: 6px; }
                .helper-text { font-size: 0.75rem; color: #64748b; margin: 0; }
                
                .context-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem; margin-top: 1rem; }
                .context-item { display: flex; flex-direction: column; gap: 0.25rem; }
                .context-item .label { font-size: 0.75rem; color: #64748b; font-weight: 500; }
                .context-item .value { font-weight: 700; color: #0f172a; }

                .swift-block { background: #0f172a; color: #10b981; padding: 1.5rem; border-radius: 8px; font-family: monospace; font-size: 0.875rem; margin-top: 1rem; }
                
                .is-invalid { border-color: #ef4444 !important; }
                .error-text { color: #ef4444; font-weight: 500; }
                
                .error-banner { padding: 1rem; background: #fef2f2; color: #991b1b; border: 1px solid #fecaca; border-radius: 6px; font-size: 0.875rem; font-weight: 600; margin-bottom: 1.5rem; }
                .mb-2 { margin-bottom: 1.5rem; }

                .stepper-footer { display: flex; justify-content: space-between; margin-top: 3rem; border-top: 1px solid #f1f5f9; padding-top: 2rem; }
                .primary-btn { background: #2563eb; color: white; border: none; padding: 0.75rem 1.5rem; border-radius: 6px; font-weight: 600; cursor: pointer; }
                .primary-btn:disabled { opacity: 0.5; cursor: not-allowed; }
                .secondary-btn { background: #f8fafc; color: #475569; border: 1px solid #e2e8f0; padding: 0.75rem 1.5rem; border-radius: 6px; font-weight: 600; cursor: pointer; }
                
                .consent-pending { display: flex; align-items: center; gap: 0.75rem; background: #fffbeb; color: #92400e; border: 1px solid #fef3c7; padding: 0.75rem 1rem; border-radius: 6px; font-size: 0.8125rem; font-weight: 700; width: fit-content; }
                .pulse-dot { width: 8px; height: 8px; background: #d97706; border-radius: 50%; display: inline-block; animation: pulse 2s infinite; }
                @keyframes pulse { 0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(217, 119, 6, 0.7); } 70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(217, 119, 6, 0); } 100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(217, 119, 6, 0); } }
                
                .warning-banner { background: #fee2e2; color: #991b1b; border: 1px solid #fecaca; padding: 1rem; border-radius: 6px; font-size: 0.875rem; }
                .warning-banner p { margin: 0.25rem 0 0 0; font-size: 0.8125rem; opacity: 0.9; }
                .mt-4 { margin-top: 1rem; }
            `}</style>
        </div>
    );
};
