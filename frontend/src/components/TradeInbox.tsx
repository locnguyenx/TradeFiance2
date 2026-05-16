'use client';

import React, { useEffect, useState } from 'react';
import { Mail, RefreshCw, CheckCircle, AlertCircle, ExternalLink, ArrowRight } from 'lucide-react';
import { tradeApi } from '../api/tradeApi';
import { TradeInboxItem } from '../api/types';

export const TradeInbox: React.FC = () => {
  const [items, setItems] = useState<TradeInboxItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [processingId, setProcessingId] = useState<string | null>(null);

  const loadInbox = async () => {
    setLoading(true);
    try {
      const res = await tradeApi.getInboxItems();
      setItems(res.inboxItems || []);
    } catch (err: any) {
      setError(err.message || 'Failed to load inbox');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadInbox();
  }, []);

  const handleAction = async (item: TradeInboxItem, action: string) => {
    setProcessingId(item.inboxItemId);
    try {
      if (action === 'acknowledge') {
        await tradeApi.acknowledgeMt730(item.inboxItemId);
      } else if (action === 'resolve') {
        await tradeApi.resolveAmendmentConsent(item.inboxItemId);
      } else if (action === 'spawn') {
        await tradeApi.spawnPresentation750(item.inboxItemId);
      }
      await loadInbox();
    } catch (err: any) {
      alert(`Action failed: ${err.message}`);
    } finally {
      setProcessingId(null);
    }
  };

  if (loading) return <div className="p-8 text-center text-slate-400">Loading inbox items...</div>;
  if (error) return <div className="p-8 text-center text-red-400">Error: {error}</div>;

  return (
    <div className="trade-inbox-container">
      <div className="inbox-controls">
        <button onClick={loadInbox} className="refresh-btn">
          <RefreshCw size={16} />
          <span>Refresh</span>
        </button>
      </div>

      <div className="inbox-grid">
        <table className="inbox-table">
          <thead>
            <tr>
              <th>MSG TYPE</th>
              <th>SENDER</th>
              <th>REFERENCE</th>
              <th>CORRELATION</th>
              <th>RECEIVED</th>
              <th>STATUS</th>
              <th>ACTIONS</th>
            </tr>
          </thead>
          <tbody>
            {items.length === 0 ? (
              <tr>
                <td colSpan={7} className="empty-row">No inbound messages in the inbox</td>
              </tr>
            ) : (
              items.map((item) => (
                <tr key={item.inboxItemId} className={item.inboxStatusEnumId === 'INBOX_UNREAD' ? 'unread' : 'processed'}>
                  <td className="msg-type-cell">
                    <div className="type-badge">{item.messageType}</div>
                  </td>
                  <td>{item.senderBic || 'UNKNOWN'}</td>
                  <td>{item.senderReference || '-'}</td>
                  <td>
                    {item.instrumentRef ? (
                      <div className="correlation-info">
                        <span className="ref">{item.instrumentRef}</span>
                        <ExternalLink size={12} className="link-icon" />
                      </div>
                    ) : (
                      <span className="orphan">ORPHAN</span>
                    )}
                  </td>
                  <td>{new Date(item.receivedTimestamp).toLocaleString()}</td>
                  <td>
                    <span className={`status-pill ${item.inboxStatusEnumId.toLowerCase()}`}>
                      {item.inboxStatusEnumId.replace('INBOX_', '')}
                    </span>
                  </td>
                  <td>
                    <div className="action-buttons">
                      {item.inboxStatusEnumId === 'INBOX_UNREAD' && (
                        <>
                          {(item.messageType === '730' || item.messageType === 'MT730') && (
                            <button 
                              disabled={!!processingId}
                              onClick={() => handleAction(item, 'acknowledge')}
                              className="action-btn acknowledge"
                            >
                              {processingId === item.inboxItemId ? '...' : 'Acknowledge'}
                            </button>
                          )}
                          {(item.messageType === '799' || item.messageType === 'MT799') && (
                            <button 
                              disabled={!!processingId}
                              onClick={() => handleAction(item, 'resolve')}
                              className="action-btn resolve"
                            >
                              {processingId === item.inboxItemId ? '...' : 'Resolve Consent'}
                            </button>
                          )}
                          {(item.messageType === '750' || item.messageType === 'MT750') && (
                            <button 
                              disabled={!!processingId}
                              onClick={() => handleAction(item, 'spawn')}
                              className="action-btn spawn"
                            >
                              {processingId === item.inboxItemId ? '...' : 'Spawn Presentation'}
                            </button>
                          )}
                        </>
                      )}
                      {item.inboxStatusEnumId === 'INBOX_PROCESSED' && (
                        <CheckCircle size={18} className="text-green-500" />
                      )}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <style jsx>{`
        .trade-inbox-container {
          background: #1e293b;
          border-radius: 0.75rem;
          padding: 1.5rem;
          color: #f1f5f9;
        }
        .inbox-controls {
          display: flex;
          justify-content: flex-end;
          margin-bottom: 1rem;
        }
        .refresh-btn {
          display: flex;
          align-items: center;
          gap: 0.5rem;
          background: #334155;
          border: none;
          color: white;
          padding: 0.5rem 1rem;
          border-radius: 0.375rem;
          cursor: pointer;
          transition: background 0.2s;
        }
        .refresh-btn:hover {
          background: #475569;
        }
        .inbox-table {
          width: 100%;
          border-collapse: collapse;
          font-size: 0.875rem;
        }
        .inbox-table th {
          text-align: left;
          padding: 1rem;
          border-bottom: 1px solid #334155;
          color: #94a3b8;
          font-weight: 500;
          text-transform: uppercase;
          letter-spacing: 0.05em;
        }
        .inbox-table td {
          padding: 1rem;
          border-bottom: 1px solid #334155;
        }
        .inbox-table tr:last-child td {
          border-bottom: none;
        }
        .unread {
          background: rgba(37, 99, 235, 0.05);
        }
        .type-badge {
          background: #334155;
          padding: 0.25rem 0.5rem;
          border-radius: 0.25rem;
          font-family: monospace;
          font-weight: 600;
          color: #38bdf8;
        }
        .correlation-info {
          display: flex;
          align-items: center;
          gap: 0.5rem;
          color: #38bdf8;
        }
        .orphan {
          color: #f87171;
          font-weight: 600;
          font-size: 0.75rem;
        }
        .status-pill {
          padding: 0.25rem 0.75rem;
          border-radius: 9999px;
          font-size: 0.75rem;
          font-weight: 600;
          text-transform: uppercase;
        }
        .inbox_unread {
          background: rgba(37, 99, 235, 0.2);
          color: #60a5fa;
          border: 1px solid rgba(37, 99, 235, 0.3);
        }
        .inbox_processed {
          background: rgba(34, 197, 94, 0.2);
          color: #4ade80;
          border: 1px solid rgba(34, 197, 94, 0.3);
        }
        .action-buttons {
          display: flex;
          gap: 0.5rem;
        }
        .action-btn {
          padding: 0.375rem 0.75rem;
          border-radius: 0.375rem;
          font-size: 0.75rem;
          font-weight: 600;
          border: none;
          cursor: pointer;
          transition: filter 0.2s;
        }
        .action-btn:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }
        .acknowledge {
          background: #2563eb;
          color: white;
        }
        .resolve {
          background: #8b5cf6;
          color: white;
        }
        .spawn {
          background: #f59e0b;
          color: white;
        }
        .empty-row {
          text-align: center;
          color: #64748b;
          padding: 3rem;
        }
      `}</style>
    </div>
  );
};
