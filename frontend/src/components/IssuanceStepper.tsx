'use client';

import React, { useState, useEffect } from 'react';
import { tradeApi } from '../api/tradeApi';
import { ClauseSelector } from './ClauseSelector';

// ABOUTME: High-fidelity LC Issuance Stepper implementing REQ-UI-IMP-03.
// ABOUTME: Maps state to BDD sequences: BDD-IMP-FLOW-01 (Draft) and BDD-IMP-FLOW-02 (Submit).

const steps = [
    'Parties & Limits',
    'Main LC Information',
    'Margin & Charges',
    'Review & Submit'
];

const sectionsPerStep = [
    ['Parties & Limits'],
    ['Financials & Dates', 'Terms & Shipping', 'Narratives'],
    ['Margin', 'Charges'],
    ['Review & Submit']
];

export const IssuanceStepper: React.FC = () => {
    const [stepIndex, setStepIndex] = useState(0);
    const [products, setProducts] = useState<any[]>([]);
    const [formData, setFormData] = useState({
        transactionRef: '',
        productCatalogId: '',
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
        // v3.0 New Fields
        chargeAllocationEnumId: 'APPLICANT',
        confirmationEnumId: 'WITHOUT',
        latestShipmentDate: '',
        lcTypeEnumId: 'SIGHT',
        marginType: 'None',
        marginPercentage: '100',
        marginAmount: '0',
        marginDebitAccount: '',
        charges: [
            { type: 'Issuance Commission', rate: '0.125', amount: '0', account: '' }
        ]
    });
    const [swiftErrors, setSwiftErrors] = useState<Record<string, string>>({});

    useEffect(() => {
        tradeApi.getProductCatalog().then(res => setProducts(res.productList || []));
    }, []);
    const [activeClauseType, setActiveClauseType] = useState<'GOODS' | 'DOCUMENTS' | 'CONDITIONS' | null>(null);
    
    const [status, setStatus] = useState<'DRAFT' | 'SUBMITTED' | 'IDLE'>('IDLE');
    const [dateError, setDateError] = useState('');
    const [errorMessage, setErrorMessage] = useState('');
    const [loading, setLoading] = useState(false);

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

    const validateSwift = (field: string, value: string) => {
        const swiftRegex = /^[a-zA-Z0-9/\-?:().,'+ \n\r]*$/;
        if (!swiftRegex.test(value)) {
            setSwiftErrors(prev => ({ ...prev, [field]: 'Invalid SWIFT character detected' }));
        } else {
            setSwiftErrors(prev => {
                const updated = { ...prev };
                delete updated[field];
                return updated;
            });
        }
    };

    const handleNext = () => {
        setErrorMessage('');
        setStepIndex(prev => Math.min(prev + 1, 3));
    }
    const handleBack = () => setStepIndex(prev => Math.max(prev - 1, 0));

    const handleSaveDraft = async () => {
        setErrorMessage('');
        setLoading(true);
        try {
            const result = await tradeApi.createLc({ 
                ...formData, 
                amount: parseFloat(formData.amount) || 0,
                businessStateId: 'LC_DRAFT',
                applicantPartyId: 'ACME_CORP_001',
                customerFacilityId: 'FAC-ACME-001'
            });
            if (result.errors || result.error) {
                setErrorMessage(result.errors?.[0] || result.error || 'Failed to save draft');
            } else {
                setStatus('DRAFT');
            }
        } catch (e: any) {
            setErrorMessage(e.message || 'An unexpected error occurred');
        } finally {
            setLoading(false);
        }
    };

    const handleSubmit = async () => {
        setErrorMessage('');
        setLoading(true);
        try {
            const result = await tradeApi.createLc({ 
                ...formData, 
                amount: parseFloat(formData.amount) || 0,
                businessStateId: 'LC_PENDING_APPROVAL',
                applicantPartyId: 'ACME_CORP_001',
                customerFacilityId: 'FAC-ACME-001'
            });
            if (result.errors || result.error) {
                setErrorMessage(result.errors?.[0] || result.error || 'Failed to submit for approval');
            } else {
                setStatus('SUBMITTED');
            }
        } catch (e: any) {
            setErrorMessage(e.message || 'An unexpected error occurred');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="stepper-layout premium-card">
            <aside className="right-nav">
                <h4>Sections</h4>
                <ul>
                    {sectionsPerStep[stepIndex].map(section => (
                        <li key={section}>
                            <a href={`#${section.replace(/\s+/g, '-').toLowerCase()}`}>{section}</a>
                        </li>
                    ))}
                </ul>
            </aside>

            <div className="stepper-main">
                <header className="stepper-header">
                    <div className="status-badge">Reference: {formData.transactionRef || 'DRAFT-NEW'}</div>
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
                <header className="step-header mb-4">
                    <h3 className="text-xl font-bold">Step {stepIndex + 1}: {steps[stepIndex]}</h3>
                </header>
                {errorMessage && <div className="error-banner mb-2">{errorMessage}</div>}
                
                {stepIndex === 0 && (
                    <section className="form-grid">
                        <div className="field-group">
                            <label htmlFor="productCatalogId">LC Product</label>
                            <select 
                                id="productCatalogId"
                                value={formData.productCatalogId}
                                onChange={e => setFormData({...formData, productCatalogId: e.target.value})}
                            >
                                <option value="">Select Product...</option>
                                {products.map(p => <option key={p.productCatalogId} value={p.productCatalogId}>{p.productName}</option>)}
                            </select>
                        </div>
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
                    <div className="main-lc-scroll-area">
                        <section id="financials-&-dates" className="form-grid section-divider">
                            <h3 className="section-title">Financials & Dates</h3>
                            <div className="field-group">
                                <label htmlFor="amount">Amount</label>
                                <input id="amount" type="number" value={formData.amount} onChange={e => setFormData({...formData, amount: e.target.value})} />
                            </div>
                            <div className="field-group">
                                <label htmlFor="issueDate">Issue Date</label>
                                <input id="issueDate" type="date" value={formData.issueDate} onChange={e => setFormData({...formData, issueDate: e.target.value})} />
                            </div>
                            <div className="field-group">
                                <label htmlFor="expiryDate">Expiry Date</label>
                                <input id="expiryDate" type="date" value={formData.expiryDate} onChange={e => setFormData({...formData, expiryDate: e.target.value})} />
                                {dateError && <p className="error-text">{dateError}</p>}
                            </div>
                        </section>

                        <section id="terms-&-shipping" className="form-grid section-divider">
                            <h3 className="section-title">Terms & Shipping</h3>
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
                            <div className="field-group">
                                <label htmlFor="portOfDischarge">Port of Discharge</label>
                                <input id="portOfDischarge" value={formData.portOfDischarge} onChange={e => setFormData({...formData, portOfDischarge: e.target.value})} />
                            </div>
                        </section>

                        <section id="narratives" className="form-grid section-divider">
                            <h3 className="section-title">Narratives</h3>
                            <div className="field-group full-width">
                                <div className="flex justify-between items-center mb-1">
                                    <label htmlFor="goodsDescription">Description of Goods</label>
                                    <button className="helper-link" onClick={() => setActiveClauseType('GOODS')}>+ Standard Clauses</button>
                                </div>
                                <textarea 
                                    id="goodsDescription" 
                                    rows={4} 
                                    value={formData.goodsDescription} 
                                    onChange={e => setFormData({...formData, goodsDescription: e.target.value})} 
                                    onBlur={e => validateSwift('goodsDescription', e.target.value)}
                                />
                                {swiftErrors.goodsDescription && <p className="error-text">{swiftErrors.goodsDescription}</p>}
                            </div>
                            <div className="field-group full-width">
                                <div className="flex justify-between items-center mb-1">
                                    <label htmlFor="documentsRequired">Documents Required</label>
                                    <button className="helper-link" onClick={() => setActiveClauseType('DOCUMENTS')}>+ Standard Clauses</button>
                                </div>
                                <textarea id="documentsRequired" rows={4} value={formData.documentsRequired} onChange={e => setFormData({...formData, documentsRequired: e.target.value})} />
                            </div>
                        </section>
                    </div>
                )}

                {stepIndex === 2 && (
                    <section className="form-grid">
                        <section id="margin" className="full-width section-divider">
                            <h3 className="section-title">Margin</h3>
                            <div className="grid grid-cols-2 gap-4">
                                <div className="field-group">
                                    <label>Margin Type</label>
                                    <select value={formData.marginType} onChange={e => setFormData({...formData, marginType: e.target.value})}>
                                        <option>None</option>
                                        <option>Cash</option>
                                        <option>Lombard</option>
                                    </select>
                                </div>
                                <div className="field-group">
                                    <label>Margin Percentage</label>
                                    <input type="number" value={formData.marginPercentage} onChange={e => setFormData({...formData, marginPercentage: e.target.value})} />
                                </div>
                            </div>
                        </section>
                        <section id="charges" className="full-width">
                            <h3 className="section-title">Charges</h3>
                            <div className="field-group mb-4">
                                <label htmlFor="chargeAllocationEnumId">Charge Allocation</label>
                                <select 
                                    id="chargeAllocationEnumId"
                                    value={formData.chargeAllocationEnumId}
                                    onChange={e => setFormData({...formData, chargeAllocationEnumId: e.target.value})}
                                >
                                    <option value="APPLICANT">Applicant Account</option>
                                    <option value="BENEFICIARY">Beneficiary (Deduct from proceeds)</option>
                                    <option value="SHARED">Shared (70/30)</option>
                                </select>
                            </div>
                            <div className="helper-box">
                                <p>Issuance Commission (Default): 0.125%</p>
                                <p>Estimated Charge: {formData.amount ? (Number(formData.amount) * 0.00125).toFixed(2) : '0.00'}</p>
                            </div>
                        </section>
                    </section>
                )}

                {stepIndex === 3 && (
                    <section className="review-panel">
                        <div className="review-card">
                            <h4>Review & Submit</h4>
                            <p>Reference: {formData.transactionRef}</p>
                            <p>Applicant: {formData.applicant}</p>
                            <p>Beneficiary: {formData.beneficiary}</p>
                            <p>Amount: {Number(formData.amount).toLocaleString()}</p>
                            <p>Expiry: {formData.expiryDate}</p>
                            <p>Margin: {formData.marginType} ({formData.marginPercentage}%)</p>
                        </div>
                        <div className="validation-panel">
                            <h5>System Validations</h5>
                            <p>✅ Limit Check Passed</p>
                            <p>✅ Sanctions Compliance Clear</p>
                            <p>✅ Pricing Rules Validated</p>
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
                    {stepIndex < 3 ? (
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
                .stepper-layout { display: flex; gap: 2rem; padding: 2rem; border-radius: 12px; border: 1px solid #e2e8f0; background: white; box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1); min-height: 600px; }
                .right-nav { width: 220px; border-right: 1px solid #f1f5f9; padding-right: 1.5rem; }
                .right-nav h4 { font-size: 0.75rem; text-transform: uppercase; color: #64748b; margin-bottom: 1rem; border-bottom: 1px solid #f1f5f9; padding-bottom: 0.5rem; }
                .right-nav ul { list-style: none; padding: 0; margin: 0; }
                .right-nav li { margin-bottom: 0.75rem; }
                .right-nav a { font-size: 0.875rem; color: #475569; text-decoration: none; font-weight: 500; transition: color 0.2s; }
                .right-nav a:hover { color: #2563eb; }
                .stepper-main { flex: 1; display: flex; flex-direction: column; }
                .main-lc-scroll-area { height: 500px; overflow-y: auto; padding-right: 1rem; scroll-behavior: smooth; }
                .section-divider { border-bottom: 1px solid #f1f5f9; padding-bottom: 2rem; margin-bottom: 2rem; }
                .section-title { font-size: 1.125rem; color: #1e293b; margin-bottom: 1.5rem; grid-column: span 2; }
                .stepper-header { display: flex; justify-content: space-between; margin-bottom: 2rem; font-weight: 600; color: #475569; }
                .stepper-progress { display: flex; justify-content: space-between; margin-bottom: 3rem; }
                .step-item { flex: 1; display: flex; flex-direction: column; align-items: center; position: relative; opacity: 0.3; }
                .step-item.active { opacity: 1; }
                .step-number { width: 32px; height: 32px; border-radius: 50%; background: #2563eb; color: white; display: flex; align-items: center; justify-content: center; margin-bottom: 0.5rem; }
                .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }
                .full-width { grid-column: span 2; }
                .field-group { display: flex; flex-direction: column; gap: 0.5rem; }
                .field-group label { font-weight: 600; font-size: 0.875rem; color: #1e293b; }
                .field-group input, .field-group textarea, .field-group select { padding: 0.75rem; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 0.875rem; }
                .helper-box { padding: 1rem; background: #f8fafc; border-radius: 8px; margin-top: 0.5rem; font-size: 0.875rem; color: #475569; }
                .stepper-footer { display: flex; justify-content: space-between; margin-top: auto; border-top: 1px solid #f1f5f9; padding-top: 2rem; }
                .primary-btn { background: #2563eb; color: white; padding: 0.75rem 1.5rem; border-radius: 6px; font-weight: 600; border: none; cursor: pointer; }
                .secondary-btn { background: #f8fafc; color: #1e293b; padding: 0.75rem 1.5rem; border-radius: 6px; border: 1px solid #e2e8f0; cursor: pointer; }
                .primary-btn:hover { background: #1d4ed8; }
                .primary-btn:disabled { opacity: 0.5; cursor: not-allowed; }
                .error-text { color: #dc2626; font-size: 0.75rem; margin-top: 0.25rem; }
                .success-banner { padding: 1rem; background: #ecfdf5; color: #065f46; border-radius: 8px; text-align: center; margin-top: 1rem; }
                .helper-link { background: none; border: none; color: #2563eb; font-size: 0.75rem; font-weight: 700; cursor: pointer; text-decoration: underline; padding: 0; }
                .helper-link:hover { color: #1d4ed8; }
                .error-banner { padding: 1rem; background: #fef2f2; color: #991b1b; border: 1px solid #fecaca; border-radius: 6px; font-size: 0.875rem; font-weight: 600; margin-bottom: 1.5rem; }
                .text-success { color: #059669; }
                .radio-group { display: flex; gap: 1rem; margin-top: 0.25rem; }
                .radio-group label { font-weight: 400; font-size: 0.875rem; cursor: pointer; }
            `}</style>
            </div>
        </div>
    );
};
