'use client';

import React, { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { tradeApi } from '../api/tradeApi';
import { TradeInstrument, ImportLetterOfCredit } from '../api/types';
import { InstrumentDetails } from './InstrumentDetails';

// ABOUTME: A modal wrapper that uses React Portals to ensure top-level rendering.
// ABOUTME: Fixes positioning issues by mounting directly to document.body.

interface Props {
  instrumentId: string;
  onClose: () => void;
}

export const InstrumentDetailsModal: React.FC<Props> = ({ instrumentId, onClose }) => {
  const [instrument, setInstrument] = useState<(TradeInstrument & ImportLetterOfCredit) | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
    setLoading(true);
    tradeApi.getInstrument(instrumentId)
      .then(data => {
        setInstrument(data as any);
        setLoading(false);
      })
      .catch(err => {
        console.error('Fetch Details Error:', err);
        setError('Failed to load instrument details.');
        setLoading(false);
      });
    
    // Prevent body scroll when modal is open
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = 'unset';
    };
  }, [instrumentId]);

  if (!mounted) return null;

  const modalContent = (
    <div className="modal-portal-overlay" onClick={onClose}>
      <div className="modal-portal-content premium-card" onClick={e => e.stopPropagation()}>
        <header className="modal-header">
          <div className="header-prefix">
             <span className="status-dot"></span>
             <h2>Transaction Details</h2>
          </div>
          <button className="close-btn" onClick={onClose}>✕</button>
        </header>

        <main className="modal-body">
          {loading ? (
            <div className="status-message">
              <div className="spinner"></div>
              <span>Synchronizing Data...</span>
            </div>
          ) : error ? (
            <div className="status-message error">{error}</div>
          ) : instrument ? (
            <InstrumentDetails instrument={instrument} />
          ) : (
            <div className="status-message">No data found.</div>
          )}
        </main>

        <style jsx>{`
          .modal-portal-overlay {
            position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
            background: rgba(15, 23, 42, 0.7); backdrop-filter: blur(8px);
            display: flex; align-items: center; justify-content: center;
            z-index: 10000; padding: 2rem;
            animation: fadeIn 0.2s ease-out;
          }
          .modal-portal-content {
            width: 100%; max-width: 1200px; height: 90vh;
            display: flex; flex-direction: column; overflow: hidden;
            animation: modalSlideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
            background: white; border: 1px solid #e2e8f0; border-radius: 16px;
            box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
          }
          
          @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
          @keyframes modalSlideUp { 
            from { opacity: 0; transform: translateY(40px) scale(0.98); } 
            to { opacity: 1; transform: translateY(0) scale(1); } 
          }
          
          .modal-header {
            padding: 1.25rem 2rem; border-bottom: 1px solid #f1f5f9;
            display: flex; justify-content: space-between; align-items: center;
            background: #fff; flex-shrink: 0;
          }
          .header-prefix { display: flex; align-items: center; gap: 0.75rem; }
          .status-dot { width: 8px; height: 8px; background: #2563eb; border-radius: 50%; box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.1); }
          .modal-header h2 { font-size: 1.1rem; font-weight: 800; color: #0f172a; margin: 0; text-transform: uppercase; letter-spacing: 0.025em; }
          .close-btn { background: #f1f5f9; border: none; width: 32px; height: 32px; border-radius: 50%; font-size: 1rem; color: #64748b; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
          .close-btn:hover { background: #e2e8f0; color: #0f172a; transform: rotate(90deg); }
          
          .modal-body { flex: 1; overflow: hidden; background: #f8fafc; position: relative; }
          .status-message { height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 1rem; color: #64748b; font-weight: 600; }
          .status-message.error { color: #dc2626; }
          
          .spinner { width: 40px; height: 40px; border: 3px solid #f1f5f9; border-top-color: #2563eb; border-radius: 50%; animation: spin 0.8s linear infinite; }
          @keyframes spin { to { transform: rotate(360deg); } }
        `}</style>
      </div>
    </div>
  );

  return createPortal(modalContent, document.body);
};
