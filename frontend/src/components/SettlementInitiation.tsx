'use client';

import React, { useState } from 'react';

// ABOUTME: Settlement Initiation component implementing REQ-IMP-PRC-04.
// ABOUTME: Handles accounting entry triggers for LC payments and fee collection.

interface SettlementInitiationProps {
    instrumentId: string;
}

export const SettlementInitiation: React.FC<SettlementInitiationProps> = ({ instrumentId }) => {
    const [formData, setFormData] = useState({
        debitAccount: '',
        valueDate: new Date().toISOString().split('T')[0],
        principalAmount: '0',
        charges: '150',
    });

    const [isCheckingFunds, setIsCheckingFunds] = useState(false);
    const [fundsVerified, setFundsVerified] = useState(false);

    const totalDebit = (parseFloat(formData.principalAmount) || 0) + (parseFloat(formData.charges) || 0);

    const handleCheckFunds = async () => {
        setIsCheckingFunds(true);
        // Simulation of backend fund verification
        await new Promise(resolve => setTimeout(resolve, 800));
        setFundsVerified(true);
        setIsCheckingFunds(false);
    };

    return (
        <div className="settlement-container premium-card">
            <header className="header-section">
                <h2 className="title">Settlement Initiation</h2>
                <p className="subtitle">Payment processing for Instrument: {instrumentId}</p>
            </header>

            <main className="form-grid">
                <section className="account-section">
                    <div className="field-group">
                        <label htmlFor="debitAccount">Applicant Debit Account</label>
                        <select 
                            id="debitAccount"
                            value={formData.debitAccount}
                            onChange={e => setFormData({...formData, debitAccount: e.target.value})}
                        >
                            <option value="">Select Account...</option>
                            <option value="ACC-101">Main Trading AC (USD) - *9921</option>
                            <option value="ACC-102">Collateral AC (USD) - *4412</option>
                        </select>
                    </div>
                    <div className="field-group">
                        <label htmlFor="valueDate">Value Date</label>
                        <input 
                            id="valueDate"
                            type="date"
                            value={formData.valueDate}
                            onChange={e => setFormData({...formData, valueDate: e.target.value})}
                        />
                    </div>
                </section>

                <section className="amounts-section">
                    <div className="field-group">
                        <label htmlFor="principalAmount">Principal Amount</label>
                        <input 
                            id="principalAmount"
                            type="number"
                            value={formData.principalAmount}
                            onChange={e => setFormData({...formData, principalAmount: e.target.value})}
                        />
                    </div>
                    <div className="field-group">
                        <label>Transaction Fees</label>
                        <input type="text" value={formData.charges} readOnly disabled />
                    </div>
                    <div className="summary-total">
                        <span className="label">Total Debit Amount</span>
                        <span className="value">$ {totalDebit.toLocaleString()}</span>
                    </div>
                </section>

                <section className="verification-section">
                    <button 
                        className={`verify-btn ${fundsVerified ? 'success' : ''}`}
                        onClick={handleCheckFunds}
                        disabled={isCheckingFunds || !formData.debitAccount}
                    >
                        {isCheckingFunds ? 'Verifying...' : fundsVerified ? '✅ Funds Verified' : 'Check Funds Availability'}
                    </button>
                </section>
            </main>

            <footer className="footer-actions">
                <button className="secondary-btn">Cancel</button>
                <button className="primary-btn" disabled={!fundsVerified}>Confirm & Execute Payment</button>
            </footer>

            <style jsx>{`
                .settlement-container { padding: 2rem; background: white; border-radius: 12px; }
                .header-section { margin-bottom: 2rem; border-bottom: 1px solid #f1f5f9; padding-bottom: 1rem; }
                .title { font-size: 1.25rem; font-weight: 700; color: #0f172a; }
                .subtitle { font-size: 0.875rem; color: #64748b; }

                .form-grid { display: grid; gap: 2rem; }
                .account-section, .amounts-section { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }
                
                .field-group { display: flex; flex-direction: column; gap: 0.5rem; }
                .field-group label { font-size: 0.875rem; font-weight: 600; color: #334155; }
                .field-group input, .field-group select { padding: 0.75rem; border: 1px solid #e2e8f0; border-radius: 6px; }

                .summary-total { grid-column: span 2; background: #f8fafc; padding: 1.5rem; border-radius: 8px; display: flex; justify-content: space-between; align-items: center; border: 1px dashed #cbd5e1; }
                .summary-total .label { font-weight: 600; color: #475569; }
                .summary-total .value { font-size: 1.5rem; font-weight: 800; color: #1e293b; }

                .verify-btn { width: 100%; padding: 1rem; border-radius: 8px; font-weight: 700; cursor: pointer; border: 1px solid #e2e8f0; background: #fff; color: #2563eb; transition: all 0.2s; }
                .verify-btn:hover { background: #f0f7ff; }
                .verify-btn.success { background: #ecfdf5; border-color: #10b981; color: #059669; }
                .verify-btn:disabled { opacity: 0.7; cursor: not-allowed; }

                .footer-actions { display: flex; justify-content: flex-end; gap: 1rem; margin-top: 3rem; border-top: 1px solid #f1f5f9; padding-top: 2rem; }
                .primary-btn { background: #2563eb; color: white; border: none; padding: 0.75rem 2rem; border-radius: 6px; font-weight: 600; cursor: pointer; }
                .primary-btn:disabled { opacity: 0.5; cursor: not-allowed; }
                .secondary-btn { background: #f8fafc; color: #475569; border: 1px solid #e2e8f0; padding: 0.75rem 2rem; border-radius: 6px; font-weight: 600; cursor: pointer; }
            `}</style>
        </div>
    );
};
