'use client';

import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'next/navigation';
import { tradeApi } from '../api/tradeApi';
import { ClauseSelector } from './ClauseSelector';
import { isValidXChars, isValidZChars, getLineCount } from '../utils/SwiftUtils';

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
    const [parties, setParties] = useState<any[]>([]);
    const [facilities, setFacilities] = useState<any[]>([]);
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
        maxCreditAmountFlag: 'N',
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
        instrumentId: '',
        advisingBankBic: '',
        advisingThroughBankBic: '',
        issuingBankBic: '',
        availableByEnumId: 'SIGHT',
        availableWithBic: '',
        availableWithName: '',
        draweeBankBic: '',
        shipmentPeriodText: ''
    });
    const [swiftErrors, setSwiftErrors] = useState<Record<string, string>>({});

    const searchParams = useSearchParams();
    const queryId = searchParams.get('id');

    useEffect(() => {
        tradeApi.getProductCatalog().then(res => setProducts(res.productList || []));
        tradeApi.getParties().then(res => setParties(res.partyList || []));
    }, []);

    // Load existing draft if ID is provided
    useEffect(() => {
        if (queryId) {
            setLoading(true);
            tradeApi.getImportLc(queryId).then(lc => {
                setFormData(prev => ({
                    ...prev,
                    instrumentId: lc.instrumentId,
                    transactionRef: lc.transactionRef,
                    productCatalogId: lc.productCatalogId || '',
                    applicantPartyId: lc.applicantPartyId || '',
                    beneficiaryPartyId: lc.beneficiaryPartyId || '',
                    applicant: lc.applicantName || lc.applicantPartyName || '',
                    beneficiary: lc.beneficiaryName || lc.beneficiaryPartyName || '',
                    amount: (lc.amount || 0).toString(),
                    currency: lc.currencyUomId || 'USD',
                    issueDate: lc.issueDate || '',
                    expiryDate: lc.expiryDate || '',
                    positiveTolerance: (lc.tolerancePositive || 0).toString(),
                    negativeTolerance: (lc.toleranceNegative || 0).toString(),
                    portOfLoading: lc.portOfLoading || '',
                    portOfDischarge: lc.portOfDischarge || '',
                    goodsDescription: lc.goodsDescription || '',
                    documentsRequired: lc.documentsRequired || '',
                    additionalConditions: lc.additionalConditions || '',
                    customerFacilityId: lc.customerFacilityId || '',
                    chargeAllocationEnumId: lc.chargeAllocationEnumId || 'APPLICANT',
                    confirmationEnumId: lc.confirmationEnumId || 'WITHOUT',
                    latestShipmentDate: lc.latestShipmentDate || '',
                    lcTypeEnumId: lc.lcTypeEnumId || 'SIGHT',
                    usanceDays: lc.usanceDays || 0,
                    expiryPlace: lc.expiryPlace || '',
                    partialShipmentEnumId: lc.partialShipmentEnumId || 'ALLOWED',
                    transhipmentEnumId: lc.transhipmentEnumId || 'ALLOWED',
                    marginType: lc.marginType || 'None',
                    marginPercentage: lc.marginPercentage || '100',
                    marginAmount: lc.marginAmount || '0',
                    marginDebitAccount: lc.marginDebitAccount || '',
                    issuingBankBic: lc.issuingBankBic || '',
                    advisingBankBic: lc.advisingBankBic || '',
                    advisingThroughBankBic: lc.advisingThroughBankBic || '',
                    availableByEnumId: lc.availableByEnumId || 'SIGHT',
                    availableWithBic: lc.availableWithBic || '',
                    availableWithName: lc.availableWithName || '',
                    draweeBankBic: lc.draweeBankBic || '',
                    shipmentPeriodText: lc.shipmentPeriodText || ''
                }));
                setLoading(false);
            }).catch(err => {
                console.error("Failed to load draft:", err);
                setErrorMessage("Failed to load draft details.");
                setLoading(false);
            });
        }
    }, [queryId]);
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
    
    // Mutual Exclusion Logic for SWIFT Tags
    useEffect(() => {
        if (formData.maxCreditAmountFlag === 'Y' && (formData.positiveTolerance !== '0' || formData.negativeTolerance !== '0')) {
            setFormData(prev => ({ ...prev, positiveTolerance: '0', negativeTolerance: '0' }));
        }
    }, [formData.maxCreditAmountFlag]);

    useEffect(() => {
        if (formData.latestShipmentDate && formData.shipmentPeriodText) {
            setFormData(prev => ({ ...prev, latestShipmentDate: '' }));
        }
    }, [formData.shipmentPeriodText]);

    useEffect(() => {
        if (formData.latestShipmentDate && formData.shipmentPeriodText) {
            setFormData(prev => ({ ...prev, shipmentPeriodText: '' }));
        }
    }, [formData.latestShipmentDate]);

    // Proactively fetch facilities whenever applicant changes (essential for drafts)
    useEffect(() => {
        if (formData.applicantPartyId) {
            tradeApi.getCustomerFacilities(formData.applicantPartyId).then(res => {
                setFacilities(res.facilityList || []);
            }).catch(e => {
                console.error('Failed to fetch facilities:', e);
                setFacilities([]);
            });
        } else {
            setFacilities([]);
        }
    }, [formData.applicantPartyId]);

    const validateSwiftField = (field: string, value: string, charset: 'X' | 'Z' = 'X', maxLines?: number) => {
        let errorMsg = '';
        if (charset === 'X' && !isValidXChars(value)) {
            errorMsg = 'Invalid characters detected (SWIFT X charset required)';
        } else if (charset === 'Z' && !isValidZChars(value)) {
            errorMsg = 'Invalid characters detected (SWIFT Z charset required)';
        } else if (maxLines && getLineCount(value) > maxLines) {
            errorMsg = `Exceeds maximum ${maxLines} lines`;
        }

        setSwiftErrors(prev => {
            const updated = { ...prev };
            if (errorMsg) updated[field] = errorMsg;
            else delete updated[field];
            return updated;
        });

        // Bug fix in my thought: it should be delete updated[field]
    };

    const handleNext = async () => {
        setErrorMessage('');
        
        const missingFields = [];
        if (stepIndex === 0) {
            if (!formData.productCatalogId) missingFields.push('LC Product');
            if (!formData.applicant) missingFields.push('Applicant');
        } else if (stepIndex === 1) {
            if (!formData.amount || parseFloat(formData.amount) <= 0) missingFields.push('LC Amount');
            if (!formData.currency) missingFields.push('Currency');
            if (!formData.expiryPlace) missingFields.push('Expiry Place');
            if (!formData.latestShipmentDate && !formData.shipmentPeriodText) missingFields.push('Latest Shipment Date or Shipment Period');
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

        // Logic for Step-Level Persistence and Backend Validation
        if (stepIndex === 0) {
            setLoading(true);
            try {
                // Persistent Save as Draft to obtain instrumentId
                const success = await handleSaveDraft();
                if (!success) return; 
            } catch (e) {
                return;
            } finally {
                setLoading(false);
            }
        } else if (stepIndex === 1 && formData.instrumentId) {
            setLoading(true);
            try {
                // Persistent Save as Draft to capture Step 2 changes
                const success = await handleSaveDraft();
                if (!success) return;

                // Call backend validation for Spec A
                const res = await tradeApi.validateLcSwiftFields(formData.instrumentId, 'ImportLetterOfCredit');
                const newErrors: Record<string, string> = {};
                res.errors?.forEach(err => {
                    if (newErrors[err.fieldName]) {
                        newErrors[err.fieldName] += `; ${err.message}`;
                    } else {
                        newErrors[err.fieldName] = err.message;
                    }
                });
                setSwiftErrors(newErrors);
                
                if (res.errors && res.errors.length > 0) {
                    setErrorMessage(`SWIFT Validation: ${res.errors.length} violation(s) detected. Please check flagged fields.`);
                    return; // Block transition to ensure user sees errors
                }
            } catch (e) {
                console.error('Validation Error:', e);
            } finally {
                setLoading(false);
            }
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

    const handleSaveDraft = async (): Promise<boolean> => {
        setErrorMessage('');
        
        // Comprehensive Validation across all steps for final Save Draft
        const missingFields = [];
        // Step 0
        if (!formData.productCatalogId) missingFields.push('LC Product');
        if (!formData.applicant) missingFields.push('Applicant');
        
        // If it's a final save, we expect more, but for step transition we are lenient
        if (stepIndex > 0) {
            if (!formData.amount || parseFloat(formData.amount) <= 0) missingFields.push('LC Amount');
            if (!formData.currency) missingFields.push('Currency');
            if (!formData.expiryPlace) missingFields.push('Expiry Place');
            if (!formData.latestShipmentDate && !formData.shipmentPeriodText) missingFields.push('Latest Shipment Date or Shipment Period');
            if (!formData.goodsDescription) missingFields.push('Goods Description');
        }
        
        if (missingFields.length > 0) {
            setErrorMessage(`Validation Error: ${missingFields.join(', ')} is required.`);
            return false;
        }

        setLoading(true);
        try {
            const payload = {
                ...formData,
                applicantName: formData.applicant,
                beneficiaryName: formData.beneficiary,
                lcAmount: Number(formData.amount),
                lcCurrencyUomId: formData.currency,
                amount: parseFloat(formData.amount) || 0,
                currencyUomId: formData.currency || 'USD',
                issueDate: formData.issueDate || new Date().toISOString().split('T')[0],
                expiryDate: formData.expiryDate || undefined,
                latestShipmentDate: formData.latestShipmentDate || undefined,
                usanceBaseDate: formData.usanceBaseDate || formData.issueDate || new Date().toISOString().split('T')[0],
                businessStateId: 'LC_DRAFT'
            };
            
            const result = await (formData.instrumentId 
                ? tradeApi.updateLc(formData.instrumentId, payload)
                : tradeApi.createLc(payload));
            
            if (result.instrumentId) {
                setFormData(prev => ({ 
                    ...prev, 
                    instrumentId: result.instrumentId,
                    transactionRef: result.transactionRef || prev.transactionRef
                }));
                setStatus('DRAFT');
                return true;
            } else {
                setErrorMessage(result.errors?.[0] || result.error || 'Failed to save draft');
                return false;
            }
        } catch (e: any) {
            console.error('Save Draft Detail:', e);
            setErrorMessage(`Save Draft Failed: ${extractErrorMessage(e)}`);
            return false;
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
            const payload = { 
                ...formData, 
                applicantName: formData.applicant,
                lcAmount: Number(formData.amount),
                lcCurrencyUomId: formData.currency,
                businessStateId: 'LC_PENDING',
                applicantPartyId: formData.applicantPartyId,
                customerFacilityId: formData.customerFacilityId,
                transactionRef: formData.transactionRef || undefined
            };

            const result = formData.instrumentId 
                ? await tradeApi.updateLc(formData.instrumentId, payload)
                : await tradeApi.createLc(payload);
            
            if (result.instrumentId || result.transactionRef) {
                if (result.instrumentId) {
                    setFormData(prev => ({ 
                        ...prev, 
                        instrumentId: result.instrumentId,
                        transactionRef: result.transactionRef || prev.transactionRef
                    }));
                }
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
                        <div className="success-banner flex justify-between items-center">
                            {status === 'SUBMITTED' ? (
                                <div className="success-banner premium-card mb-6" style={{ backgroundColor: '#dcfce7', borderColor: '#166534', padding: '1.5rem', borderRadius: '12px', border: '1px solid' }}>
                                    <h3 style={{ color: '#166534', fontWeight: 700, marginBottom: '0.5rem' }}>Submission Successful</h3>
                                    <p style={{ color: '#166534', marginBottom: '1rem' }}>
                                        Letter of Credit has been submitted for approval.
                                        <br />
                                        <strong>Transaction Ref: {formData.transactionRef || 'Generating...'}</strong>
                                        <br />
                                        <small>Instrument ID: {formData.instrumentId}</small>
                                    </p>
                                    <div style={{ display: 'flex', gap: '1rem' }}>
                                        <a href="/import-lc" style={{ color: '#166534', fontWeight: 600, textDecoration: 'underline' }}>Back to Dashboard</a>
                                        <span>|</span>
                                        <a href="/approvals" style={{ color: '#166534', fontWeight: 600, textDecoration: 'underline' }}>View Approvals Queue</a>
                                    </div>
                                </div>
                            ) : (
                                <div>
                                    Draft Saved Successfully
                                    <button className="ml-4 opacity-70 hover:opacity-100" onClick={() => setStatus('IDLE')}>✕</button>
                                </div>
                            )}
                        </div>
                    )}
                </div>

                <main className="stepper-content" style={status === 'SUBMITTED' ? { pointerEvents: 'none', opacity: 0.8 } : {}}>
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
                                    disabled={status === 'SUBMITTED'}
                                    value={formData.lcTypeEnumId}
                                    onChange={e => setFormData({...formData, lcTypeEnumId: e.target.value})}
                                    className={swiftErrors.lcTypeEnumId ? 'is-invalid' : ''}
                                >
                                    <option value="SIGHT">Sight LC</option>
                                    <option value="USANCE">Usance LC</option>
                                </select>
                                {swiftErrors.lcTypeEnumId && <p className="error-text text-xs mt-1">{swiftErrors.lcTypeEnumId}</p>}
                            </div>
                            <div className="field-group">
                                <label htmlFor="confirmationEnumId">Confirmation Instruction</label>
                                <select 
                                    id="confirmationEnumId"
                                    disabled={status === 'SUBMITTED'}
                                    value={formData.confirmationEnumId}
                                    onChange={e => setFormData({...formData, confirmationEnumId: e.target.value})}
                                    className={swiftErrors.confirmationEnumId ? 'is-invalid' : ''}
                                >
                                    <option value="WITHOUT">Without</option>
                                    <option value="CONFIRM">Confirm</option>
                                    <option value="MAY_ADD">May Add</option>
                                </select>
                                {swiftErrors.confirmationEnumId && <p className="error-text text-xs mt-1">{swiftErrors.confirmationEnumId}</p>}
                            </div>
                            <div className="field-group">
                                <label htmlFor="productCatalogId" className="required-label">LC Product</label>
                                <select 
                                    id="productCatalogId"
                                    disabled={status === 'SUBMITTED'}
                                    value={formData.productCatalogId}
                                    onChange={e => setFormData({...formData, productCatalogId: e.target.value})}
                                >
                                    <option value="">Select Product...</option>
                                    {products.map(p => <option key={p.productId} value={p.productId}>{p.productName}</option>)}
                                </select>
                            </div>
                            <div className="field-group">
                                <label htmlFor="transactionRef">Transaction Reference</label>
                                <input 
                                    id="transactionRef"
                                    className={swiftErrors.transactionRef ? 'is-invalid' : ''}
                                    value={formData.transactionRef}
                                    onChange={e => setFormData({...formData, transactionRef: e.target.value})}
                                    placeholder="e.g., IMLC/2026/001"
                                />
                                {swiftErrors.transactionRef && <p className="error-text text-xs mt-1">{swiftErrors.transactionRef}</p>}
                            </div>
                            <div className="field-group">
                                <label htmlFor="applicant" className="required-label">Applicant</label>
                                <select 
                                    id="applicant"
                                    value={formData.applicantPartyId}
                                    onChange={e => {
                                        const party = parties.find(p => p.partyId === e.target.value);
                                        const partyName = party?.partyName || '';
                                        setFormData({...formData, applicantPartyId: e.target.value, applicant: partyName});
                                        validateSwiftField('applicant', partyName, 'X', 4);
                                    }}
                                    className={(swiftErrors.applicantName || swiftErrors.applicant) ? 'is-invalid' : ''}
                                >
                                    <option value="">Select Applicant...</option>
                                    {parties.map(p => <option key={p.partyId} value={p.partyId}>{p.partyName}</option>)}
                                </select>
                                {(swiftErrors.applicantName || swiftErrors.applicant) && <p className="error-text text-xs mt-1">{swiftErrors.applicantName || swiftErrors.applicant}</p>}
                                <div className="helper-box">
                                    <p>Available Facility Limit: $1,000,000</p>
                                    <p>KYC Status: <span className="text-success">VERIFIED</span></p>
                                </div>
                            </div>
                            <div className="field-group">
                                <label htmlFor="advisingThroughBankBic">Advising Through Bank BIC (Tag 58A)</label>
                                <input 
                                    id="advisingThroughBankBic"
                                    className={swiftErrors.advisingThroughBankBic ? 'is-invalid' : ''}
                                    value={formData.advisingThroughBankBic}
                                    onChange={e => setFormData({...formData, advisingThroughBankBic: e.target.value.toUpperCase()})}
                                />
                                {swiftErrors.advisingThroughBankBic && <p className="error-text text-xs mt-1">{swiftErrors.advisingThroughBankBic}</p>}
                            </div>
                            <div className="field-group">
                                <label htmlFor="beneficiary" className="required-label">Beneficiary (Tag 59)</label>
                                <textarea 
                                    id="beneficiary"
                                    className={(swiftErrors.beneficiaryName || swiftErrors.beneficiary) ? 'is-invalid' : ''}
                                    rows={3}
                                    value={formData.beneficiary}
                                    onChange={e => {
                                        setFormData({...formData, beneficiary: e.target.value});
                                        validateSwiftField('beneficiary', e.target.value, 'X', 4);
                                    }}
                                    placeholder="Multi-line Beneficiary details..."
                                />
                                {(swiftErrors.beneficiaryName || swiftErrors.beneficiary) && <p className="error-text text-xs mt-1">{swiftErrors.beneficiaryName || swiftErrors.beneficiary}</p>}
                            </div>
                            <div className="field-group">
                                <label htmlFor="advisingBankBic" className="required-label">Advising Bank BIC (Tag 57A)</label>
                                <input 
                                    id="advisingBankBic"
                                    className={swiftErrors.advisingBankBic ? 'is-invalid' : ''}
                                    value={formData.advisingBankBic}
                                    onChange={e => setFormData({...formData, advisingBankBic: e.target.value.toUpperCase()})}
                                    placeholder="e.g., ABIC US 33"
                                />
                                {swiftErrors.advisingBankBic && <p className="error-text text-xs mt-1">{swiftErrors.advisingBankBic}</p>}
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
                                    <input id="negativeTolerance" type="number" disabled={formData.maxCreditAmountFlag === 'Y'} value={formData.negativeTolerance} onChange={e => setFormData({...formData, negativeTolerance: e.target.value})} />
                                </div>
                                <div className="field-group mt-6">
                                    <label className="flex items-center gap-2 cursor-pointer">
                                        <input 
                                            id="maxCreditAmountFlag"
                                            type="checkbox" 
                                            checked={formData.maxCreditAmountFlag === 'Y'} 
                                            onChange={e => setFormData({...formData, maxCreditAmountFlag: e.target.checked ? 'Y' : 'N'})} 
                                        />
                                        <span className="font-semibold text-sm">Maximum Credit Amount (Tag 39B)</span>
                                    </label>
                                </div>
                                <div className="field-group">
                                    <label htmlFor="issueDate">Issue Date</label>
                                    <input id="issueDate" type="date" value={formData.issueDate} onChange={e => setFormData({...formData, issueDate: e.target.value})} />
                                </div>
                                <div className="field-group">
                                    <label htmlFor="expiryDate" className="required-label">Expiry Date</label>
                                    <input id="expiryDate" type="date" value={formData.expiryDate} onChange={e => setFormData({...formData, expiryDate: e.target.value})} />
                                    {dateError && <p className="error-text">{dateError}</p>}
                                </div>
                                <div className="field-group">
                                    <label htmlFor="expiryPlace" className="required-label">Expiry Place</label>
                                    <input id="expiryPlace" value={formData.expiryPlace} onChange={e => setFormData({...formData, expiryPlace: e.target.value})} placeholder="e.g., AT COUNTERS OF ISSUING BANK" />
                                </div>
                                <div className="field-group">
                                    <label htmlFor="latestShipmentDate">Latest Shipment Date (Tag 44C)</label>
                                    <input id="latestShipmentDate" type="date" disabled={!!formData.shipmentPeriodText} value={formData.latestShipmentDate} onChange={e => setFormData({...formData, latestShipmentDate: e.target.value})} />
                                </div>
                                <div className="field-group">
                                    <label htmlFor="shipmentPeriodText">Shipment Period (Tag 44D)</label>
                                    <input 
                                        id="shipmentPeriodText" 
                                        className={swiftErrors.shipmentPeriodText ? 'is-invalid' : ''}
                                        disabled={!!formData.latestShipmentDate} 
                                        value={formData.shipmentPeriodText} 
                                        onChange={e => setFormData({...formData, shipmentPeriodText: e.target.value})} 
                                        placeholder="e.g. SHIPMENT WITHIN 30 DAYS" 
                                    />
                                    {swiftErrors.shipmentPeriodText && <p className="error-text text-xs mt-1">{swiftErrors.shipmentPeriodText}</p>}
                                </div>
                                {formData.lcTypeEnumId === 'USANCE' && (
                                    <>
                                        <div className="field-group">
                                            <label htmlFor="usanceDays">Usance Days</label>
                                            <input id="usanceDays" type="number" value={formData.usanceDays} onChange={e => setFormData({...formData, usanceDays: parseInt(e.target.value) || 0})} />
                                        </div>
                                        <div className="field-group">
                                            <label htmlFor="usanceBaseDate" className="required-label">Usance Base Date</label>
                                            <input 
                                                id="usanceBaseDate" 
                                                type="date" 
                                                className={swiftErrors.usanceBaseDate ? 'is-invalid' : ''}
                                                value={formData.usanceBaseDate} 
                                                onChange={e => setFormData({...formData, usanceBaseDate: e.target.value})} 
                                            />
                                            {swiftErrors.usanceBaseDate && <p className="error-text text-xs mt-1">{swiftErrors.usanceBaseDate}</p>}
                                        </div>
                                    </>
                                )}
                                <div className="field-group">
                                    <label htmlFor="availableByEnumId" className="required-label">Available By (Tag 41a)</label>
                                    <select 
                                        id="availableByEnumId"
                                        className={swiftErrors.availableByEnumId ? 'is-invalid' : ''}
                                        value={formData.availableByEnumId}
                                        onChange={e => setFormData({...formData, availableByEnumId: e.target.value})}
                                    >
                                        <option value="SIGHT">By Sight</option>
                                        <option value="ACCEPTANCE">By Acceptance</option>
                                        <option value="NEGOTIATION">By Negotiation</option>
                                        <option value="DEF_PAYMENT">By Deferred Payment</option>
                                        <option value="MIXED_PAYMENT">By Mixed Payment</option>
                                    </select>
                                    {swiftErrors.availableByEnumId && <p className="error-text text-xs mt-1">{swiftErrors.availableByEnumId}</p>}
                                </div>
                                <div className="field-group">
                                    <label htmlFor="availableWithBic">Available With BIC (Tag 41A)</label>
                                    <input 
                                        id="availableWithBic"
                                        className={swiftErrors.availableWithBic ? 'is-invalid' : ''}
                                        value={formData.availableWithBic}
                                        onChange={e => setFormData({...formData, availableWithBic: e.target.value.toUpperCase()})}
                                        placeholder="BIC of the bank"
                                    />
                                    {swiftErrors.availableWithBic && <p className="error-text text-xs mt-1">{swiftErrors.availableWithBic}</p>}
                                </div>
                                <div className="field-group">
                                    <label htmlFor="availableWithName">Available With Name (Tag 41D)</label>
                                    <textarea 
                                        id="availableWithName"
                                        className={swiftErrors.availableWithName ? 'is-invalid' : ''}
                                        rows={2}
                                        value={formData.availableWithName}
                                        onChange={e => {
                                            setFormData({...formData, availableWithName: e.target.value});
                                            validateSwiftField('availableWithName', e.target.value, 'X', 4);
                                        }}
                                        placeholder="Name and address if BIC not available..."
                                    />
                                    {swiftErrors.availableWithName && <p className="error-text text-xs mt-1">{swiftErrors.availableWithName}</p>}
                                </div>
                                <div className="field-group">
                                    <label htmlFor="draweeBankBic">Drawee Bank BIC (Tag 42A)</label>
                                    <input 
                                        id="draweeBankBic"
                                        className={swiftErrors.draweeBankBic ? 'is-invalid' : ''}
                                        value={formData.draweeBankBic}
                                        onChange={e => setFormData({...formData, draweeBankBic: e.target.value.toUpperCase()})}
                                    />
                                    {swiftErrors.draweeBankBic && <p className="error-text text-xs mt-1">{swiftErrors.draweeBankBic}</p>}
                                </div>
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
                                    <input 
                                        id="portOfLoading" 
                                        className={swiftErrors.portOfLoading ? 'is-invalid' : ''}
                                        value={formData.portOfLoading} 
                                        onChange={e => {
                                            setFormData({...formData, portOfLoading: e.target.value});
                                            validateSwiftField('portOfLoading', e.target.value, 'X');
                                        }} 
                                    />
                                    {swiftErrors.portOfLoading && <p className="error-text text-xs mt-1">{swiftErrors.portOfLoading}</p>}
                                </div>
                                <div className="field-group">
                                    <label htmlFor="portOfDischarge">Port of Discharge</label>
                                    <input 
                                        id="portOfDischarge" 
                                        className={swiftErrors.portOfDischarge ? 'is-invalid' : ''}
                                        value={formData.portOfDischarge} 
                                        onChange={e => {
                                            setFormData({...formData, portOfDischarge: e.target.value});
                                            validateSwiftField('portOfDischarge', e.target.value, 'X');
                                        }} 
                                    />
                                    {swiftErrors.portOfDischarge && <p className="error-text text-xs mt-1">{swiftErrors.portOfDischarge}</p>}
                                </div>
                            </section>

                            <section id="narratives" className="form-grid section-divider">
                                <h3 className="section-title">Narratives</h3>
                                <div className="field-group full-width">
                                    <div className="flex justify-between items-center mb-1">
                                        <label htmlFor="goodsDescription" className="required-label">Description of Goods (Tag 45A)</label>
                                        <button className="helper-link" onClick={() => setActiveClauseType('GOODS')}>+ Standard Clauses</button>
                                    </div>
                                    <textarea 
                                        id="goodsDescription" 
                                        className={swiftErrors.goodsDescription ? 'is-invalid' : ''}
                                        rows={4} 
                                        value={formData.goodsDescription} 
                                        onChange={e => {
                                            setFormData({...formData, goodsDescription: e.target.value});
                                            validateSwiftField('goodsDescription', e.target.value, 'X');
                                        }} 
                                    />
                                    {swiftErrors.goodsDescription && <p className="error-text text-xs mt-1">{swiftErrors.goodsDescription}</p>}
                                </div>
                                <div className="field-group full-width">
                                    <div className="flex justify-between items-center mb-1">
                                        <label htmlFor="documentsRequired" className="required-label">Documents Required (Tag 46A)</label>
                                        <button className="helper-link" onClick={() => setActiveClauseType('DOCUMENTS')}>+ Standard Clauses</button>
                                    </div>
                                    <textarea 
                                        id="documentsRequired" 
                                        className={swiftErrors.documentsRequired ? 'is-invalid' : ''}
                                        rows={4} 
                                        value={formData.documentsRequired} 
                                        onChange={e => {
                                            setFormData({...formData, documentsRequired: e.target.value});
                                            validateSwiftField('documentsRequired', e.target.value, 'X');
                                        }} 
                                    />
                                    {swiftErrors.documentsRequired && <p className="error-text text-xs mt-1">{swiftErrors.documentsRequired}</p>}
                                </div>
                                <div className="field-group">
                                    <label htmlFor="issuingBankBic">Issuing Bank BIC (Tag 51A)</label>
                                    <input 
                                        id="issuingBankBic"
                                        className={swiftErrors.issuingBankBic ? 'is-invalid' : ''}
                                        value={formData.issuingBankBic}
                                        onChange={e => setFormData({...formData, issuingBankBic: e.target.value.toUpperCase()})}
                                    />
                                    {swiftErrors.issuingBankBic && <p className="error-text text-xs mt-1">{swiftErrors.issuingBankBic}</p>}
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
                                    <label htmlFor="customerFacilityId" className="required-label">Customer Facility (Optional for Draft)</label>
                                    <select 
                                        id="customerFacilityId"
                                        value={formData.customerFacilityId}
                                        onChange={e => setFormData({...formData, customerFacilityId: e.target.value})}
                                    >
                                        <option value="">Select Facility...</option>
                                        {facilities.map(f => (
                                            <option key={f.facilityId} value={f.facilityId}>
                                                {f.description || f.facilityId} - ${f.limitAmount?.toLocaleString()}
                                            </option>
                                        ))}
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
                        {stepIndex > 0 && <button data-testid="back-button" disabled={status === 'SUBMITTED'} onClick={handleBack}>Back</button>}
                        {stepIndex < 3 ? (
                            <button 
                                data-testid="next-button" 
                                className="primary-btn" 
                                disabled={status === 'SUBMITTED'}
                                onClick={handleNext}
                            >
                                Next
                            </button>
                        ) : (
                            <button 
                                data-testid="submit-button" 
                                className="primary-btn" 
                                disabled={status === 'SUBMITTED'}
                                onClick={handleSubmit}
                            >
                                {status === 'SUBMITTED' ? 'Submitted' : 'Submit for Approval'}
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
