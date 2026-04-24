'use client';

import React, { useState, useEffect } from 'react';
import { tradeApi } from '../api/tradeApi';

// ABOUTME: High-Density Document Examination Workspace (REQ-UI-IMP-04).
// ABOUTME: Implements Split-Pane for comparing LC Terms vs presented Documents.

interface DocRow {
    id: string;
    name: string;
    originals: number;
    copies: number;
}

interface Discrepancy {
    id: string;
    code: string;
    description: string;
    isWaived: boolean;
}

export const DocumentExamination: React.FC = () => {
    const [decision, setDecision] = useState<'Accept' | 'Discrepant' | null>(null);
    const [docs, setDocs] = useState<DocRow[]>([
        { id: '1', name: 'Commercial Invoice', originals: 3, copies: 3 },
        { id: '2', name: 'Ocean Bill of Lading', originals: 3, copies: 0 },
        { id: '3', name: 'Packing List', originals: 1, copies: 2 },
    ]);
    const [discrepancies, setDiscrepancies] = useState<Discrepancy[]>([]);
    const [selectedCode, setSelectedCode] = useState('');
    const [detail, setDetail] = useState('');

    const logDiscrepancy = () => {
        if (!selectedCode) return;
        const newDisc: Discrepancy = {
            id: Math.random().toString(36).substr(2, 9),
            code: selectedCode,
            description: detail,
            isWaived: false
        };
        setDiscrepancies([...discrepancies, newDisc]);
        setDecision('Discrepant');
        setSelectedCode('');
        setDetail('');
    };

    const handleWaive = async (id: string) => {
        // In real app, we'd have presentationId
        const res = await tradeApi.waiveDiscrepancy('TEST_PRES_ID');
        if (res.success || !res.error) {
            setDiscrepancies(discrepancies.map(d => d.id === id ? { ...d, isWaived: true } : d));
        }
    };

    return (
        <div className="exam-workspace">
            <aside className="left-pane-lc-context">
                <header className="pane-header">
                    <h3>LC Terms (Reference)</h3>
                </header>
                
                <div className="accordion-ctx">
                    <details open>
                        <summary>Financials & Dates</summary>
                        <div className="ctx-content">
                            <p><strong>Amount:</strong> USD 500,000.00</p>
                            <p><strong>Expiry:</strong> 2027-01-15</p>
                            <p><strong>Tolerance:</strong> +/- 5%</p>
                        </div>
                    </details>
                    <details>
                        <summary>Documents Required</summary>
                        <div className="ctx-content">
                            <p>1. Commercial Invoice in 3 originals and 3 copies.</p>
                            <p>2. Full set of on-board Bill of Lading.</p>
                        </div>
                    </details>
                    <details>
                        <summary>Additional Conditions</summary>
                        <div className="ctx-content">
                            <p>All bank charges for beneficiary account.</p>
                        </div>
                    </details>
                </div>
            </aside>

            <main className="right-pane-entry">
                <section className="doc-matrix">
                    <header className="pane-header">
                        <h3>Presentation Matrix</h3>
                    </header>
                    <table className="matrix-table">
                        <thead>
                            <tr>
                                <th>Document Name</th>
                                <th>Originals</th>
                                <th>Copies</th>
                            </tr>
                        </thead>
                        <tbody>
                            {docs.map(doc => (
                                <tr key={doc.id}>
                                    <td>{doc.name}</td>
                                    <td><input type="number" defaultValue={doc.originals} /></td>
                                    <td><input type="number" defaultValue={doc.copies} /></td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </section>

                <section className="discrepancy-logger">
                    <header className="pane-header">
                        <h3>Discrepancy Logger (ISBP-745)</h3>
                    </header>
                    <div className="logger-entry">
                        <select 
                            className="isbp-select" 
                            value={selectedCode}
                            onChange={(e) => setSelectedCode(e.target.value)}
                        >
                            <option value="">Select ISBP Code...</option>
                            <option value="LATE_SHIPMENT">Late Shipment</option>
                            <option value="GOODS_MISMATCH">Description of Goods Mismatch</option>
                            <option value="DOCS_MISSING">Mandatory Documents Missing</option>
                            <option value="EXPIRY_LAPSED">Presentation after Expiry</option>
                        </select>
                        <textarea 
                            value={detail}
                            onChange={(e) => setDetail(e.target.value)}
                            placeholder="Specific discrepancy detail..."
                        />
                        <button className="log-btn" onClick={logDiscrepancy}>Log Discrepancy</button>
                    </div>

                    <div className="logged-discrepancies">
                        {discrepancies.map(d => (
                            <div key={d.id} className={`disc-item ${d.isWaived ? 'waived' : ''}`}>
                                <div className="disc-info">
                                    <span className="disc-code">{d.code}</span>
                                    <p className="disc-desc">{d.description || 'No additional detail'}</p>
                                </div>
                                {!d.isWaived ? (
                                    <button className="waive-btn" onClick={() => handleWaive(d.id)}>Waive</button>
                                ) : (
                                    <span className="waived-tag">WAIVED</span>
                                )}
                            </div>
                        ))}
                    </div>
                </section>

                <footer className="action-footer">
                    <div className="decision-toggle">
                        <button className={decision === 'Accept' ? 'active green' : ''} onClick={() => setDecision('Accept')}>Clean</button>
                        <button className={decision === 'Discrepant' ? 'active red' : ''} onClick={() => setDecision('Discrepant')}>Discrepant</button>
                    </div>
                    <button className="primary-btn" disabled={!decision}>Submit Examination</button>
                </footer>
            </main>

            <style jsx>{`
                .exam-workspace { display: flex; height: calc(100vh - 120px); border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; }
                .left-pane-lc-context { width: 350px; background: #f8fafc; border-right: 1px solid #e2e8f0; display: flex; flex-direction: column; }
                .right-pane-entry { flex: 1; background: white; display: flex; flex-direction: column; overflow-y: auto; }
                
                .pane-header { padding: 1.25rem; border-bottom: 1px solid #f1f5f9; background: white; }
                .pane-header h3 { margin: 0; font-size: 0.8125rem; text-transform: uppercase; letter-spacing: 0.05em; color: #64748b; font-weight: 800; }

                .accordion-ctx { flex: 1; overflow-y: auto; }
                details { border-bottom: 1px solid #f1f5f9; }
                summary { padding: 1rem 1.25rem; font-weight: 700; color: #1e293b; cursor: pointer; font-size: 0.875rem; background: white; }
                summary:hover { background: #f1f5f9; }
                .ctx-content { padding: 1.25rem; font-size: 0.8125rem; color: #475569; line-height: 1.6; background: #f8fafc; }

                .doc-matrix, .discrepancy-logger { padding: 0 1.5rem 2rem 1.5rem; }
                .matrix-table { width: 100%; border-collapse: collapse; margin-top: 1.5rem; }
                .matrix-table th { text-align: left; padding: 0.75rem; font-size: 0.75rem; color: #94a3b8; text-transform: uppercase; }
                .matrix-table td { padding: 0.75rem; border-bottom: 1px solid #f1f5f9; }
                .matrix-table input { width: 60px; padding: 0.4rem; border: 1px solid #e2e8f0; border-radius: 4px; }

                .discrepancy-logger { border-top: 1px solid #f1f5f9; padding-top: 1.5rem; }
                .logger-entry { margin-top: 1.5rem; display: flex; flex-direction: column; gap: 1rem; }
                .isbp-select { padding: 0.75rem; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 0.875rem; }
                .logger-entry textarea { height: 100px; padding: 0.75rem; border: 1px solid #e2e8f0; border-radius: 6px; resize: none; font-size: 0.875rem; }
                .log-btn { background: #1e293b; color: white; border: none; padding: 0.6rem 1.25rem; border-radius: 6px; font-weight: 700; cursor: pointer; font-size: 0.75rem; align-self: flex-end; }

                .logged-discrepancies { margin-top: 2rem; display: flex; flex-direction: column; gap: 0.75rem; }
                .disc-item { display: flex; justify-content: space-between; align-items: center; padding: 1rem; background: #fff1f2; border: 1px solid #fecdd3; border-radius: 8px; }
                .disc-item.waived { background: #f0f9ff; border-color: #bae6fd; opacity: 0.8; }
                .disc-code { font-size: 0.7rem; font-weight: 800; color: #9f1239; background: white; padding: 0.2rem 0.5rem; border-radius: 4px; }
                .disc-desc { font-size: 0.8125rem; color: #1e293b; margin: 0.25rem 0 0 0; }
                .waive-btn { background: #9f1239; color: white; border: none; padding: 0.4rem 0.75rem; border-radius: 4px; font-size: 0.7rem; font-weight: 700; cursor: pointer; }
                .waived-tag { font-size: 0.7rem; font-weight: 800; color: #0369a1; }

                .action-footer { margin-top: auto; padding: 1.5rem; border-top: 1px solid #e2e8f0; display: flex; justify-content: space-between; align-items: center; background: #f8fafc; position: sticky; bottom: 0; }
                .decision-toggle { display: flex; gap: 1px; background: #e2e8f0; border-radius: 8px; overflow: hidden; padding: 2px; }
                .decision-toggle button { border: none; padding: 0.6rem 1.5rem; font-weight: 700; font-size: 0.8125rem; cursor: pointer; background: white; color: #94a3b8; }
                .decision-toggle button.active.green { background: #059669; color: white; }
                .decision-toggle button.active.red { background: #dc2626; color: white; }
                
                .primary-btn { background: #2563eb; color: white; padding: 0.75rem 2rem; border-radius: 6px; font-weight: 700; border: none; cursor: pointer; }
                .primary-btn:disabled { opacity: 0.3; cursor: not-allowed; }
            `}</style>
        </div>
    );
};
