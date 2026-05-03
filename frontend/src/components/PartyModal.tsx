'use client';

import React, { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { tradeApi } from '../api/tradeApi';
import { TradeParty } from '../api/types';

// ABOUTME: PartyModal handles creation and updating of trade parties with bank extensions.
// ABOUTME: Supports both COMMERCIAL and BANK party types with conditional fields.

interface Props {
  party?: TradeParty | null;
  onClose: () => void;
  onSuccess: () => void;
}

export const PartyModal: React.FC<Props> = ({ party, onClose, onSuccess }) => {
  const isEdit = !!party;
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [mounted, setMounted] = useState(false);

  const [formData, setFormData] = useState<any>({
    partyId: '',
    partyName: '',
    partyTypeEnumId: 'PARTY_COMMERCIAL',
    registeredAddress: '',
    accountNumber: '',
    countryOfRisk: '',
    kycStatus: 'Active',
    sanctionsStatus: 'SANCTION_CLEAR',
    // Bank specific
    swiftBic: '',
    clearingCode: '',
    hasActiveRMA: false,
    nostroAccountRef: '',
    fiLimitAvailable: 0,
    fiLimitCurrencyUomId: 'USD'
  });

  useEffect(() => {
    setMounted(true);
    if (party) {
      setFormData({
        ...formData,
        ...party,
        hasActiveRMA: party.hasActiveRMA === 'Y' || party.hasActiveRMA === true
      });
    }
    
    // Only apply scroll lock after mounting to avoid hydration mismatch
    document.body.classList.add('scroll-lock');
    return () => {
      document.body.classList.remove('scroll-lock');
    };
  }, [party]);


  if (!mounted) return null;

  const validateSwiftX = (text: string) => {
    // SWIFT X Character Set: A-Z a-z 0-9 / - ? : ( ) . , ' + [space]
    const pattern = /^[A-Za-z0-9/ \-?:\().,'+\n\r]*$/;
    return pattern.test(text);
  };

  const validateBic = (bic: string) => {
    if (!bic) return true; // Optional field
    const pattern = /^[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?$/;
    return pattern.test(bic);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setFieldErrors({});

    const newFieldErrors: Record<string, string> = {};

    // Validate SWIFT X Charset
    if (!validateSwiftX(formData.partyName)) {
      newFieldErrors.partyName = 'Contains invalid characters for SWIFT (use only A-Z, 0-9, and standard punctuation)';
    }
    if (formData.registeredAddress && !validateSwiftX(formData.registeredAddress)) {
      newFieldErrors.registeredAddress = 'Contains invalid characters for SWIFT';
    }

    // Validate BIC format
    if (formData.partyTypeEnumId === 'PARTY_BANK' && formData.swiftBic) {
      if (!validateBic(formData.swiftBic)) {
        newFieldErrors.swiftBic = 'Invalid BIC format (must be 8 or 11 uppercase alphanumeric characters)';
      }
    }

    if (Object.keys(newFieldErrors).length > 0) {
      setFieldErrors(newFieldErrors);
      setLoading(false);
      return;
    }

    try {
      if (isEdit) {
        await tradeApi.updateParty(party.partyId, formData);
      } else {
        await tradeApi.createParty(formData);
      }
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to save party');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value, type } = e.target;
    const val = type === 'checkbox' ? (e.target as HTMLInputElement).checked : value;
    setFormData((prev: any) => ({ ...prev, [name]: val }));
  };

  const modalContent = (
    <div className="modal-portal-overlay" onClick={onClose}>
      <div className="modal-portal-content premium-card" onClick={e => e.stopPropagation()}>
        <header className="modal-header">
          <div className="header-prefix">
             <span className="status-dot"></span>
             <h2>{isEdit ? 'Edit Counterparty' : 'Register New Counterparty'}</h2>
          </div>
          <button className="close-btn" onClick={onClose}>✕</button>
        </header>

        <main className="modal-body">
          <form id="party-form" onSubmit={handleSubmit}>
            <div className="form-grid">
              <section className="form-section">
                <h3>Identity & Classification</h3>
                <div className="input-group">
                  <label>Party ID</label>
                  <input 
                    name="partyId" 
                    value={formData.partyId} 
                    onChange={handleChange} 
                    required 
                    disabled={isEdit}
                    placeholder="e.g. ACME_CORP_001"
                  />
                  {fieldErrors.partyId && <span className="field-error-text">{fieldErrors.partyId}</span>}
                </div>
                <div className="input-group">
                  <label>Full Legal Name</label>
                  <input 
                    name="partyName" 
                    value={formData.partyName} 
                    onChange={handleChange} 
                    required 
                    placeholder="Official registered name"
                    className={fieldErrors.partyName ? 'input-error' : ''}
                  />
                  {fieldErrors.partyName && <span className="field-error-text">{fieldErrors.partyName}</span>}
                </div>
                <div className="input-group">
                  <label>Party Type</label>
                  <select name="partyTypeEnumId" value={formData.partyTypeEnumId} onChange={handleChange} disabled={isEdit}>
                    <option value="PARTY_COMMERCIAL">Commercial Entity</option>
                    <option value="PARTY_BANK">Financial Institution (Bank)</option>
                  </select>
                </div>
              </section>

              <section className="form-section">
                <h3>Contact & Financials</h3>
                <div className="input-group">
                  <label>Registered Address</label>
                  <textarea 
                    name="registeredAddress" 
                    value={formData.registeredAddress} 
                    onChange={handleChange} 
                    rows={3}
                    placeholder="Street, City, Country"
                    className={fieldErrors.registeredAddress ? 'input-error' : ''}
                  />
                  {fieldErrors.registeredAddress && <span className="field-error-text">{fieldErrors.registeredAddress}</span>}
                </div>
                <div className="input-group">
                  <label>Default Account Number</label>
                  <input 
                    name="accountNumber" 
                    value={formData.accountNumber} 
                    onChange={handleChange} 
                    placeholder="IBAN or Account Ref"
                  />
                </div>
                <div className="input-group">
                  <label>Country of Risk</label>
                  <input 
                    name="countryOfRisk" 
                    value={formData.countryOfRisk} 
                    onChange={handleChange} 
                    placeholder="ISO Country Code"
                  />
                </div>
              </section>

              <section className="form-section full-width">
                <h3>Compliance Status</h3>
                <div className="status-inputs">
                  <div className="input-group">
                    <label>KYC Status</label>
                    <select name="kycStatus" value={formData.kycStatus} onChange={handleChange}>
                      <option value="Active">Active / Passed</option>
                      <option value="Pending">Pending Review</option>
                      <option value="Expired">Expired</option>
                      <option value="Suspended">Suspended</option>
                    </select>
                  </div>
                  <div className="input-group">
                    <label>Sanctions Status</label>
                    <select name="sanctionsStatus" value={formData.sanctionsStatus} onChange={handleChange}>
                      <option value="SANCTION_CLEAR">Clear / Clean</option>
                      <option value="SANCTION_PENDING">Check Pending</option>
                      <option value="SANCTION_BLOCKED">Blocked / Matched</option>
                    </select>
                  </div>
                </div>
              </section>

              {formData.partyTypeEnumId === 'PARTY_BANK' && (
                <section className="form-section full-width bank-section">
                  <h3>Bank Specific Details</h3>
                  <div className="bank-grid">
                    <div className="input-group">
                      <label>SWIFT BIC</label>
                      <input 
                        name="swiftBic" 
                        value={formData.swiftBic} 
                        onChange={handleChange} 
                        placeholder="8 or 11 chars"
                        className={fieldErrors.swiftBic ? 'input-error' : ''}
                      />
                      {fieldErrors.swiftBic && <span className="field-error-text">{fieldErrors.swiftBic}</span>}
                    </div>
                    <div className="input-group">
                      <label>Clearing Code</label>
                      <input name="clearingCode" value={formData.clearingCode} onChange={handleChange} />
                    </div>
                    <div className="input-group checkbox-group">
                      <label>
                        <input type="checkbox" name="hasActiveRMA" checked={formData.hasActiveRMA} onChange={handleChange} />
                        Active RMA Present
                      </label>
                    </div>
                    <div className="input-group">
                      <label>Nostro Account Reference</label>
                      <input name="nostroAccountRef" value={formData.nostroAccountRef} onChange={handleChange} />
                    </div>
                    <div className="input-group">
                      <label>FI Limit Available</label>
                      <input type="number" name="fiLimitAvailable" value={formData.fiLimitAvailable} onChange={handleChange} />
                    </div>
                  </div>
                </section>
              )}
            </div>

            {error && <div className="form-error">{error}</div>}
          </form>
        </main>

        <footer className="modal-footer">
          <button className="secondary-btn" onClick={onClose} disabled={loading}>Cancel</button>
          <button className="primary-btn" type="submit" form="party-form" disabled={loading}>
            {loading ? 'Saving...' : isEdit ? 'Update Profile' : 'Register Counterparty'}
          </button>
        </footer>

        <style jsx>{`
          .modal-header {
            padding: 1.5rem 2rem; border-bottom: 1px solid #f1f5f9;
            display: flex; justify-content: space-between; align-items: center;
          }

          .header-prefix { display: flex; align-items: center; gap: 0.75rem; }
          .status-dot { width: 8px; height: 8px; background: #2563eb; border-radius: 50%; }
          .modal-header h2 { font-size: 1.1rem; font-weight: 800; color: #0f172a; margin: 0; text-transform: uppercase; }
          .close-btn { background: #f1f5f9; border: none; width: 32px; height: 32px; border-radius: 50%; cursor: pointer; }
          
          .modal-body { flex: 1; overflow-y: auto; padding: 2rem; background: #f8fafc; }
          .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 2rem; }
          .form-section { display: flex; flex-direction: column; gap: 1rem; }
          .form-section.full-width { grid-column: span 2; }
          .form-section h3 { font-size: 0.75rem; font-weight: 700; color: #64748b; text-transform: uppercase; margin-bottom: 0.5rem; border-bottom: 1px solid #e2e8f0; padding-bottom: 0.5rem; }
          
          .bank-section { background: #eff6ff; padding: 1.5rem; border-radius: 12px; border: 1px solid #dbeafe; }
          .bank-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
          
          .input-group { display: flex; flex-direction: column; gap: 0.4rem; }
          .input-group label { font-size: 0.8rem; font-weight: 600; color: #475569; }
          .input-group input, .input-group select, .input-group textarea {
            padding: 0.6rem 0.8rem; border: 1px solid #e2e8f0; border-radius: 8px; font-size: 0.9rem;
            transition: all 0.2s;
          }
          .input-group input:focus { border-color: #2563eb; outline: none; box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1); }
          .checkbox-group { flex-direction: row; align-items: center; padding-top: 1.5rem; }
          .checkbox-group label { display: flex; align-items: center; gap: 0.5rem; cursor: pointer; }
          
          .status-inputs { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
          
          .field-error-text { font-size: 0.7rem; color: #dc2626; font-weight: 500; margin-top: 0.2rem; }
          .input-error { border-color: #fca5a5 !important; background-color: #fffafb; }

          .form-error { margin-top: 1.5rem; padding: 1rem; background: #fef2f2; border: 1px solid #fee2e2; color: #dc2626; border-radius: 8px; font-size: 0.875rem; font-weight: 500; }
          
          .modal-footer { padding: 1.5rem 2rem; border-top: 1px solid #f1f5f9; display: flex; justify-content: flex-end; gap: 1rem; background: white; }
          .primary-btn { padding: 0.75rem 1.5rem; background: #2563eb; color: white; border: none; border-radius: 8px; font-weight: 600; cursor: pointer; }
          .secondary-btn { padding: 0.75rem 1.5rem; background: white; color: #475569; border: 1px solid #e2e8f0; border-radius: 8px; font-weight: 600; cursor: pointer; }
          .primary-btn:hover { background: #1d4ed8; }
          .secondary-btn:hover { background: #f8fafc; }
        `}</style>
      </div>
    </div>
  );

  return createPortal(modalContent, document.body);
};
