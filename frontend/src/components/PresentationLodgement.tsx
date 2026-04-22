'use client';

import React, { useState, useEffect } from 'react';

// ABOUTME: Document Presentation Lodgement implementing REQ-IMP-PRC-03.
// ABOUTME: Captures claims, document matrices, and tracks UCP 600 5-day examination SLA.

interface PresentationLodgementProps {
    instrumentId: string;
}

export const PresentationLodgement: React.FC<PresentationLodgementProps> = ({ instrumentId }) => {
    const [formData, setFormData] = useState({
        presentationDate: new Date().toISOString().split('T')[0],
        claimAmount: '',
        currency: 'USD',
    });

    const [docs, setDocs] = useState([
        { type: 'BL', name: 'Bill of Lading', originals: '0', copies: '0' },
        { type: 'INV', name: 'Commercial Invoice', originals: '0', copies: '0' },
        { type: 'PL', name: 'Packing List', originals: '0', copies: '0' },
    ]);

    const [deadline, setDeadline] = useState('');

    useEffect(() => {
        if (formData.presentationDate) {
            const date = new Date(formData.presentationDate);
            // Simple +5 days logic for visual SLA (real banking day logic in backend)
            date.setDate(date.getDate() + 5);
            setDeadline(date.toISOString().split('T')[0]);
        }
    }, [formData.presentationDate]);

    const totalDocs = docs.filter(d => parseInt(d.originals) > 0 || parseInt(d.copies) > 0).length;

    const handleDocChange = (type: string, field: 'originals' | 'copies', value: string) => {
        setDocs(docs.map(d => d.type === type ? { ...d, [field]: value } : d));
    };

    return (
        <div className="lodgement-container premium-card">
            <header className="header-flex">
                <div>
                    <h2 className="title">Document Presentation Lodgement</h2>
                    <p className="subtitle">Instrument: {instrumentId}</p>
                </div>
                <div className="sla-badge">
                    <span className="label">Examination Deadline (UCP 600)</span>
                    <span className="value text-warning">{deadline}</span>
                </div>
            </header>

            <main className="content-grid">
                <section className="header-fields">
                    <div className="field-group">
                        <label htmlFor="presentationDate">Presentation Date</label>
                        <input 
                            id="presentationDate"
                            type="date"
                            value={formData.presentationDate}
                            onChange={e => setFormData({...formData, presentationDate: e.target.value})}
                        />
                    </div>
                    <div className="field-group">
                        <label htmlFor="claimAmount">Claim Amount</label>
                        <div className="input-with-addon">
                            <span className="addon">{formData.currency}</span>
                            <input 
                                id="claimAmount"
                                type="number"
                                value={formData.claimAmount}
                                onChange={e => setFormData({...formData, claimAmount: e.target.value})}
                                placeholder="e.g. 150000"
                            />
                        </div>
                    </div>
                </section>

                <section className="document-matrix">
                    <h3>Document Matrix</h3>
                    <table className="matrix-table">
                        <thead>
                            <tr>
                                <th>Document Type</th>
                                <th>Originals</th>
                                <th>Copies</th>
                            </tr>
                        </thead>
                        <tbody>
                            {docs.map(doc => (
                                <tr key={doc.type} data-testid={`doc-row-${doc.type}`}>
                                    <td>{doc.name}</td>
                                    <td>
                                        <input 
                                            name="originals"
                                            type="number" 
                                            value={doc.originals} 
                                            onChange={e => handleDocChange(doc.type, 'originals', e.target.value)}
                                        />
                                    </td>
                                    <td>
                                        <input 
                                            name="copies"
                                            type="number" 
                                            value={doc.copies} 
                                            onChange={e => handleDocChange(doc.type, 'copies', e.target.value)}
                                        />
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                    <div className="summary-footer">
                        Total Documents Logged: {totalDocs}
                    </div>
                </section>
            </main>

            <footer className="footer-actions">
                <button className="secondary-btn">Cancel</button>
                <button className="primary-btn">Log Presentation</button>
            </footer>

            <style jsx>{`
                .lodgement-container { padding: 2rem; background: white; border-radius: 12px; }
                .header-flex { display: flex; justify-content: space-between; border-bottom: 1px solid #f1f5f9; padding-bottom: 1.5rem; margin-bottom: 2rem; }
                .title { font-size: 1.25rem; font-weight: 700; color: #1e293b; }
                .subtitle { font-size: 0.875rem; color: #64748b; }
                .sla-badge { background: #fffbeb; border: 1px solid #fef3c7; padding: 0.75rem 1rem; border-radius: 8px; text-align: right; }
                .sla-badge .label { display: block; font-size: 0.75rem; color: #92400e; font-weight: 600; }
                .sla-badge .value { font-size: 1rem; font-weight: 700; }
                
                .content-grid { display: grid; gap: 2.5rem; }
                .header-fields { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }
                .field-group { display: flex; flex-direction: column; gap: 0.5rem; }
                .field-group label { font-size: 0.875rem; font-weight: 600; color: #334155; }
                .field-group input { padding: 0.75rem; border: 1px solid #e2e8f0; border-radius: 6px; }
                
                .input-with-addon { display: flex; }
                .input-with-addon .addon { background: #f1f5f9; border: 1px solid #e2e8f0; border-right: none; padding: 0.75rem; border-radius: 6px 0 0 6px; font-weight: 600; color: #475569; }
                .input-with-addon input { border-radius: 0 6px 6px 0; width: 100%; }

                .matrix-table { width: 100%; border-collapse: collapse; margin-top: 1rem; }
                .matrix-table th { text-align: left; padding: 0.75rem; background: #f8fafc; color: #475569; font-size: 0.75rem; text-transform: uppercase; }
                .matrix-table td { padding: 0.75rem; border-bottom: 1px solid #f1f5f9; font-size: 0.875rem; }
                .matrix-table input { width: 60px; padding: 0.5rem; border: 1px solid #e2e8f0; border-radius: 4px; }
                
                .summary-footer { margin-top: 1rem; padding: 1rem; background: #f8fafc; border-radius: 6px; text-align: right; font-weight: 600; color: #1e293b; }

                .footer-actions { display: flex; justify-content: flex-end; gap: 1rem; margin-top: 3rem; border-top: 1px solid #f1f5f9; padding-top: 2rem; }
                .primary-btn { background: #2563eb; color: white; border: none; padding: 0.75rem 2rem; border-radius: 6px; font-weight: 600; cursor: pointer; }
                .secondary-btn { background: #f8fafc; color: #475569; border: 1px solid #e2e8f0; padding: 0.75rem 2rem; border-radius: 6px; font-weight: 600; cursor: pointer; }
            `}</style>
        </div>
    );
};
