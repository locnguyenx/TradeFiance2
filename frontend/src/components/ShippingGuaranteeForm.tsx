'use client';

import React, { useState } from 'react';

// ABOUTME: Shipping Guarantee form implementing REQ-IMP-PRC-05.
// ABOUTME: Handles the 110% over-collateralization earmark requirement for early cargo release.

interface ShippingGuaranteeFormProps {
    instrumentId: string;
}

export const ShippingGuaranteeForm: React.FC<ShippingGuaranteeFormProps> = ({ instrumentId }) => {
    const [formData, setFormData] = useState({
        invoiceAmount: '0',
        transportDocRef: '',
        carrierName: '',
        vesselName: '',
        guaranteeType: 'Full Indemnity',
    });

    const earmarkAmount = (parseFloat(formData.invoiceAmount) || 0) * 1.1;

    return (
        <div className="sg-container premium-card">
            <header className="header-flex">
                <div>
                    <h2 className="title">Shipping Guarantee Request</h2>
                    <p className="subtitle">Linked to Parent LC: {instrumentId}</p>
                </div>
                <div className="limit-calc-box">
                    <span className="label">Total Limit Earmark (110%)</span>
                    <span className="value" data-testid="earmark-value">$ {earmarkAmount.toLocaleString()}</span>
                </div>
            </header>

            <main className="form-grid">
                <section className="cargo-details">
                    <div className="field-group">
                        <label htmlFor="invoiceAmount">Invoice Amount</label>
                        <input 
                            id="invoiceAmount"
                            type="number"
                            value={formData.invoiceAmount}
                            onChange={e => setFormData({...formData, invoiceAmount: e.target.value})}
                        />
                    </div>
                    <div className="field-group">
                        <label htmlFor="transportDocRef">Transport Document Ref</label>
                        <input 
                            id="transportDocRef"
                            value={formData.transportDocRef}
                            onChange={e => setFormData({...formData, transportDocRef: e.target.value})}
                            placeholder="e.g. BL-XYZ-123"
                        />
                    </div>
                </section>

                <section className="facility-status">
                    <div className="status-item">
                        <span className="stat-label">Available Facility</span>
                        <span className="stat-value text-success">$ 1,000,000</span>
                    </div>
                    <div className="status-item">
                        <span className="stat-label">Projected Utilization</span>
                        <span className="stat-value text-error">$ {earmarkAmount.toLocaleString()}</span>
                    </div>
                </section>

                <section className="vessel-narrative">
                    <div className="field-group">
                        <label htmlFor="carrierName">Carrier Name</label>
                        <input id="carrierName" value={formData.carrierName} onChange={e => setFormData({...formData, carrierName: e.target.value})} />
                    </div>
                    <div className="field-group">
                        <label htmlFor="vesselName">Vessel Name</label>
                        <input id="vesselName" value={formData.vesselName} onChange={e => setFormData({...formData, vesselName: e.target.value})} />
                    </div>
                </section>
            </main>

            <footer className="footer-actions">
                <button className="secondary-btn">Cancel</button>
                <button className="primary-btn">Submit SG Request</button>
            </footer>

            <style jsx>{`
                .sg-container { padding: 2rem; background: white; border-radius: 12px; }
                .header-flex { display: flex; justify-content: space-between; border-bottom: 1px solid #f1f5f9; padding-bottom: 2rem; margin-bottom: 2.5rem; }
                .title { font-size: 1.25rem; font-weight: 700; color: #1e293b; }
                .subtitle { font-size: 0.875rem; color: #64748b; }

                .limit-calc-box { background: #eff6ff; border: 1px solid #bfdbfe; padding: 1rem 1.5rem; border-radius: 8px; text-align: right; }
                .limit-calc-box .label { display: block; font-size: 0.75rem; font-weight: 700; color: #1d4ed8; }
                .limit-calc-box .value { font-size: 1.25rem; font-weight: 800; color: #1e40af; }

                .form-grid { display: grid; gap: 2.5rem; }
                .cargo-details, .vessel-narrative { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }
                .field-group { display: flex; flex-direction: column; gap: 0.5rem; }
                .field-group label { font-size: 0.875rem; font-weight: 600; color: #334155; }
                .field-group input { padding: 0.75rem; border: 1px solid #e2e8f0; border-radius: 6px; }

                .facility-status { display: flex; gap: 3rem; background: #f8fafc; padding: 1.5rem; border-radius: 10px; border: 1px solid #f1f5f9; }
                .status-item { display: flex; flex-direction: column; gap: 0.25rem; }
                .stat-label { font-size: 0.75rem; font-weight: 600; color: #64748b; }
                .stat-value { font-size: 1.125rem; font-weight: 700; }

                .footer-actions { display: flex; justify-content: flex-end; gap: 1rem; margin-top: 3rem; border-top: 1px solid #f1f5f9; padding-top: 2rem; }
                .primary-btn { background: #2563eb; color: white; border: none; padding: 0.75rem 2rem; border-radius: 6px; font-weight: 600; cursor: pointer; }
                .secondary-btn { background: #f8fafc; color: #475569; border: 1px solid #e2e8f0; padding: 0.75rem 2rem; border-radius: 6px; font-weight: 600; cursor: pointer; }
            `}</style>
        </div>
    );
};
