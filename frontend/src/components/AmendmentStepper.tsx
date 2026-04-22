'use client';

import React, { useState } from 'react';

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
    const [delta, setDelta] = useState({
        amountAdjustment: '0',
        newExpiryDate: '',
        amendmentNarrative: '',
        beneficiaryConsentRequired: true,
    });

    // Mock original LC context
    const originalLc = {
        id: lcId,
        amount: 500000,
        currency: 'USD',
        expiryDate: '2026-12-31',
        beneficiary: 'Global Export Ltd',
    };

    const newTotalLiability = originalLc.amount + (parseFloat(delta.amountAdjustment) || 0);

    const handleNext = () => setStepIndex(prev => Math.min(prev + 1, 4));
    const handleBack = () => setStepIndex(prev => Math.max(prev - 1, 0));

    return (
        <div className="stepper-container premium-card">
            <header className="stepper-header">
                <div className="context-banner">Amending Instrument: {originalLc.id}</div>
                <div className="liability-banner">New Total Liability: $ {newTotalLiability.toLocaleString()}</div>
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
                {stepIndex === 0 && (
                    <section className="context-section">
                        <h3>Current LC Context</h3>
                        <div className="context-grid">
                            <div className="context-item">
                                <span className="label">Original Amount</span>
                                <span className="value">{originalLc.currency} {originalLc.amount.toLocaleString()}</span>
                            </div>
                            <div className="context-item">
                                <span className="label">Original Expiry</span>
                                <span className="value">{originalLc.expiryDate}</span>
                            </div>
                            <div className="context-item">
                                <span className="label">Beneficiary</span>
                                <span className="value">{originalLc.beneficiary}</span>
                            </div>
                        </div>
                    </section>
                )}

                {stepIndex === 1 && (
                    <section className="form-grid">
                        <div className="field-group">
                            <label htmlFor="amountAdjustment">Amount Adjustment (Delta)</label>
                            <input 
                                id="amountAdjustment"
                                type="number"
                                placeholder="e.g. +50000"
                                value={delta.amountAdjustment}
                                onChange={e => setDelta({...delta, amountAdjustment: e.target.value})}
                            />
                            <p className="helper-text">Positive for increase, negative for decrease.</p>
                        </div>
                        <div className="field-group">
                            <label htmlFor="newExpiryDate">New Expiry Date</label>
                            <input 
                                id="newExpiryDate"
                                type="date"
                                value={delta.newExpiryDate}
                                onChange={e => setDelta({...delta, newExpiryDate: e.target.value})}
                            />
                        </div>
                    </section>
                )}

                {stepIndex === 2 && (
                    <section className="form-grid">
                        <div className="field-group full-width">
                            <label htmlFor="shippingChanges">Shipping & Terms Changes</label>
                            <textarea 
                                id="shippingChanges"
                                rows={5}
                                placeholder="Describe changes to Ports, Latest Shipment Date, etc."
                                value={delta.amendmentNarrative}
                                onChange={e => setDelta({...delta, amendmentNarrative: e.target.value})}
                            />
                        </div>
                    </section>
                )}

                {stepIndex === 3 && (
                    <section className="swift-preview">
                        <h3>MT 707 Preview</h3>
                        <div className="swift-block" data-testid="swift-block">
                            <pre>
{`:20: ${originalLc.id.trim()}
:32B: ${originalLc.currency}${Math.abs(parseFloat(delta.amountAdjustment))}
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
                            <p>Adjustment: $ {parseFloat(delta.amountAdjustment).toLocaleString()}</p>
                            <p>New Total: $ {newTotalLiability.toLocaleString()}</p>
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
                        <button data-testid="submit-button" className="primary-btn">Submit Amendment</button>
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
                
                .context-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem; margin-top: 1rem; }
                .context-item { display: flex; flex-direction: column; gap: 0.25rem; }
                .context-item .label { font-size: 0.75rem; color: #64748b; font-weight: 500; }
                .context-item .value { font-weight: 700; color: #0f172a; }

                .swift-block { background: #0f172a; color: #10b981; padding: 1.5rem; border-radius: 8px; font-family: monospace; font-size: 0.875rem; margin-top: 1rem; }
                
                .stepper-footer { display: flex; justify-content: space-between; margin-top: 3rem; border-top: 1px solid #f1f5f9; padding-top: 2rem; }
                .primary-btn { background: #2563eb; color: white; border: none; padding: 0.75rem 1.5rem; border-radius: 6px; font-weight: 600; cursor: pointer; }
                .secondary-btn { background: #f8fafc; color: #475569; border: 1px solid #e2e8f0; padding: 0.75rem 1.5rem; border-radius: 6px; font-weight: 600; cursor: pointer; }
            `}</style>
        </div>
    );
};
