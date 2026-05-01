'use client';

import React, { useEffect, useState } from 'react';
import { 
  Users, 
  CreditCard, 
  Truck, 
  Scale, 
  ShieldCheck, 
  Activity,
  Calendar,
  MapPin,
  ClipboardList,
  Info,
  ChevronRight,
  Printer,
  Edit
} from 'lucide-react';
import Link from 'next/link';
import { TradeInstrument, ImportLetterOfCredit } from '../api/types';
import './InstrumentDetails.css';

// ABOUTME: High-fidelity "Blue Premium" Instrument Details view.
// ABOUTME: Uses standard CSS import for robust layout and deterministic 2-column grid rows.

interface Props {
  instrument: TradeInstrument & ImportLetterOfCredit;
}

export const InstrumentDetails: React.FC<Props> = ({ instrument }) => {
  const [activeTab, setActiveTab] = useState('general');

  const sections = [
    { id: 'general', title: 'General Info', icon: <Activity size={16} /> },
    { id: 'parties', title: 'Parties', icon: <Users size={16} /> },
    { id: 'financials', title: 'Financials', icon: <CreditCard size={16} /> },
    { id: 'shipping', title: 'Shipment', icon: <Truck size={16} /> },
    { id: 'terms', title: 'Terms & Conditions', icon: <Scale size={16} /> },
    { id: 'charges', title: 'Margin/Fees', icon: <ShieldCheck size={16} /> }
  ];

  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) setActiveTab(entry.target.id);
        });
      },
      { threshold: 0.3, rootMargin: '-80px 0px -50% 0px' }
    );
    sections.forEach((s) => {
      const el = document.getElementById(s.id);
      if (el) observer.observe(el);
    });
    return () => observer.disconnect();
  }, []);

  const scrollToSection = (id: string) => {
    const el = document.getElementById(id);
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  const getPartyName = (role: string) => {
    const party = instrument.parties?.find(p => p.roleEnumId === role);
    return party?.partyName || party?.partyId || '';
  };

  const getBankBic = (role: string) => {
    const party = instrument.parties?.find(p => p.roleEnumId === role);
    return party?.swiftBic || '---';
  };

  const DataField = ({ label, value, highlight = false }: { label: string; value: any; highlight?: boolean }) => {
    let displayValue = value;
    
    // Standardize empty values
    if (value === null || value === undefined || value === '' || (typeof value === 'number' && isNaN(value))) {
      displayValue = '---';
    } else if (typeof value === 'number' && !label.toLowerCase().includes('days') && !label.toLowerCase().includes('percentage')) {
      displayValue = value.toLocaleString(undefined, { minimumFractionDigits: 2 });
    }

    return (
      <div className={`detail-row ${highlight ? 'highlight-row' : ''}`}>
        <div className="row-label">{label}</div>
        <div className="row-value">{displayValue}</div>
      </div>
    );
  };

  const SectionHeader = ({ id, title, icon }: { id: string; title: string; icon: React.ReactNode }) => (
    <div className="section-header" id={id}>
      <div className="header-icon">{icon}</div>
      <h3 className="header-title">{title}</h3>
      <div className="header-line"></div>
    </div>
  );

  return (
    <div className="audit-view">
      <div className="audit-layout">
        <div className="audit-content">
          <div className="content-stack">
            
            <section className="audit-section">
              <SectionHeader id="general" title="General Information" icon={<Activity size={20} />} />
              <div className="identity-hero">
                <div className="identity-main">
                  <div className="id-group">
                    <span className="id-label">INSTRUMENT REFERENCE</span>
                    <div className="id-value">{instrument.transactionRef}</div>
                  </div>
                  <div className="status-pill">{instrument.businessStateId?.replace('LC_', '') || 'DRAFT'}</div>
                </div>
                
                <div className="identity-meta">
                  <div className="meta-item">
                    <div className="meta-icon"><ClipboardList size={18} /></div>
                    <div className="meta-content">
                      <span className="meta-label">INSTRUMENT ID</span>
                      <div className="meta-value">{instrument.instrumentId}</div>
                    </div>
                  </div>
                  <div className="meta-item">
                    <div className="meta-icon"><Calendar size={18} /></div>
                    <div className="meta-content">
                      <span className="meta-label">ISSUE DATE</span>
                      <div className="meta-value">{instrument.issueDate}</div>
                    </div>
                  </div>
                  <div className="meta-item">
                    <div className="meta-icon"><Activity size={18} /></div>
                    <div className="meta-content">
                      <span className="meta-label">PRIORITY</span>
                      <div className="meta-value">{instrument.priorityEnumId}</div>
                    </div>
                  </div>
                </div>
              </div>
              <div className="data-table mt-4">
                <DataField label="Maker User ID" value={instrument.makerUserId} />
                <DataField label="Data Version" value={instrument.versionNumber ? `v${instrument.versionNumber}` : 'v1'} />
              </div>
            </section>

            <section className="audit-section">
              <SectionHeader id="parties" title="Parties & BICs" icon={<Users size={20} />} />
              <div className="data-table">
                <DataField label="Applicant (Obligor)" value={getPartyName('TP_APPLICANT') || instrument.applicantPartyName || instrument.applicantName || instrument.applicantPartyId} highlight />
                <DataField label="Beneficiary (Payee)" value={getPartyName('TP_BENEFICIARY') || instrument.beneficiaryPartyName || instrument.beneficiaryName || instrument.beneficiaryPartyId} highlight />
                <div className="row-divider">Banking Network Details</div>
                <DataField label="Issuing Bank BIC" value={getBankBic('TP_ISSUING_BANK')} />
                <DataField label="Advising Bank BIC" value={getBankBic('TP_ADVISING_BANK')} />
                <DataField label="Available with Bank" value={instrument.availableWithEnumId === 'AVAIL_ANY_BANK' ? 'ANY BANK' : getBankBic('TP_NEGOTIATING_BANK')} />
                <DataField label="Drawee Bank" value={getBankBic('TP_DRAWEE_BANK')} />
              </div>
            </section>

            <section className="audit-section">
              <SectionHeader id="financials" title="Financial Exposure" icon={<CreditCard size={20} />} />
              <div className="financial-hero">
                <div className="hero-top">TOTAL NOMINAL VALUE</div>
                <div className="hero-main">
                  <span className="hero-curr">{instrument.currencyUomId}</span>
                  <span className="hero-val">{(instrument.effectiveAmount || instrument.amount || 0).toLocaleString(undefined, {minimumFractionDigits: 2})}</span>
                </div>
                <div className="hero-footer">
                  <div className="stat">
                    <span>Outstanding Balance</span>
                    <strong>{instrument.currencyUomId} {instrument.effectiveOutstandingAmount?.toLocaleString() || '0.00'}</strong>
                  </div>
                  <div className="stat">
                    <span>Tolerance (Pos/Neg)</span>
                    <strong>{instrument.effectiveTolerancePositive || 0}% / {instrument.effectiveToleranceNegative || 0}%</strong>
                  </div>
                </div>
              </div>
              <div className="data-table mt-4">
                <DataField label="Cumulative Drawn" value={instrument.cumulativeDrawnAmount} />
                <DataField label="Current Exposure" value={instrument.effectiveAmount} highlight />
              </div>
            </section>

            <section className="audit-section">
              <SectionHeader id="shipping" title="Logistics & Timeline" icon={<Truck size={20} />} />
              <div className="data-table">
                <DataField label="Latest Shipment Date" value={instrument.latestShipmentDate} />
                <DataField label="Expiry Date" value={instrument.effectiveExpiryDate || instrument.expiryDate} highlight />
                <DataField label="Place of Expiry" value={instrument.expiryPlace} />
                <DataField label="Port of Loading" value={instrument.portOfLoading} />
                <DataField label="Port of Discharge" value={instrument.portOfDischarge} />
                <DataField label="Partial Shipment Policy" value={instrument.partialShipmentEnumId} />
                <DataField label="Transhipment Policy" value={instrument.transhipmentEnumId} />
              </div>
            </section>

            <section className="audit-section">
              <SectionHeader id="terms" title="Instrument Terms" icon={<Scale size={20} />} />
              <div className="data-table">
                <DataField label="Credit Category" value={instrument.lcTypeEnumId} />
                <DataField label="Payment Tenor (Days)" value={instrument.usanceDays} />
                <DataField label="Confirmation Instruction" value={instrument.confirmationEnumId} />
                <DataField label="Charges Rule" value={instrument.chargeAllocationEnumId} />
              </div>
              <div className="narrative-stack mt-6">
                {instrument.goodsDescription && (
                  <div className="narrative-box">
                    <header>Goods Description (Tag 45A)</header>
                    <p>{instrument.goodsDescription}</p>
                  </div>
                )}
                {instrument.documentsRequired && (
                  <div className="narrative-box">
                    <header>Documents Required (Tag 46A)</header>
                    <p>{instrument.documentsRequired}</p>
                  </div>
                )}
                {instrument.additionalConditions && (
                  <div className="narrative-box">
                    <header>Additional Conditions (Tag 47A)</header>
                    <p>{instrument.additionalConditions}</p>
                  </div>
                )}
              </div>
            </section>

            <section className="audit-section">
              <SectionHeader id="charges" title="Collateral Control" icon={<ShieldCheck size={20} />} />
              <div className="data-table">
                <DataField label="Margin Requirement Type" value={instrument.marginType} />
                <DataField label="Margin Percentage" value={instrument.marginPercentage ? `${instrument.marginPercentage}%` : null} />
                <DataField label="Collateral Debit Account" value={instrument.marginDebitAccount} highlight />
              </div>
            </section>

            <div className="bottom-spacer"></div>
          </div>
        </div>

        <aside className="audit-sidebar">
          <div className="sidebar-card nav-card">
            <header className="sidebar-label">NAVIGATION MAP</header>
            <nav className="sidebar-links">
              {sections.map(s => (
                <button 
                  key={s.id} 
                  className={`sidebar-link ${activeTab === s.id ? 'active' : ''}`}
                  onClick={() => scrollToSection(s.id)}
                >
                  <span className="icon">{activeTab === s.id ? <ChevronRight size={14} /> : s.icon}</span>
                  <span className="label">{s.title}</span>
                </button>
              ))}
            </nav>
          </div>

          <div className="sidebar-card action-card">
            <header className="sidebar-label">WORKSPACE ACTIONS</header>
            {instrument.businessStateId === 'LC_DRAFT' ? (
              <>
                <div className="action-help">This is a draft. You can continue editing before submission.</div>
                <Link href={`/issuance?id=${instrument.instrumentId}`}>
                  <button className="primary-btn pulse">
                    <Edit size={16} />
                    <span>Continue Editing Draft</span>
                  </button>
                </Link>
              </>
            ) : (
              <>
                <div className="action-help">Prepare documents for physical audit or compliance review.</div>
                <button className="primary-btn" onClick={() => window.print()}>
                  <Printer size={16} />
                  <span>Export Audit Document</span>
                </button>
              </>
            )}
          </div>
        </aside>
      </div>
    </div>
  );
};
