'use client';

import React from 'react';
import { Calendar, Hash, CreditCard, Landmark, CheckCircle } from 'lucide-react';

// ABOUTME: Read-only detail view for an Import LC Settlement record.
// ABOUTME: Focuses on remittance accuracy, FX rates, and debit account verification.

interface Props {
    settlement: any; // ImportLcSettlement
}

export const SettlementDetails: React.FC<Props> = ({ settlement }) => {
    const DataItem = ({ label, value, icon, variant = 'default' }: { label: string; value: any; icon?: React.ReactNode, variant?: 'default' | 'success' | 'info' }) => (
        <div className={`detail-item variant-${variant}`}>
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
                        <span className="type-tag">LC Settlement Record</span>
                        <h1>Settlement: {settlement.settlementId}</h1>
                    </div>
                    <div className="hero-stats">
                        <div className="stat">
                            <label>Value Date</label>
                            <span>{settlement.valueDate}</span>
                        </div>
                        <div className="stat">
                            <label>Status</label>
                            <span className="status-badge">{settlement.settlementStatusId?.replace('SETTLE_', '') || 'PENDING'}</span>
                        </div>
                    </div>
                </div>
            </header>

            <div className="details-grid">
                <section className="section-card premium-card">
                    <div className="section-header">
                        <CreditCard size={16} />
                        <h2>Financial Remittance</h2>
                    </div>
                    <div className="data-stack">
                        <DataItem 
                            label="Principal Amount" 
                            value={`${settlement.remittanceCurrency || 'USD'} ${settlement.principalAmount?.toLocaleString(undefined, { minimumFractionDigits: 2 })}`} 
                            icon={<Hash size={14} />} 
                        />
                        <DataItem 
                            label="FX Rate" 
                            value={settlement.fxRate?.toString()} 
                            icon={<Landmark size={14} />} 
                        />
                        <DataItem 
                            label="Local Equivalent" 
                            value={`VND ${settlement.localEquivalent?.toLocaleString()}`} 
                            icon={<Hash size={14} />} 
                        />
                        <DataItem 
                            label="Net Debit Amount" 
                            value={`${settlement.remittanceCurrency || 'USD'} ${settlement.netDebitAmount?.toLocaleString()}`} 
                            icon={<CheckCircle size={14} />} 
                            variant="success"
                        />
                    </div>
                </section>

                <section className="section-card premium-card">
                    <div className="section-header">
                        <Landmark size={16} />
                        <h2>Accounting Details</h2>
                    </div>
                    <div className="data-stack">
                        <DataItem 
                            label="Debit Account" 
                            value={settlement.debitAccountId} 
                            icon={<Hash size={14} />} 
                        />
                        <DataItem 
                            label="Charges Detail" 
                            value={settlement.chargesDetailEnumId} 
                            icon={<ClipboardList size={14} />} 
                        />
                        <DataItem 
                            label="Maturity Date" 
                            value={settlement.maturityDate} 
                            icon={<Calendar size={14} />} 
                        />
                    </div>
                </section>
            </div>

            <style jsx>{`
                .record-details-view { display: flex; flex-direction: column; gap: 1.5rem; }
                
                .details-hero { background: white; padding: 2rem; border: 1px solid #e2e8f0; border-radius: 12px; }
                .hero-main { display: flex; justify-content: space-between; align-items: flex-start; }
                .type-tag { font-size: 0.65rem; font-weight: 800; color: #166534; text-transform: uppercase; letter-spacing: 0.05em; background: #dcfce7; padding: 0.25rem 0.625rem; border-radius: 4px; display: inline-block; margin-bottom: 0.75rem; }
                h1 { margin: 0; font-size: 1.75rem; font-weight: 800; color: #0f172a; letter-spacing: -0.025em; }
                
                .hero-stats { display: flex; gap: 3rem; }
                .stat { display: flex; flex-direction: column; gap: 0.25rem; }
                .stat label { font-size: 0.7rem; font-weight: 700; color: #94a3b8; text-transform: uppercase; }
                .stat span { font-size: 1.125rem; font-weight: 700; color: #1e293b; }

                .status-badge { background: #fffbeb; color: #92400e; padding: 0.25rem 0.75rem; border-radius: 999px; font-size: 0.7rem; font-weight: 800; text-transform: uppercase; border: 1px solid #fde68a; }

                .details-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }
                .section-card { background: white; padding: 1.5rem; border: 1px solid #e2e8f0; border-radius: 12px; }
                .section-header { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 1.5rem; padding-bottom: 1rem; border-bottom: 1px solid #f1f5f9; color: #64748b; }
                .section-header h2 { margin: 0; font-size: 0.875rem; font-weight: 800; text-transform: uppercase; letter-spacing: 0.05em; }
                
                .data-stack { display: flex; flex-direction: column; gap: 1.25rem; }
                .detail-item { display: flex; flex-direction: column; gap: 0.375rem; }
                .item-label { display: flex; align-items: center; gap: 0.5rem; font-size: 0.75rem; font-weight: 700; color: #94a3b8; text-transform: uppercase; }
                .item-value { font-size: 0.9375rem; font-weight: 700; color: #1e293b; }

                .variant-success .item-value { color: #059669; }
                .premium-card { box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
            `}</style>
        </div>
    );
};

const ClipboardList = ({ size }: { size: number }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="8" y="2" width="8" height="4" rx="1" ry="1"></rect><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"></path><path d="M12 11h4"></path><path d="M12 16h4"></path><path d="M8 11h.01"></path><path d="M8 16h.01"></path></svg>
);
