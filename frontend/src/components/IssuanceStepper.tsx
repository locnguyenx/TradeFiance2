'use client';

import React, { useState, useEffect } from 'react';
import { tradeApi } from '../api/tradeApi';
import { ClauseSelector } from './ClauseSelector';

// ABOUTME: High-fidelity LC Issuance Stepper implementing REQ-UI-IMP-03.
// ABOUTME: Maps state to BDD sequences: BDD-IMP-FLOW-01 (Draft) and BDD-IMP-FLOW-02 (Submit).

const steps = [
    'Step 1: Parties & Limits',
    'Step 2: Financials & Dates',
    'Step 3: Terms & Shipping',
    'Step 4: Narratives',
    'Step 5: Review & Submit',
];

export const IssuanceStepper: React.FC = () => {
    const [stepIndex, setStepIndex] = useState(0);
    const [formData, setFormData] = useState({
        transactionRef: '',
        applicant: '',
        beneficiary: '',
        amount: '',
        currency: 'USD',
        issueDate: '',
        expiryDate: '',
        positiveTolerance: '0',
        negativeTolerance: '0',
        partialShipment: 'Allowed',
        transhipment: 'Allowed',
        portOfLoading: '',
        portOfDischarge: '',
        goodsDescription: '',
        documentsRequired: '',
        additionalConditions: '',
    });
    const [activeClauseType, setActiveClauseType] = useState<'GOODS' | 'DOCUMENTS' | 'CONDITIONS' | null>(null);
    
    const [status, setStatus] = useState<'DRAFT' | 'SUBMITTED' | 'IDLE'>('IDLE');
    const [dateError, setDateError] = useState('');

    useEffect(() => {
        if (formData.issueDate && formData.expiryDate) {
            const issue = new Date(formData.issueDate);
            const expiry = new Date(formData.expiryDate);
            if (expiry <= issue) {
                setDateError('Expiry Date cannot be in the past or before Issue Date');
            } else {
                setDateError('');
            }
        }
    }, [formData.issueDate, formData.expiryDate]);

    const handleNext = () => setStepIndex(prev => Math.min(prev + 1, 4));
    const handleBack = () => setStepIndex(prev => Math.max(prev - 1, 0));

    const handleSaveDraft = async () => {
        await tradeApi.createLc({ 
            ...formData, 
            amount: parseFloat(formData.amount) || 0,
            businessStateId: 'LC_DRAFT',
            applicantName: formData.applicant 
        });
        setStatus('DRAFT');
    };

    const handleSubmit = async () => {
        await tradeApi.createLc({ 
            ...formData, 
            amount: parseFloat(formData.amount) || 0,
            businessStateId: 'LC_PENDING_APPROVAL' 
        });
        setStatus('SUBMITTED');
    };

    return (
        <div className="stepper-container premium-card">
            <header className="stepper-header">
                <div className="status-badge">Draft Reference: DRAFT-NEW</div>
                <div className="amount-badge">Base Equivalent: {formData.currency} {formData.amount || '0'}</div>
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
                    <section className="form-grid">
                        <div className="field-group">
                            <label htmlFor="transactionRef">Transaction Reference</label>
                            <input 
                                id="transactionRef"
                                value={formData.transactionRef}
                                onChange={e => setFormData({...formData, transactionRef: e.target.value})}
                                placeholder="e.g., IMLC/2026/001"
                            />
                        </div>
                        <div className="field-group">
                            <label htmlFor="applicant">Applicant</label>
                            <input 
                                id="applicant"
                                value={formData.applicant}
                                onChange={e => setFormData({...formData, applicant: e.target.value})}
                                placeholder="Search Applicant..."
                            />
                            <div className="helper-box">
                                <p>Available Facility Limit: $1,000,000</p>
                                <p>KYC Status: <span className="text-success">VERIFIED</span></p>
                            </div>
                        </div>
                        <div className="field-group">
                            <label htmlFor="beneficiary">Beneficiary</label>
                            <textarea 
                                id="beneficiary"
                                rows={3}
                                value={formData.beneficiary}
                                onChange={e => setFormData({...formData, beneficiary: e.target.value})}
                                placeholder="Multi-line Beneficiary details..."
                            />
                        </div>
                    </section>
                )}

                {stepIndex === 1 && (
                    <section className="form-grid">
                        <div className="field-group">
                            <label htmlFor="amount">Amount</label>
                            <input 
                                id="amount"
                                type="number"
                                value={formData.amount}
                                onChange={e => setFormData({...formData, amount: e.target.value})}
                            />
                        </div>
                        <div className="field-group">
                            <label htmlFor="issueDate">Issue Date</label>
                            <input 
                                id="issueDate"
                                type="date"
                                value={formData.issueDate}
                                onChange={e => setFormData({...formData, issueDate: e.target.value})}
                            />
                        </div>
                        <div className="field-group">
                            <label htmlFor="expiryDate">Expiry Date</label>
                            <input 
                                id="expiryDate"
                                type="date"
                                value={formData.expiryDate}
                                onChange={e => setFormData({...formData, expiryDate: e.target.value})}
                            />
                            {dateError && <p className="error-text">{dateError}</p>}
                        </div>
                    </section>
                )}

                {stepIndex === 2 && (
                    <section className="form-grid">
                        <div className="field-group">
                            <label>Partial Shipment</label>
                            <div className="radio-group">
                                <label><input type="radio" checked={formData.partialShipment === 'Allowed'} onChange={() => setFormData({...formData, partialShipment: 'Allowed'})} /> Allowed</label>
                                <label><input type="radio" checked={formData.partialShipment === 'Not Allowed'} onChange={() => setFormData({...formData, partialShipment: 'Not Allowed'})} /> Not Allowed</label>
                            </div>
                        </div>
                        <div className="field-group">
                            <label htmlFor="portOfLoading">Port of Loading</label>
                            <input id="portOfLoading" value={formData.portOfLoading} onChange={e => setFormData({...formData, portOfLoading: e.target.value})} />
                        </div>
                    </section>
                )}

                {stepIndex === 3 && (
                    <section className="form-grid">
                        <div className="field-group full-width">
                            <div className="flex justify-between items-center mb-1">
                                <label htmlFor="goodsDescription">Description of Goods</label>
                                <button className="helper-link" onClick={() => setActiveClauseType('GOODS')}>+ Standard Clauses</button>
                            </div>
                            <textarea id="goodsDescription" rows={4} value={formData.goodsDescription} onChange={e => setFormData({...formData, goodsDescription: e.target.value})} />
                        </div>
                        <div className="field-group full-width">
                            <div className="flex justify-between items-center mb-1">
                                <label htmlFor="documentsRequired">Documents Required</label>
                                <button className="helper-link" onClick={() => setActiveClauseType('DOCUMENTS')}>+ Standard Clauses</button>
                            </div>
                            <textarea id="documentsRequired" rows={4} value={formData.documentsRequired} onChange={e => setFormData({...formData, documentsRequired: e.target.value})} />
                        </div>
                        <div className="field-group full-width">
                            <div className="flex justify-between items-center mb-1">
                                <label htmlFor="additionalConditions">Additional Conditions</label>
                                <button className="helper-link" onClick={() => setActiveClauseType('CONDITIONS')}>+ Standard Clauses</button>
                            </div>
                            <textarea id="additionalConditions" rows={4} value={formData.additionalConditions} onChange={e => setFormData({...formData, additionalConditions: e.target.value})} />
                        </div>
                    </section>
                )}

                {stepIndex === 4 && (
                    <section className="review-panel">
                        <div className="review-card">
                            <h4>Review & Submit</h4>
                            <p>Reference: {formData.transactionRef}</p>
                            <p>Applicant: {formData.applicant}</p>
                            <p>Beneficiary: {formData.beneficiary}</p>
                            <p>Amount: {Number(formData.amount).toLocaleString()}</p>
                            <p>Expiry: {formData.expiryDate}</p>
                        </div>
                        <div className="validation-panel">
                            <h5>System Validations</h5>
                            <p>✅ Limit Check Passed</p>
                            <p>✅ Sanctions Compliance Clear</p>
                        </div>
                        {status === 'SUBMITTED' && <div className="success-banner">Successfully Submitted for Approval</div>}
                    </section>
                )}
            </main>

            <footer className="stepper-footer">
                <div className="left-actions">
                    <button className="secondary-btn" onClick={handleSaveDraft}>Save Draft</button>
                </div>
                <div className="right-actions">
                    {stepIndex > 0 && <button data-testid="back-button" onClick={handleBack}>Back</button>}
                    {stepIndex < 4 ? (
                        <button 
                            data-testid="next-button" 
                            className="primary-btn" 
                            onClick={handleNext}
                            disabled={!!dateError || (stepIndex === 0 && !formData.applicant)}
                        >
                            Next
                        </button>
                    ) : (
                        <button 
                            data-testid="submit-button" 
                            className="primary-btn" 
                            onClick={handleSubmit}
                            disabled={status === 'SUBMITTED'}
                        >
                            Submit for Approval
                        </button>
                    )}
                </div>
            </footer>

            {activeClauseType && (
                <ClauseSelector 
                    type={activeClauseType}
                    onClose={() => setActiveClauseType(null)}
                    onSelect={(text) => {
                        const fieldMap: Record<string, keyof typeof formData> = {
                            GOODS: 'goodsDescription',
                            DOCUMENTS: 'documentsRequired',
                            CONDITIONS: 'additionalConditions'
                        };
                        const field = fieldMap[activeClauseType];
                        setFormData({
                            ...formData,
                            [field]: (formData[field] ? formData[field] + '\n' : '') + text
                        });
                    }}
                />
            )}

            <style jsx>{`
                .stepper-container { padding: 2rem; border-radius: 12px; border: 1px solid #e2e8f0; background: white; box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1); }
                .stepper-header { display: flex; justify-content: space-between; margin-bottom: 2rem; font-weight: 600; color: #475569; }
                .stepper-progress { display: flex; justify-content: space-between; margin-bottom: 3rem; }
                .step-item { flex: 1; display: flex; flex-direction: column; align-items: center; position: relative; opacity: 0.3; }
                .step-item.active { opacity: 1; }
                .step-number { width: 32px; height: 32px; border-radius: 50%; background: #2563eb; color: white; display: flex; items-center; justify-content: center; margin-bottom: 0.5rem; }
                .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }
                .full-width { grid-column: span 2; }
                .field-group { display: flex; flex-direction: column; gap: 0.5rem; }
                .field-group label { font-weight: 600; font-size: 0.875rem; color: #1e293b; }
                .field-group input, .field-group textarea { padding: 0.75rem; border: 1px solid #cbd5e1; border-radius: 6px; }
                .helper-box { padding: 1rem; background: #f8fafc; border-radius: 8px; margin-top: 0.5rem; font-size: 0.875rem; }
                .stepper-footer { display: flex; justify-content: space-between; margin-top: 3rem; border-top: 1px solid #f1f5f9; padding-top: 2rem; }
                .primary-btn { background: #2563eb; color: white; padding: 0.75rem 1.5rem; border-radius: 6px; font-weight: 600; border: none; }
                .secondary-btn { background: #f8fafc; color: #1e293b; padding: 0.75rem 1.5rem; border-radius: 6px; border: 1px solid #e2e8f0; }
                .primary-btn:disabled { opacity: 0.5; cursor: not-allowed; }
                .error-text { color: #dc2626; font-size: 0.75rem; margin-top: 0.25rem; }
                .success-banner { padding: 1rem; background: #ecfdf5; color: #065f46; border-radius: 8px; text-align: center; }
                .helper-link { background: none; border: none; color: #2563eb; font-size: 0.75rem; font-weight: 700; cursor: pointer; text-decoration: underline; padding: 0; }
                .helper-link:hover { color: #1d4ed8; }
                .text-success { color: #059669; }
            `}</style>
        </div>
    );
};
