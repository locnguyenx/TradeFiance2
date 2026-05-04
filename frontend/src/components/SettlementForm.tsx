'use client';

import React, { useState, useEffect } from 'react';
import { tradeApi } from '../api/tradeApi';
import { TradeInstrument, ImportLetterOfCredit } from '../api/types';

// ABOUTME: SettlementForm implements LC drawing and final settlement.
// ABOUTME: Handles balance deduction and validates against effective outstanding amount.

interface SettlementFormProps {
    instrumentId: string;
    presentationId?: string;
    onSuccess?: () => void;
}

export const SettlementForm: React.FC<SettlementFormProps> = ({ instrumentId, presentationId, onSuccess }) => {
    const [lc, setLc] = useState<TradeInstrument & ImportLetterOfCredit | null>(null);
    const [settlementAmount, setSettlementAmount] = useState<number>(0);
    const [settlementDate, setSettlementDate] = useState<string>(new Date().toISOString().split('T')[0]);
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        loadLc();
    }, [instrumentId]);

    const loadLc = async () => {
        if (!instrumentId) {
            setLoading(false);
            setError('Please select an active Letter of Credit from the dashboard to initiate settlement.');
            return;
        }

        setLoading(true);
        setError(null);
        try {
            const data = await tradeApi.getImportLc(instrumentId);
            setLc(data);
        } catch (err: any) {
            setError(err.message || 'Failed to load LC details. Please verify the LC ID.');
        } finally {
            setLoading(false);
        }
    };

    const handleSettle = async () => {
        if (!lc) return;
        if (settlementAmount <= 0) {
            setError('Settlement amount must be greater than zero');
            return;
        }
        if (settlementAmount > (lc.effectiveOutstandingAmount || 0)) {
            setError('Settlement amount exceeds outstanding balance');
            return;
        }

        setSubmitting(true);
        try {
            await tradeApi.settleLcPresentation(instrumentId, presentationId || 'AUTO_PRES', {
                principalAmount: settlementAmount,
                settlementDate,
                currencyUomId: lc.currencyUomId
            });
            if (onSuccess) onSuccess();
        } catch (err) {
            setError('Settlement failed');
        } finally {
            setSubmitting(false);
        }
    };

    if (loading) return <div className="p-8 text-center premium-card">Loading LC details...</div>;
    if (!lc) {
        return (
            <div className="p-8 text-center premium-card">
                <div className="error-message mb-4">
                    {error || 'Letter of Credit not found or context missing.'}
                </div>
                <button 
                    className="secondary-btn" 
                    style={{ background: '#f8fafc', color: '#475569', border: '1px solid #e2e8f0', padding: '0.75rem 1.5rem', borderRadius: '6px', fontWeight: '600', cursor: 'pointer' }}
                    onClick={() => window.location.href = '/import-lc'}
                >
                    Back to Dashboard
                </button>
            </div>
        );
    }

    const remainingBalance = (lc.effectiveOutstandingAmount || 0) - settlementAmount;

    return (
        <div className="settlement-form premium-card">
            <header className="form-header">
                <h3>Settlement / Drawing</h3>
                <span className="ref-badge">{lc.instrumentId}</span>
            </header>

            <div className="lc-summary">
                <div className="summary-item">
                    <span className="label">Beneficiary</span>
                    <span className="value">{lc.beneficiaryPartyName}</span>
                </div>
                <div className="summary-item">
                    <span className="label">Current Outstanding</span>
                    <span className="value highlight">{lc.currencyUomId} {lc.effectiveOutstandingAmount?.toLocaleString()}</span>
                </div>
            </div>

            <div className="input-section">
                <div className="field">
                    <label htmlFor="settlementAmount" className="required-label">Drawing Amount</label>
                    <div className="amount-input">
                        <span className="currency">{lc.currencyUomId}</span>
                        <input 
                            id="settlementAmount"
                            type="number" 
                            value={settlementAmount} 
                            onChange={(e) => setSettlementAmount(parseFloat(e.target.value) || 0)} 
                            placeholder="0.00"
                        />
                    </div>
                </div>

                <div className="field">
                    <label htmlFor="settlementDate" className="required-label">Value Date</label>
                    <input 
                        id="settlementDate"
                        type="date" 
                        value={settlementDate} 
                        onChange={(e) => setSettlementDate(e.target.value)} 
                    />
                </div>
            </div>

            <div className="balance-projection">
                <div className="projection-row">
                    <span>Projected Outstanding Balance</span>
                    <span className={`projected-value ${remainingBalance < 0 ? 'negative' : ''}`}>
                        {lc.currencyUomId} {remainingBalance.toLocaleString()}
                    </span>
                </div>
                {remainingBalance < 0 && (
                    <div className="error-alert">ERROR: Drawing exceeds available balance</div>
                )}
            </div>

            {error && <div className="error-message">{error}</div>}

            <div className="form-actions">
                <button 
                    className="primary-btn" 
                    onClick={handleSettle}
                    disabled={submitting || remainingBalance < 0 || settlementAmount <= 0}
                >
                    {submitting ? 'Processing...' : 'Confirm Settlement'}
                </button>
            </div>

            <style jsx>{`
                .settlement-form {
                    padding: 2rem;
                    background: white;
                    border-radius: 12px;
                    border: 1px solid #e2e8f0;
                    display: flex;
                    flex-direction: column;
                    gap: 2rem;
                }

                .form-header { display: flex; justify-content: space-between; align-items: center; }
                .form-header h3 { margin: 0; color: #1e293b; }
                .ref-badge { background: #f1f5f9; padding: 0.25rem 0.5rem; border-radius: 4px; font-size: 0.75rem; font-weight: 700; color: #64748b; }

                .lc-summary { border-bottom: 2px solid #f1f5f9; padding-bottom: 1.5rem; display: flex; flex-direction: column; gap: 0.75rem; }
                .summary-item { display: flex; justify-content: space-between; }
                .summary-item .label { color: #64748b; font-size: 0.875rem; }
                .summary-item .value { font-weight: 600; color: #1e293b; }
                .summary-item .value.highlight { color: #2563eb; font-size: 1.125rem; font-weight: 800; }

                .input-section { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }
                .field { display: flex; flex-direction: column; gap: 0.5rem; }
                .field label { font-size: 0.875rem; font-weight: 600; color: #475569; }

                .amount-input { display: flex; border: 1px solid #e2e8f0; border-radius: 6px; overflow: hidden; }
                .amount-input .currency { background: #f8fafc; padding: 0 0.75rem; display: flex; align-items: center; color: #64748b; font-weight: 700; border-right: 1px solid #e2e8f0; }
                .amount-input input { border: none; padding: 0.625rem; flex: 1; outline: none; }
                
                input[type="date"] { border: 1px solid #e2e8f0; border-radius: 6px; padding: 0.625rem; outline: none; }

                .balance-projection { background: #f8fafc; padding: 1.25rem; border-radius: 8px; display: flex; flex-direction: column; gap: 0.5rem; }
                .projection-row { display: flex; justify-content: space-between; font-weight: 600; font-size: 0.875rem; color: #475569; }
                .projected-value.negative { color: #ef4444; }

                .error-alert { color: #ef4444; font-size: 0.75rem; font-weight: 700; margin-top: 0.25rem; }
                .error-message { color: #ef4444; font-size: 0.875rem; background: #fee2e2; padding: 0.75rem; border-radius: 6px; }

                .primary-btn {
                    background: #2563eb; color: white; border: none; padding: 0.75rem; border-radius: 6px; font-weight: 600; cursor: pointer; transition: background 0.2s;
                }
                .primary-btn:hover { background: #1d4ed8; }
                .primary-btn:disabled { background: #94a3b8; cursor: not-allowed; }
            `}</style>
        </div>
    );
};
