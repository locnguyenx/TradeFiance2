'use client';

import React, { useState, useEffect } from 'react';
import { tradeApi } from '../api/tradeApi';

// ABOUTME: Clause Selector component for injecting standard legal text templates.
// ABOUTME: Fetches data via tradeApi to ensure centralized REST handling.

interface Clause {
    clauseId: string;
    clauseName: string;
    clauseText: string;
}

interface ClauseSelectorProps {
    type: 'GOODS' | 'DOCUMENTS' | 'CONDITIONS';
    onSelect: (text: string) => void;
    onClose: () => void;
}

export const ClauseSelector: React.FC<ClauseSelectorProps> = ({ type, onSelect, onClose }) => {
    const [clauses, setClauses] = useState<Clause[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchClauses = async () => {
            try {
                const data = await tradeApi.getStandardClauses(type);
                setClauses(data);
            } catch (error) {
                console.error('Failed to fetch clauses:', error);
                setClauses([]);
            } finally {
                setLoading(false);
            }
        };
        fetchClauses();
    }, [type]);

    return (
        <div className="clause-modal-overlay">
            <div className="clause-modal premium-card">
                <header className="modal-header">
                    <h3>Standard Clauses: {type}</h3>
                    <button className="close-x" onClick={onClose}>&times;</button>
                </header>
                
                <div className="clause-list">
                    {loading ? (
                        <p>Loading templates...</p>
                    ) : clauses.map(clause => (
                        <div 
                            key={clause.clauseId} 
                            className="clause-item"
                            onClick={() => {
                                onSelect(clause.clauseText);
                                onClose();
                            }}
                        >
                            <span className="clause-name">{clause.clauseName}</span>
                            <p className="clause-preview">{clause.clauseText.substring(0, 80)}...</p>
                        </div>
                    ))}
                </div>
            </div>

            <style jsx>{`
                .clause-modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
                .clause-modal { width: 500px; max-height: 80vh; background: white; padding: 0; overflow: hidden; display: flex; flex-direction: column; }
                .modal-header { padding: 1.25rem; border-bottom: 1px solid #f1f5f9; display: flex; justify-content: space-between; align-items: center; }
                .modal-header h3 { margin: 0; font-size: 1rem; color: #1e293b; }
                .close-x { border: none; background: none; font-size: 1.5rem; cursor: pointer; color: #94a3b8; }
                
                .clause-list { padding: 1rem; overflow-y: auto; display: flex; flex-direction: column; gap: 0.75rem; }
                .clause-item { padding: 1rem; border: 1px solid #e2e8f0; border-radius: 8px; cursor: pointer; transition: all 0.2s; }
                .clause-item:hover { border-color: #2563eb; background: #f8fafc; }
                .clause-name { display: block; font-weight: 700; color: #1e293b; font-size: 0.875rem; margin-bottom: 0.25rem; }
                .clause-preview { margin: 0; font-size: 0.75rem; color: #64748b; line-height: 1.4; }
            `}</style>
        </div>
    );
};
