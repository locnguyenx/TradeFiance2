'use client';

import React from 'react';
import { Database, CreditCard, User, BarChart, FileText } from 'lucide-react';

// ABOUTME: Read-only detail view for an Internal Bank Amendment.
// ABOUTME: Focuses on operational adjustments (Facility, Fees, Margin) that don't impact the Beneficiary.

interface Props {
    amendment: any;
}

export const InternalAmendmentDetails: React.FC<Props> = ({ amendment }) => {
    const DataItem = ({ label, value, icon, subValue }: { label: string; value: any; icon?: React.ReactNode; subValue?: string }) => (
        <div className="detail-item">
            <div className="item-label">
                {icon}
                <span>{label}</span>
            </div>
            <div className="value-group">
                <div className="item-value">{value || '---'}</div>
                {subValue && <div className="item-subvalue">{subValue}</div>}
            </div>
        </div>
    );

    return (
        <div className="record-details-view">
            <header className="details-hero premium-card">
                <div className="hero-main">
                    <div className="title-group">
                        <span className="type-tag">Internal Bank Amendment</span>
                        <h1>Internal Ref: {amendment.internalAmendmentId}</h1>
                    </div>
                    <div className="hero-stats">
                        <div className="stat">
                            <label>Adjustment Date</label>
                            <span>{new Date(amendment.amendmentDate).toLocaleDateString()}</span>
                        </div>
                        <div className="stat">
                            <label>Transaction ID</label>
                            <span>{amendment.transactionRef || '---'}</span>
                        </div>
                    </div>
                </div>
            </header>

            <div className="details-grid">
                <section className="section-card premium-card">
                    <div className="section-header">
                        <BarChart size={16} />
                        <h2>Credit & Facility</h2>
                    </div>
                    <div className="data-stack">
                        <DataItem 
                            label="New Facility Assignment" 
                            value={amendment.newFacilityId} 
                            icon={<Database size={14} />} 
                        />
                        <DataItem 
                            label="New Margin Percentage" 
                            value={amendment.newMarginPercentage ? `${amendment.newMarginPercentage}%` : null} 
                            icon={<BarChart size={14} />} 
                        />
                    </div>
                </section>

                <section className="section-card premium-card">
                    <div className="section-header">
                        <CreditCard size={16} />
                        <h2>Accounting & Billing</h2>
                    </div>
                    <div className="data-stack">
                        <DataItem 
                            label="New Fee Debit Account" 
                            value={amendment.newFeeDebitAccountId} 
                            icon={<CreditCard size={14} />} 
                        />
                        <DataItem 
                            label="New Margin Account" 
                            value={amendment.newMarginAccountId} 
                            icon={<CreditCard size={14} />} 
                        />
                    </div>
                </section>

                <section className="section-card premium-card">
                    <div className="section-header">
                        <User size={16} />
                        <h2>Relationship Management</h2>
                    </div>
                    <div className="data-stack">
                        <DataItem 
                            label="Assigned RM" 
                            value={amendment.newRelationshipManagerId} 
                            icon={<User size={14} />} 
                        />
                    </div>
                </section>

                <section className="section-card premium-card">
                    <div className="section-header">
                        <FileText size={16} />
                        <h2>Audit Info</h2>
                    </div>
                    <div className="data-stack">
                        <DataItem label="Instrument ID" value={amendment.instrumentId} icon={<FileText size={14} />} />
                    </div>
                </section>
            </div>

            <style jsx>{`
                .record-details-view { display: flex; flex-direction: column; gap: 1.5rem; }
                .details-hero { background: white; padding: 2rem; border: 1px solid #e2e8f0; border-radius: 12px; }
                .hero-main { display: flex; justify-content: space-between; align-items: flex-start; }
                .type-tag { font-size: 0.65rem; font-weight: 800; color: #6366f1; text-transform: uppercase; letter-spacing: 0.05em; background: #eef2ff; padding: 0.25rem 0.625rem; border-radius: 4px; display: inline-block; margin-bottom: 0.75rem; }
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
                .item-subvalue { font-size: 0.7rem; color: #64748b; }

                .premium-card { box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
            `}</style>
        </div>
    );
};
