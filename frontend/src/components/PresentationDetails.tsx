'use client';

import React from 'react';
import { Calendar, Hash, FileText, AlertTriangle, ShieldCheck } from 'lucide-react';

// ABOUTME: Read-only detail view for a Trade Document Presentation record.
// ABOUTME: Emphasizes claim accuracy and discrepancy status for checker review.

interface Props {
    presentation: any; // TradeDocumentPresentation
}

export const PresentationDetails: React.FC<Props> = ({ presentation }) => {
    const DataItem = ({ label, value, icon, variant = 'default' }: { label: string; value: any; icon?: React.ReactNode, variant?: 'default' | 'urgent' | 'success' }) => (
        <div className={`detail-item variant-${variant}`}>
            <div className="item-label">
                {icon}
                <span>{label}</span>
            </div>
            <div className="item-value">{value || '---'}</div>
        </div>
    );

    const isDiscrepant = presentation.isDiscrepant === 'Y';

    return (
        <div className="record-details-view">
            <header className="details-hero premium-card">
                <div className="hero-main">
                    <div className="title-group">
                        <span className="type-tag">Document Presentation</span>
                        <h1>Presentation: {presentation.presentationId}</h1>
                    </div>
                    <div className="hero-stats">
                        <div className="stat">
                            <label>Presentation Date</label>
                            <span>{presentation.presentationDate}</span>
                        </div>
                        <div className="stat">
                            <label>Discrepancy Status</label>
                            <span className={isDiscrepant ? 'text-urgent' : 'text-success'}>
                                {isDiscrepant ? 'DISCREPANT' : 'CLEAN'}
                            </span>
                        </div>
                    </div>
                </div>
            </header>

            <div className="details-grid">
                <section className="section-card premium-card">
                    <div className="section-header">
                        <FileText size={16} />
                        <h2>Claim Details</h2>
                    </div>
                    <div className="data-stack">
                        <DataItem 
                            label="Claim Amount" 
                            value={`${presentation.claimCurrency || 'USD'} ${presentation.claimAmount?.toLocaleString(undefined, { minimumFractionDigits: 2 })}`} 
                            icon={<Hash size={14} />} 
                        />
                        <DataItem 
                            label="Regulatory Deadline" 
                            value={presentation.regulatoryDeadline} 
                            icon={<Calendar size={14} />} 
                        />
                        <DataItem 
                            label="Presenting Bank Ref" 
                            value={presentation.presentingBankRef} 
                            icon={<ShieldCheck size={14} />} 
                        />
                        <DataItem 
                            label="Decision Status" 
                            value={presentation.applicantDecisionEnumId || 'Awaiting Decision'} 
                            variant={presentation.applicantDecisionEnumId === 'WAIVED' ? 'success' : presentation.applicantDecisionEnumId === 'REFUSED' ? 'urgent' : 'default'}
                        />
                    </div>
                </section>

                <section className="section-card premium-card">
                    <div className="section-header">
                        <AlertTriangle size={16} />
                        <h2>Discrepancies & Remarks</h2>
                    </div>
                    <div className="narrative-content">
                        {presentation.senderToReceiverPresentationInfo ? (
                            <div className="tag-box">
                                <label>Sender to Receiver Info (Tag 72Z)</label>
                                <p>{presentation.senderToReceiverPresentationInfo}</p>
                            </div>
                        ) : (
                            <p className="italic text-slate-400">No additional presentation remarks provided.</p>
                        )}
                        
                        {presentation.chargesDeducted && (
                            <div className="tag-box mt-4">
                                <label>Charges Deducted (Tag 73)</label>
                                <p>{presentation.chargesDeducted}</p>
                            </div>
                        )}
                    </div>
                </section>
            </div>

            <style jsx>{`
                .record-details-view { display: flex; flex-direction: column; gap: 1.5rem; }
                
                .details-hero { background: white; padding: 2rem; border: 1px solid #e2e8f0; border-radius: 12px; }
                .hero-main { display: flex; justify-content: space-between; align-items: flex-start; }
                .type-tag { font-size: 0.65rem; font-weight: 800; color: #0891b2; text-transform: uppercase; letter-spacing: 0.05em; background: #ecfeff; padding: 0.25rem 0.625rem; border-radius: 4px; display: inline-block; margin-bottom: 0.75rem; }
                h1 { margin: 0; font-size: 1.75rem; font-weight: 800; color: #0f172a; letter-spacing: -0.025em; }
                
                .hero-stats { display: flex; gap: 3rem; }
                .stat { display: flex; flex-direction: column; gap: 0.25rem; }
                .stat label { font-size: 0.7rem; font-weight: 700; color: #94a3b8; text-transform: uppercase; }
                .stat span { font-size: 1.125rem; font-weight: 700; color: #1e293b; }

                .text-urgent { color: #dc2626; }
                .text-success { color: #166534; }

                .details-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }
                .section-card { background: white; padding: 1.5rem; border: 1px solid #e2e8f0; border-radius: 12px; }
                .section-header { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 1.5rem; padding-bottom: 1rem; border-bottom: 1px solid #f1f5f9; color: #64748b; }
                .section-header h2 { margin: 0; font-size: 0.875rem; font-weight: 800; text-transform: uppercase; letter-spacing: 0.05em; }
                
                .data-stack { display: flex; flex-direction: column; gap: 1.25rem; }
                .detail-item { display: flex; flex-direction: column; gap: 0.375rem; }
                .item-label { display: flex; align-items: center; gap: 0.5rem; font-size: 0.75rem; font-weight: 700; color: #94a3b8; text-transform: uppercase; }
                .item-value { font-size: 0.9375rem; font-weight: 700; color: #1e293b; }

                .variant-success .item-value { color: #059669; }
                .variant-urgent .item-value { color: #dc2626; }

                .narrative-content { background: #f8fafc; padding: 1.25rem; border-radius: 8px; border: 1px solid #f1f5f9; min-height: 150px; }
                .tag-box label { display: block; font-size: 0.65rem; font-weight: 800; color: #64748b; text-transform: uppercase; margin-bottom: 0.5rem; }
                .tag-box p { font-size: 0.875rem; color: #334155; margin: 0; line-height: 1.5; }
                
                .mt-4 { margin-top: 1rem; }
                .premium-card { box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
            `}</style>
        </div>
    );
};
