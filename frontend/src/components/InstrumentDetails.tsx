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
  Edit,
  Mail
} from 'lucide-react';
import Link from 'next/link';
import { TradeInstrument, ImportLetterOfCredit, TradeTransaction } from '../api/types';
import { SwiftMessageViewer } from './SwiftMessageViewer';
import './InstrumentDetails.css';

// ABOUTME: High-fidelity "Blue Premium" Instrument Details view.
// ABOUTME: Uses standard CSS import for robust layout and deterministic 2-column grid rows.

interface Props {
  instrument: TradeInstrument & ImportLetterOfCredit;
  transaction?: TradeTransaction;
}

export const InstrumentDetails: React.FC<Props> = ({ instrument, transaction }) => {
  const [activeTab, setActiveTab] = useState('general');

  const sections = [
    { id: 'general', title: 'General Info', icon: <Activity size={16} /> },
    { id: 'parties', title: 'Parties', icon: <Users size={16} /> },
    { id: 'financials', title: 'Financials', icon: <CreditCard size={16} /> },
    { id: 'shipping', title: 'Shipment', icon: <Truck size={16} /> },
    { id: 'terms', title: 'Terms & Conditions', icon: <Scale size={16} /> },
    { id: 'charges', title: 'Margin/Fees', icon: <ShieldCheck size={16} /> },
    { id: 'swift', title: 'SWIFT Messages', icon: <Mail size={16} /> }
  ];

  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) setActiveTab(entry.target.id);
        });
      },
      { threshold: 0.1, rootMargin: '-10% 0px -80% 0px' }
    );
    sections.forEach((s) => {
      const el = document.getElementById(s.id);
      if (el) observer.observe(el);
    });
    return () => observer.disconnect();
  }, []);

  const scrollToSection = (id: string) => {
    setActiveTab(id);
    const el = document.getElementById(id);
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  const getPartyName = (role: string) => {
    const party = instrument.parties?.find(p => p.roleEnumId === role);
    return party?.partyName || party?.partyId || '';
  };

  const getBankBic = (role: string) => {
    const party = instrument.parties?.find(p => p.roleEnumId === role);
    return party?.swiftBic || party?.partyName || party?.partyId || '---';
  };

  const formatDate = (dateValue: any) => {
    if (!dateValue || dateValue === '---') return '---';
    // If it's a number (timestamp), format it
    if (typeof dateValue === 'number') {
      if (dateValue > 10000000000) {
        return new Date(dateValue).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
      }
      return dateValue.toString();
    }
    // If it's a string, try to parse
    if (typeof dateValue === 'string') {
      // Check if it's a number string (timestamp as string)
      if (/^\d+$/.test(dateValue)) {
        const ts = parseInt(dateValue);
        if (ts > 10000000000) return new Date(ts).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
      }
      const d = new Date(dateValue);
      if (!isNaN(d.getTime()) && d.getFullYear() > 1900) {
        return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
      }
    }
    return String(dateValue);
  };

  const DataField = ({ label, value, highlight = false }: { label: string; value: any; highlight?: boolean }) => {
    let displayValue: React.ReactNode = '---';
    
    if (value === null || value === undefined || value === '' || (typeof value === 'number' && isNaN(value))) {
      displayValue = '---';
    } else if (label.toLowerCase().includes('date') || label.toLowerCase().includes('at') || label.toLowerCase().includes('timestamp')) {
      displayValue = formatDate(value);
    } else if (typeof value === 'number' && !label.toLowerCase().includes('days') && !label.toLowerCase().includes('percentage')) {
      displayValue = value.toLocaleString(undefined, { minimumFractionDigits: 2 });
    } else {
      displayValue = String(value);
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
                    <div className="id-value">{instrument.instrumentRef}</div>
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
                      <div className="meta-value">{formatDate(instrument.issueDate)}</div>
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
                <DataField label="Pre-Advice Ref (Tag 23)" value={instrument.preAdviceRef} />
                <DataField label="Data Version" value={instrument.versionNumber ? `v${instrument.versionNumber}` : 'v1'} />
              </div>
            </section>

            <section className="audit-section">
              <SectionHeader id="parties" title="Parties & BICs" icon={<Users size={20} />} />
              <div className="data-table">
                <DataField label="Applicant (Obligor)" value={getPartyName('TP_APPLICANT') || instrument.applicantPartyName || instrument.applicantName || instrument.applicantPartyId} highlight />
                <DataField label="Beneficiary (Payee)" value={getPartyName('TP_BENEFICIARY') || instrument.beneficiaryPartyName || instrument.beneficiaryName || instrument.beneficiaryPartyId} highlight />
                <div className="row-divider">Banking Network Details</div>
                <DataField label="Issuing Bank (Sender)" value={getBankBic('TP_ISSUING_BANK')} />
                <DataField label="Advising Bank (Receiver)" value={getBankBic('TP_ADVISING_BANK')} />
                <DataField label="Advise Through Bank (Tag 57A)" value={getBankBic('TP_ADVISE_THROUGH_BANK')} />
                <DataField label="Confirming Bank (Tag 58A)" value={getBankBic('TP_CONFIRMING_BANK')} />
                <div className="row-divider">Availability (Tag 41a)</div>
                <DataField 
                  label="Available with Bank" 
                  value={instrument.availableWithEnumId === 'AVAIL_ANY_BANK' ? 'ANY BANK' : (getBankBic('TP_NEGOTIATING_BANK') !== '---' ? getBankBic('TP_NEGOTIATING_BANK') : getBankBic('TP_ADVISING_BANK'))} 
                />
                <DataField label="Available By" value={instrument.availableByEnumId?.replace('BY_', '') || '---'} />
                <DataField label="Drawee Bank (Tag 42A)" value={getBankBic('TP_DRAWEE_BANK')} />
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
                <DataField label="Max Credit Amount (Tag 39B)" value={instrument.maxCreditAmountFlag === 'Y' ? 'NOT EXCEEDING' : 'FIXED'} />
                <DataField label="Additional Amounts (Tag 39C)" value={instrument.additionalAmountsText} />
                <DataField label="Cumulative Drawn" value={instrument.cumulativeDrawnAmount} />
                <DataField label="Current Exposure" value={instrument.effectiveAmount} highlight />
              </div>
            </section>

            <section className="audit-section">
              <SectionHeader id="shipping" title="Logistics & Timeline" icon={<Truck size={20} />} />
              <div className="data-table">
                <DataField label="Latest Shipment Date" value={formatDate(instrument.latestShipmentDate)} />
                <DataField label="Expiry Date" value={formatDate(instrument.effectiveExpiryDate || instrument.expiryDate)} highlight />
                <DataField label="Place of Expiry" value={instrument.expiryPlace} />
                <div className="row-divider">Route Details</div>
                <DataField label="Place of Receipt (Tag 44A)" value={instrument.receiptPlace} />
                <DataField label="Port of Loading (Tag 44E)" value={instrument.portOfLoading} />
                <DataField label="Port of Discharge (Tag 44F)" value={instrument.portOfDischarge} />
                <DataField label="Final Delivery (Tag 44B)" value={instrument.finalDeliveryPlace} />
                <DataField label="Partial Shipment (Tag 43P)" value={instrument.partialShipmentEnumId} />
                <DataField label="Transhipment (Tag 43T)" value={instrument.transhipmentEnumId} />
                <DataField label="Shipment Period (Tag 44D)" value={instrument.shipmentPeriodText} />
              </div>
            </section>

            <section className="audit-section">
              <SectionHeader id="terms" title="Instrument Terms" icon={<Scale size={20} />} />
              <div className="data-table">
                <DataField label="Credit Category (Tag 40A)" value={instrument.lcTypeEnumId} />
                <DataField label="Payment Tenor (Days)" value={instrument.usanceDays} />
                <DataField label="Usance Base Date (Tag 42C)" value={formatDate(instrument.usanceBaseDate)} />
                <DataField label="Confirmation (Tag 49)" value={instrument.confirmationEnumId} />
                <DataField label="Presentation Period (Tag 48)" value={instrument.presentationPeriodDays ? `${instrument.presentationPeriodDays} Days` : null} />
                <DataField label="Charges Rule (Tag 71D)" value={instrument.chargeAllocationEnumId} />
                <DataField label="Charges Narrative (Tag 71B)" value={instrument.chargeAllocationText} />
                <DataField label="Sender to Receiver (Tag 72Z)" value={instrument.bankToBankInstructions || instrument.senderToReceiverInfo} />
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
            <section className="audit-section">
              <SectionHeader id="swift" title="SWIFT Automation History" icon={<Mail size={20} />} />
              <SwiftMessageViewer instrumentId={instrument.instrumentId} />
            </section>

            <div className="bottom-spacer"></div>
          </div>
        </div>

        <aside className="audit-sidebar">
          <div className="sidebar-card action-card">
            <header className="sidebar-label">WORKSPACE ACTIONS</header>
            
            {transaction?.transactionStatusId === 'TX_DRAFT' && (
              <div className="mb-4">
                <div className="action-help">This transaction is in Draft.</div>
                {transaction.transactionTypeEnumId === 'IMP_NEW' && (
                  <Link href={`/issuance?id=${transaction.instrumentId}`}>
                    <button className="primary-btn pulse">
                      <Edit size={16} />
                      <span>Continue Editing Issuance</span>
                    </button>
                  </Link>
                )}
                {transaction.transactionTypeEnumId === 'IMP_AMENDMENT' && (
                  <Link href={`/import-lc/amendments?amendmentId=${transaction.relatedRecordId}`}>
                    <button className="primary-btn pulse">
                      <Edit size={16} />
                      <span>Continue Editing Amendment</span>
                    </button>
                  </Link>
                )}
                <hr className="my-4 border-slate-100" />
              </div>
            )}

            {instrument.businessStateId === 'LC_DRAFT' && !transaction ? (
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
        </aside>
      </div>
    </div>
  );
};
