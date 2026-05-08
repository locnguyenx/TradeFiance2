'use client';

import React from 'react';
import { Truck, Hash, Calendar, ShieldCheck } from 'lucide-react';

// ABOUTME: Read-only detail view for a Shipping Guarantee record.
// ABOUTME: Focuses on transport details and collateral backup.

interface Props {
    guarantee: any;
}

export const ShippingGuaranteeDetails: React.FC<Props> = ({ guarantee }) => {
    const DataItem = ({ label, value, icon }: { label: string; value: any; icon?: React.ReactNode }) => (
        <div className="detail-item">
            <div className="item-label">
                {icon}
                <span>{label}</span>
            </div>
            <div className="item-value">{value || '---'}</div>
        </div>
    );

    return (
        <div className="record-details-view">
            <header className="details-hero premium-card">
                <div className="hero-main">
                    <div className="title-group">
                        <span className="type-tag">Shipping Guarantee</span>
                        <h1>Guarantee: {guarantee.guaranteeId || guarantee.transactionId}</h1>
                    </div>
                    <div className="hero-stats">
                        <div className="stat">
                            <label>Request Date</label>
                            <span>{guarantee.transactionDate ? new Date(guarantee.transactionDate).toLocaleDateString() : '---'}</span>
                        </div>
                    </div>
                </div>
            </header>

            <div className="details-grid">
                <section className="section-card premium-card">
                    <div className="section-header">
                        <Truck size={16} />
                        <h2>Transport & Consignment</h2>
                    </div>
                    <div className="data-stack">
                        <DataItem label="Vessel / Carrier" value={guarantee.vesselName} icon={<Truck size={14} />} />
                        <DataItem label="B/L Number" value={guarantee.billOfLadingNo} icon={<Hash size={14} />} />
                        <DataItem label="Port of Discharge" value={guarantee.portOfDischarge} icon={<ShieldCheck size={14} />} />
                    </div>
                </section>

                <section className="section-card premium-card">
                    <div className="section-header">
                        <ShieldCheck size={16} />
                        <h2>Guarantee Value</h2>
                    </div>
                    <div className="data-stack">
                        <DataItem label="Invoice Amount" value={guarantee.invoiceAmount?.toLocaleString()} icon={<Hash size={14} />} />
                        <DataItem label="Expiry Date" value={guarantee.expiryDate} icon={<Calendar size={14} />} />
                    </div>
                </section>
            </div>

            <style jsx>{`
                .record-details-view { display: flex; flex-direction: column; gap: 1.5rem; }
                .details-hero { background: white; padding: 2rem; border: 1px solid #e2e8f0; border-radius: 12px; }
                .hero-main { display: flex; justify-content: space-between; align-items: flex-start; }
                .type-tag { font-size: 0.65rem; font-weight: 800; color: #7c3aed; text-transform: uppercase; letter-spacing: 0.05em; background: #f5f3ff; padding: 0.25rem 0.625rem; border-radius: 4px; display: inline-block; margin-bottom: 0.75rem; }
                h1 { margin: 0; font-size: 1.75rem; font-weight: 800; color: #0f172a; letter-spacing: -0.025em; }
                
                .hero-stats { display: flex; gap: 3rem; }
                .stat { display: flex; flex-direction: column; gap: 0.25rem; }
                .stat label { font-size: 0.7rem; font-weight: 700; color: #94a3b8; text-transform: uppercase; }
                .stat span { font-size: 1.125rem; font-weight: 700; color: #1e293b; }

                .details-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }
                .section-card { background: white; padding: 1.5rem; border: 1px solid #e2e8f0; border-radius: 12px; }
                .section-header { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 1.5rem; padding-bottom: 1rem; border-bottom: 1px solid #f1f5f9; color: #64748b; }
                .section-header h2 { margin: 0; font-size: 0.875rem; font-weight: 800; text-transform: uppercase; letter-spacing: 0.05em; }
                
                .data-stack { display: flex; flex-direction: column; gap: 1.25rem; }
                .detail-item { display: flex; flex-direction: column; gap: 0.375rem; }
                .item-label { display: flex; align-items: center; gap: 0.5rem; font-size: 0.75rem; font-weight: 700; color: #94a3b8; text-transform: uppercase; }
                .item-value { font-size: 0.9375rem; font-weight: 700; color: #1e293b; }
                .premium-card { box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
            `}</style>
        </div>
    );
};
