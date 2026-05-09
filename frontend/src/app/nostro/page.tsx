'use client';

import React, { useState, useEffect } from 'react';
import { tradeApi } from '../../api/tradeApi';
import { NostroReconciliation } from '../../api/types';
import { useToast } from '../../context/ToastContext';

// ABOUTME: Nostro Reconciliation Management Screen (REQ-UI-IMP-05).
// ABOUTME: Enables manual matching of Nostro debit statements with MT 740/MT 747 expectations.

export default function NostroReconciliationScreen() {
    const { showToast } = useToast();
    const [reconciliations, setReconciliations] = useState<NostroReconciliation[]>([]);
    const [loading, setLoading] = useState(true);
    const [matchingId, setMatchingId] = useState<string | null>(null);
    const [matchData, setMatchData] = useState({
        nostroDebitDate: new Date().toISOString().split('T')[0],
        nostroDebitAmount: 0,
        nostroStatementRef: '',
        remarks: ''
    });

    const loadData = async () => {
        setLoading(true);
        try {
            const res = await tradeApi.getNostroReconciliations();
            setReconciliations(res.reconciliationList || []);
        } catch (e) {
            console.error("Failed to load reconciliations", e);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadData();
    }, []);

    const handleMatch = async () => {
        if (!matchingId) return;
        try {
            const res = await tradeApi.matchNostroReconciliation(matchingId, {
                ...matchData,
                matchStatusEnumId: 'RECON_MATCHED'
            });
            if (res.success || !res.error) {
                showToast('success', 'Nostro entry matched successfully');
                setMatchingId(null);
                loadData();
            }
        } catch (e) {
            showToast('error', 'Matching failed');
        }
    };

    if (loading) return <div className="p-8 text-center">Loading Nostro Records...</div>;

    return (
        <div className="p-8">
            <header className="flex justify-between items-center mb-8">
                <div>
                    <h1 className="text-2xl font-bold text-slate-800">Nostro Reconciliation</h1>
                    <p className="text-slate-500">Matching MT 740/747 expectations with actual bank statements.</p>
                </div>
            </header>

            <div className="premium-card">
                <table className="w-full text-sm text-left">
                    <thead className="bg-slate-50 text-slate-600 uppercase text-xs font-bold">
                        <tr>
                            <th className="p-4">Instrument ID</th>
                            <th className="p-4">Reimbursing Bank</th>
                            <th className="p-4">Expected Amt</th>
                            <th className="p-4">Status</th>
                            <th className="p-4">Debit Details</th>
                            <th className="p-4">Actions</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                        {reconciliations.map(rec => (
                            <tr key={rec.reconciliationId} className="hover:bg-slate-50 transition-colors">
                                <td className="p-4 font-mono font-bold text-blue-600">{rec.instrumentId}</td>
                                <td className="p-4">{rec.reimbursingBankPartyId}</td>
                                <td className="p-4 font-bold">{rec.expectedCurrency} {rec.expectedAmount.toLocaleString()}</td>
                                <td className="p-4">
                                    <span className={`px-2 py-1 rounded-full text-xs font-bold ${
                                        rec.matchStatusEnumId === 'RECON_MATCHED' ? 'bg-green-100 text-green-700' : 'bg-amber-100 text-amber-700'
                                    }`}>
                                        {rec.matchStatusEnumId.replace('RECON_', '')}
                                    </span>
                                </td>
                                <td className="p-4 text-slate-500">
                                    {rec.nostroDebitDate ? `${rec.nostroDebitDate} / ${rec.nostroDebitAmount}` : '---'}
                                </td>
                                <td className="p-4 text-right">
                                    {rec.matchStatusEnumId === 'RECON_PENDING' && (
                                        <button 
                                            className="px-3 py-1 bg-blue-600 text-white rounded font-bold text-xs hover:bg-blue-700"
                                            onClick={() => {
                                                setMatchingId(rec.reconciliationId);
                                                setMatchData({ ...matchData, nostroDebitAmount: rec.expectedAmount });
                                            }}
                                        >
                                            Manual Match
                                        </button>
                                    )}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {matchingId && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
                    <div className="bg-white rounded-xl p-6 w-full max-w-md shadow-2xl">
                        <h2 className="text-xl font-bold mb-4">Manual Reconciliation Match</h2>
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-bold mb-1">Debit Date</label>
                                <input type="date" className="w-full p-2 border rounded" 
                                    value={matchData.nostroDebitDate}
                                    onChange={e => setMatchData({...matchData, nostroDebitDate: e.target.value})} />
                            </div>
                            <div>
                                <label className="block text-sm font-bold mb-1">Debit Amount</label>
                                <input type="number" className="w-full p-2 border rounded" 
                                    value={matchData.nostroDebitAmount}
                                    onChange={e => setMatchData({...matchData, nostroDebitAmount: parseFloat(e.target.value)})} />
                            </div>
                            <div>
                                <label className="block text-sm font-bold mb-1">Statement Reference</label>
                                <input type="text" className="w-full p-2 border rounded" 
                                    placeholder="e.g., STMT/2026/001"
                                    value={matchData.nostroStatementRef}
                                    onChange={e => setMatchData({...matchData, nostroStatementRef: e.target.value})} />
                            </div>
                            <div>
                                <label className="block text-sm font-bold mb-1">Remarks</label>
                                <textarea className="w-full p-2 border rounded" rows={3}
                                    value={matchData.remarks}
                                    onChange={e => setMatchData({...matchData, remarks: e.target.value})} />
                            </div>
                        </div>
                        <div className="flex justify-end gap-3 mt-6">
                            <button className="px-4 py-2 text-slate-600 font-bold" onClick={() => setMatchingId(null)}>Cancel</button>
                            <button className="px-6 py-2 bg-blue-600 text-white rounded-lg font-bold" onClick={handleMatch}>Confirm Match</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
