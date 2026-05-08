'use client';

import React from 'react';
import { ImportLcAmendment } from '../api/types';
import { Calendar, Hash, CreditCard, ClipboardList } from 'lucide-react';

// ABOUTME: Read-only detail view for an Import LC Amendment record.
// ABOUTME: Focuses on delta changes (Financial/Narrative) and beneficiary status.

interface Props {
    amendment: ImportLcAmendment & { amendmentNarrative?: string; isBeneficiaryAcceptanceRequired?: string };
}

export const AmendmentDetails: React.FC<Props> = ({ amendment }) => {
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
                        <span className="type-tag">LC Amendment Record</span>
                        <h1>Amendment: {amendment.amendmentId}</h1>
                    </div>
                    <div className="hero-stats">
                        <div className="stat">
                            <label>Amendment Date</label>
                            <span>{new Date(amendment.amendmentDate).toLocaleDateString()}</span>
                        </div>
                    </div>
                </div>
            </header>

            <div className="details-grid">
                <section className="section-card premium-card">
                    <div className="section-header">
                        <CreditCard size={16} />
                        <h2>Financial Modifications</h2>
                    </div>
                    <div className="data-stack">
                        <DataItem 
                            label="Amount Adjustment" 
                            value={amendment.amount ? amendment.amount.toLocaleString(undefined, { minimumFractionDigits: 2, signDisplay: 'always' }) : '0.00'} 
                            icon={<Hash size={14} />} 
                        />
                        <DataItem 
                            label="New Expiry Date" 
                            value={amendment.expiryDate} 
                            icon={<Calendar size={14} />} 
                        />
                        <DataItem 
                            label="Beneficiary Consent" 
                            value={amendment.isBeneficiaryAcceptanceRequired === 'Y' ? 'Required' : 'Not Required'} 
                            icon={<ClipboardList size={14} />} 
                        />
                    </div>
                </section>

                <section className="section-card premium-card">
                    <div className="section-header">
                        <ClipboardList size={16} />
                        <h2>Narrative & Terms (Tag 77A)</h2>
                    </div>
                    <div className="narrative-content">
                        {amendment.amendmentNarrative ? (
                            <p className="whitespace-pre-wrap text-slate-700">{amendment.amendmentNarrative}</p>
                        ) : (
                            <p className="italic text-slate-400">No narrative changes provided for this amendment.</p>
                        )}
                    </div>
                </section>
            </div>

            <style jsx>{`
                .record-details-view { display: flex; flex-direction: column; gap: 1.5rem; }
                
                .details-hero { background: white; padding: 2rem; border: 1px solid #e2e8f0; border-radius: 12px; }
                .hero-main { display: flex; justify-content: space-between; align-items: flex-start; }
                .type-tag { font-size: 0.65rem; font-weight: 800; color: #2563eb; text-transform: uppercase; letter-spacing: 0.05em; background: #eff6ff; padding: 0.25rem 0.625rem; border-radius: 4px; display: inline-block; margin-bottom: 0.75rem; }
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

                .narrative-content { background: #f8fafc; padding: 1.25rem; border-radius: 8px; border: 1px solid #f1f5f9; min-height: 150px; }
                .premium-card { box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
            `}</style>
        </div>
    );
};
