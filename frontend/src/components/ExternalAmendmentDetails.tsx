'use client';

import React from 'react';
import { Calendar, Hash, CreditCard, ClipboardList, Info, MessageSquare } from 'lucide-react';

// ABOUTME: Read-only detail view for an External Import LC Amendment (REQ-UI-IMP-03).
// ABOUTME: Focuses on SRG 2024 Smart Delta actions (Add/Replace/Delete) and Beneficiary Consent.

interface Props {
    amendment: any;
}

export const ExternalAmendmentDetails: React.FC<Props> = ({ amendment }) => {
    const DataItem = ({ label, value, icon }: { label: string; value: any; icon?: React.ReactNode }) => (
        <div className="detail-item">
            <div className="item-label">
                {icon}
                <span>{label}</span>
            </div>
            <div className="item-value">{value || '---'}</div>
        </div>
    );

    const DeltaSection = ({ title, action, delta, icon }: { title: string; action: string; delta: string; icon: React.ReactNode }) => (
        <section className="section-card premium-card">
            <div className="section-header">
                {icon}
                <h2>{title} {action && <span className="action-tag">{action}</span>}</h2>
            </div>
            <div className="narrative-content">
                {delta ? (
                    <p className="whitespace-pre-wrap text-slate-700">{delta}</p>
                ) : (
                    <p className="italic text-slate-400">No changes for this section.</p>
                )}
            </div>
        </section>
    );

    const totalAdjustment = (amendment.amountIncrease || 0) - (amendment.amountDecrease || 0);

    return (
        <div className="record-details-view">
            <header className="details-hero premium-card">
                <div className="hero-main">
                    <div className="title-group">
                        <span className="type-tag">External Amendment (MT 707)</span>
                        <h1>Amendment: {amendment.amendmentId}</h1>
                    </div>
                    <div className="hero-stats">
                        <div className="stat">
                            <label>Amendment Date</label>
                            <span>{new Date(amendment.amendmentDate).toLocaleDateString()}</span>
                        </div>
                        <div className="stat">
                            <label>Status</label>
                            <span className={`status-pill ${amendment.amendmentBusinessStateId}`}>
                                {amendment.amendmentBusinessStateId?.replace('AMEND_', '')}
                            </span>
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
                            value={totalAdjustment.toLocaleString(undefined, { minimumFractionDigits: 2, signDisplay: 'always' })} 
                            icon={<Hash size={14} />} 
                        />
                        <DataItem 
                            label="New Expiry Date" 
                            value={amendment.newExpiryDate} 
                            icon={<Calendar size={14} />} 
                        />
                        <div className="consent-box mt-4">
                             <div className="flex items-center gap-2 mb-2">
                                <ClipboardList size={14} className="text-blue-500" />
                                <label className="text-[10px] font-bold text-slate-400 uppercase">Beneficiary Consent</label>
                             </div>
                             <div className={`consent-status ${amendment.beneficiaryConsentStatusId}`}>
                                {amendment.beneficiaryConsentStatusId}
                             </div>
                        </div>
                    </div>
                </section>

                <section className="section-card premium-card">
                    <div className="section-header">
                        <Info size={16} />
                        <h2>Operational Flags</h2>
                    </div>
                    <div className="data-stack">
                        <DataItem label="Amendment No." value={amendment.amendmentNumber} icon={<Hash size={14} />} />
                        <DataItem label="Tolerance +" value={amendment.newTolerancePositive} icon={<Info size={14} />} />
                        <DataItem label="Tolerance -" value={amendment.newToleranceNegative} icon={<Info size={14} />} />
                    </div>
                </section>
            </div>

            <div className="narrative-grid">
                <DeltaSection 
                    title="Goods & Services" 
                    action={amendment.goodsActionEnumId} 
                    delta={amendment.goodsDeltaText} 
                    icon={<ClipboardList size={16} />} 
                />
                <DeltaSection 
                    title="Documents Required" 
                    action={amendment.docsActionEnumId} 
                    delta={amendment.docsDeltaText} 
                    icon={<ClipboardList size={16} />} 
                />
                <DeltaSection 
                    title="Additional Conditions" 
                    action={amendment.conditionsActionEnumId} 
                    delta={amendment.conditionsDeltaText} 
                    icon={<ClipboardList size={16} />} 
                />
            </div>

            {amendment.senderToReceiverInfo && (
                <section className="section-card premium-card mt-4">
                    <div className="section-header">
                        <MessageSquare size={16} />
                        <h2>Sender to Receiver Information (Tag 72Z)</h2>
                    </div>
                    <div className="narrative-content bg-slate-50 border-slate-200">
                        <p className="text-slate-600 font-mono text-sm">{amendment.senderToReceiverInfo}</p>
                    </div>
                </section>
            )}

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
                
                .status-pill { font-size: 0.75rem; padding: 4px 12px; border-radius: 999px; background: #f1f5f9; color: #64748b; font-weight: 700; }
                .status-pill.AMEND_APPROVED { background: #dcfce7; color: #15803d; }
                .status-pill.AMEND_COMMITTED { background: #eff6ff; color: #1d4ed8; }

                .details-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }
                .narrative-grid { display: grid; grid-template-columns: 1fr; gap: 1.5rem; }
                
                .section-card { background: white; padding: 1.5rem; border: 1px solid #e2e8f0; border-radius: 12px; }
                .section-header { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 1.5rem; padding-bottom: 1rem; border-bottom: 1px solid #f1f5f9; color: #64748b; }
                .section-header h2 { margin: 0; font-size: 0.875rem; font-weight: 800; text-transform: uppercase; letter-spacing: 0.05em; display: flex; align-items: center; gap: 8px; flex: 1; }
                
                .action-tag { font-size: 0.6rem; background: #334155; color: white; padding: 2px 6px; border-radius: 3px; }
                
                .data-stack { display: flex; flex-direction: column; gap: 1.25rem; }
                .detail-item { display: flex; flex-direction: column; gap: 0.375rem; }
                .item-label { display: flex; align-items: center; gap: 0.5rem; font-size: 0.75rem; font-weight: 700; color: #94a3b8; text-transform: uppercase; }
                .item-value { font-size: 0.9375rem; font-weight: 700; color: #1e293b; }

                .consent-status { padding: 8px 16px; border-radius: 6px; font-weight: 800; font-size: 1.1rem; text-transform: uppercase; letter-spacing: 0.05em; display: inline-block; }
                .consent-status.PENDING { background: #fef9c3; color: #854d0e; border: 1px solid #fde047; }
                .consent-status.ACCEPTED { background: #dcfce7; color: #166534; border: 1px solid #86efac; }
                .consent-status.REJECTED { background: #fee2e2; color: #991b1b; border: 1px solid #fecaca; }

                .narrative-content { background: #f8fafc; padding: 1.25rem; border-radius: 8px; border: 1px solid #f1f5f9; min-height: 100px; }
                .premium-card { box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
            `}</style>
        </div>
    );
};
