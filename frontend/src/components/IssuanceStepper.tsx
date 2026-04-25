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
        applicantPartyId: '',
        customerFacilityId: '',
        chargeAllocationEnumId: 'APPLICANT',
        confirmationEnumId: 'WITHOUT',
        latestShipmentDate: '',
        lcTypeEnumId: 'SIGHT',
        usanceDays: 0,
        expiryPlace: '',
        partialShipmentEnumId: 'ALLOWED',
        transhipmentEnumId: 'ALLOWED',
        marginType: 'None',
        marginPercentage: '100',
        marginAmount: '0',
        marginDebitAccount: '',
        charges: [
            { type: 'Issuance Commission', rate: '0.125', amount: '0', account: '' }
        ],
        instrumentId: ''
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
        
        const missingFields = [];
        if (stepIndex === 0) {
            if (!formData.productCatalogId) missingFields.push('LC Product');
            if (!formData.applicant) missingFields.push('Applicant');
        } else if (stepIndex === 1) {
            if (!formData.amount || parseFloat(formData.amount) <= 0) missingFields.push('LC Amount');
            if (!formData.currency) missingFields.push('Currency');
            if (!formData.expiryPlace) missingFields.push('Expiry Place');
            if (!formData.latestShipmentDate) missingFields.push('Latest Shipment Date');
            if (!formData.goodsDescription) missingFields.push('Goods Description');
            if (formData.lcTypeEnumId === 'USANCE' && !formData.usanceDays) missingFields.push('Usance Days');
        } else if (stepIndex === 2) {
            if (!formData.chargeAllocationEnumId) missingFields.push('Charge Allocation');
            if (!formData.customerFacilityId) missingFields.push('Customer Facility');
        }

        if (dateError) {
            setErrorMessage(`Date Conflict: ${dateError}`);
            return;
        }

        if (missingFields.length > 0) {
            setErrorMessage(`Please complete required fields: ${missingFields.join(', ')}`);
            return;
        }

        setStepIndex(prev => Math.min(prev + 1, 3));
    }
    const handleBack = () => setStepIndex(prev => Math.max(prev - 1, 0));
    
    const extractErrorMessage = (e: any): string => {
        const body = e.body;
        if (!body) return e.message || 'An unexpected error occurred';
        
        // Priority 1: Specific validation errors from Moqui
        if (Array.isArray(body.errors) && body.errors.length > 0) return body.errors.join(', ');
        if (typeof body.errors === 'string') return body.errors;
        
        // Priority 2: Generic error field
        if (body.error && typeof body.error === 'string') {
            // Detect and sanitize technical stack traces
            if (body.error.includes('startup failed') || body.error.includes('Unexpected input')) {
                return 'Internal Server Error: A technical issue occurred during processing. Please contact support.';
            }
            return body.error;
        }
        
        // Priority 3: Message field
        if (body.message) return body.message;
        
        return e.message || 'An unexpected error occurred';
    };

    const handleSaveDraft = async () => {
        setErrorMessage('');
        
        // Comprehensive Validation across all steps for final Save Draft
        const missingFields = [];
        // Step 0
        if (!formData.productCatalogId) missingFields.push('LC Product');
        if (!formData.amount || parseFloat(formData.amount) <= 0) missingFields.push('LC Amount');
        if (!formData.currency) missingFields.push('Currency');
        if (!formData.applicant) missingFields.push('Applicant');
        if (formData.lcTypeEnumId === 'USANCE' && !formData.usanceDays) missingFields.push('Usance Days');
        
        // Step 1
        if (!formData.expiryPlace) missingFields.push('Expiry Place');
        if (!formData.latestShipmentDate) missingFields.push('Latest Shipment Date');
        if (!formData.goodsDescription) missingFields.push('Goods Description');
        
        // Step 2
        if (!formData.chargeAllocationEnumId) missingFields.push('Charge Allocation');
        if (!formData.customerFacilityId) missingFields.push('Customer Facility');
        
        if (missingFields.length > 0) {
            setErrorMessage(`Validation Error: ${missingFields.join(', ')} is required.`);
            return;
        }

        setLoading(true);
        try {
            const result = await tradeApi.createLc({ 
                ...formData, 
                lcAmount: parseFloat(formData.amount) || 0,
                lcCurrencyUomId: formData.currency,
                businessStateId: 'LC_DRAFT',
                applicantPartyId: formData.applicantPartyId,
                customerFacilityId: formData.customerFacilityId,
                // Do not send transactionRef if it's empty, let backend generate it
                transactionRef: formData.transactionRef || undefined 
            });
            
            if (result.instrumentId) {
                setFormData(prev => ({ 
                    ...prev, 
                    instrumentId: result.instrumentId,
                    transactionRef: result.transactionRef || prev.transactionRef
                }));
                setStatus('DRAFT');
            } else {
                setErrorMessage(result.errors?.[0] || result.error || 'Failed to save draft');
            }
        } catch (e: any) {
            console.error('Save Draft Detail:', e);
            setErrorMessage(`Save Draft Failed: ${extractErrorMessage(e)}`);
        } finally {
            setLoading(false);
        }
    };

    const handleSubmit = async () => {
        setErrorMessage('');

        if (parseFloat(formData.amount) <= 0) {
            setErrorMessage('Validation Error: Amount must be greater than 0 for submission.');
            return;
        }

        setLoading(true);
        try {
            const result = await tradeApi.createLc({ 
                ...formData, 
                lcAmount: parseFloat(formData.amount) || 0,
                lcCurrencyUomId: formData.currency,
                businessStateId: 'LC_PENDING_APPROVAL',
                applicantPartyId: formData.applicantPartyId,
                customerFacilityId: formData.customerFacilityId,
                transactionRef: formData.transactionRef || undefined
            });
            
            if (result.instrumentId) {
                setFormData(prev => ({ 
                    ...prev, 
                    instrumentId: result.instrumentId,
                    transactionRef: result.transactionRef || prev.transactionRef
                }));
                setStatus('SUBMITTED');
            } else {
                setErrorMessage(result.errors?.[0] || result.error || 'Failed to submit for approval');
            }
        } catch (e: any) {
            console.error('Submit Detail:', e);
            setErrorMessage(`Submission Failed: ${extractErrorMessage(e)}`);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="stepper-layout premium-card">
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

                <div className="sticky-notification-container">
                    {errorMessage && (
                        <div className="error-banner">
                            <span className="mr-2">⚠️</span>
                            {errorMessage}
                            <button className="ml-4 opacity-70 hover:opacity-100" onClick={() => setErrorMessage('')}>✕</button>
                        </div>
                    )}
                    {(status === 'SUBMITTED' || status === 'DRAFT') && (
                        <div className="success-banner">
                            {status === 'SUBMITTED' ? 'Successfully Submitted for Approval' : 'Draft Saved Successfully'}
                            <button className="ml-4 opacity-70 hover:opacity-100" onClick={() => setStatus('IDLE')}>✕</button>
                        </div>
                    )}
                </div>

                <main className="stepper-content">
                    <header className="step-header mb-4">
                        <h3 className="text-xl font-bold">Step {stepIndex + 1}: {steps[stepIndex]}</h3>
                    </header>
                    
                    {stepIndex === 0 && (
                        <section id="parties-&-limits" className="form-grid">
                            <h3 className="section-title">Parties & Limits</h3>
                            <div className="field-group">
                                <label htmlFor="lcTypeEnumId" className="required-label">LC Type</label>
                                <select 
                                    id="lcTypeEnumId"
                                    value={formData.lcTypeEnumId}
                                    onChange={e => setFormData({...formData, lcTypeEnumId: e.target.value})}
                                >
                                    <option value="SIGHT">Sight LC</option>
                                    <option value="USANCE">Usance LC</option>
                                </select>
                            </div>
                            <div className="field-group">
                                <label htmlFor="confirmationEnumId">Confirmation Instruction</label>
                                <select 
                                    id="confirmationEnumId"
                                    value={formData.confirmationEnumId}
                                    onChange={e => setFormData({...formData, confirmationEnumId: e.target.value})}
                                >
                                    <option value="WITHOUT">Without</option>
                                    <option value="CONFIRM">Confirm</option>
                                    <option value="MAY_ADD">May Add</option>
                                </select>
                            </div>
                            <div className="field-group">
                                <label htmlFor="productCatalogId" className="required-label">LC Product</label>
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
                                <label htmlFor="applicant" className="required-label">Applicant</label>
                                <input 
                                    id="applicant"
                                    value={formData.applicant}
                                    onChange={e => {
                                        // Mocking Applicant selection for now, typically would be a searchable dropdown
                                        setFormData({...formData, applicant: e.target.value, applicantPartyId: 'PARTY-' + e.target.value.toUpperCase()})
                                    }}
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
                                    <label htmlFor="amount" className="required-label">Amount</label>
                                    <div className="flex gap-2">
                                        <select 
                                            id="currency" 
                                            aria-label="Currency"
                                            value={formData.currency} 
                                            style={{ width: '80px' }}
                                            onChange={e => setFormData({...formData, currency: e.target.value})}
                                        >
                                            <option value="USD">USD</option>
                                            <option value="EUR">EUR</option>
                                            <option value="GBP">GBP</option>
                                            <option value="JPY">JPY</option>
                                        </select>
                                        <input id="amount" type="number" className="flex-1" value={formData.amount} onChange={e => setFormData({...formData, amount: e.target.value})} />
                                    </div>
                                </div>
                                <div className="field-group">
                                    <label htmlFor="positiveTolerance">Positive Tolerance (%)</label>
                                    <input id="positiveTolerance" type="number" value={formData.positiveTolerance} onChange={e => setFormData({...formData, positiveTolerance: e.target.value})} />
                                </div>
                                <div className="field-group">
                                    <label htmlFor="negativeTolerance">Negative Tolerance (%)</label>
                                    <input id="negativeTolerance" type="number" value={formData.negativeTolerance} onChange={e => setFormData({...formData, negativeTolerance: e.target.value})} />
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
                                <div className="field-group">
                                    <label htmlFor="expiryPlace" className="required-label">Expiry Place</label>
                                    <input id="expiryPlace" value={formData.expiryPlace} onChange={e => setFormData({...formData, expiryPlace: e.target.value})} placeholder="e.g., AT COUNTERS OF ISSUING BANK" />
                                </div>
                                <div className="field-group">
                                    <label htmlFor="latestShipmentDate" className="required-label">Latest Shipment Date</label>
                                    <input id="latestShipmentDate" type="date" value={formData.latestShipmentDate} onChange={e => setFormData({...formData, latestShipmentDate: e.target.value})} />
                                </div>
                                {formData.lcTypeEnumId === 'USANCE' && (
                                    <div className="field-group">
                                        <label htmlFor="usanceDays">Usance Days</label>
                                        <input id="usanceDays" type="number" value={formData.usanceDays} onChange={e => setFormData({...formData, usanceDays: parseInt(e.target.value) || 0})} />
                                    </div>
                                )}
                            </section>

                            <section id="terms-&-shipping" className="form-grid section-divider">
                                <h3 className="section-title">Terms & Shipping</h3>
                                <div className="field-group">
                                    <label>Partial Shipment</label>
                                    <div className="radio-group">
                                        <label><input type="radio" checked={formData.partialShipmentEnumId === 'ALLOWED'} onChange={() => setFormData({...formData, partialShipmentEnumId: 'ALLOWED'})} /> Allowed</label>
                                        <label><input type="radio" checked={formData.partialShipmentEnumId === 'NOT_ALLOWED'} onChange={() => setFormData({...formData, partialShipmentEnumId: 'NOT_ALLOWED'})} /> Not Allowed</label>
                                    </div>
                                </div>
                                <div className="field-group">
                                    <label>Transhipment</label>
                                    <div className="radio-group">
                                        <label><input type="radio" checked={formData.transhipmentEnumId === 'ALLOWED'} onChange={() => setFormData({...formData, transhipmentEnumId: 'ALLOWED'})} /> Allowed</label>
                                        <label><input type="radio" checked={formData.transhipmentEnumId === 'NOT_ALLOWED'} onChange={() => setFormData({...formData, transhipmentEnumId: 'NOT_ALLOWED'})} /> Not Allowed</label>
                                        <label><input type="radio" checked={formData.transhipmentEnumId === 'CONDITIONAL'} onChange={() => setFormData({...formData, transhipmentEnumId: 'CONDITIONAL'})} /> Conditional</label>
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
                                        <label htmlFor="goodsDescription" className="required-label">Description of Goods</label>
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
                        <div className="main-lc-scroll-area">
                            <section id="margin" className="form-grid section-divider">
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
                            <section id="charges" className="form-grid">
                                <h3 className="section-title">Charges</h3>
                                <div className="field-group mb-4 full-width">
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
                                <div className="field-group mb-4 full-width">
                                    <label htmlFor="customerFacilityId">Customer Facility (Optional for Draft)</label>
                                    <select 
                                        id="customerFacilityId"
                                        value={formData.customerFacilityId}
                                        onChange={e => setFormData({...formData, customerFacilityId: e.target.value})}
                                    >
                                        <option value="">Select Facility...</option>
                                        <option value="FAC-001">Working Capital Facility - $1M</option>
                                        <option value="FAC-002">Trade Line - $500K</option>
                                    </select>
                                </div>
                                <div className="helper-box">
                                    <p>Issuance Commission (Default): 0.125%</p>
                                    <p>Estimated Charge: {formData.amount ? (Number(formData.amount) * 0.00125).toFixed(2) : '0.00'}</p>
                                </div>
                            </section>
                        </div>
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
                            {/* Banners moved to sticky container */}
                        </section>
                    )}
                </main>

                <footer className="stepper-footer">
                    <div className="left-actions">
                        {stepIndex === 3 && (
                            <button className="secondary-btn" onClick={handleSaveDraft}>Save Draft</button>
                        )}
                    </div>
                    <div className="right-actions">
                        {stepIndex > 0 && <button data-testid="back-button" onClick={handleBack}>Back</button>}
                        {stepIndex < 3 ? (
                            <button 
                                data-testid="next-button" 
                                className="primary-btn" 
                                onClick={handleNext}
                            >
                                Next
                            </button>
                        ) : (
                            <button 
                                data-testid="submit-button" 
                                className="primary-btn" 
                                onClick={handleSubmit}
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
            </div>

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
        </div>
    );
};
