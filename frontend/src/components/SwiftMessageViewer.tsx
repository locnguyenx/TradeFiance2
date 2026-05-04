'use client';

import React, { useEffect, useState } from 'react';
import { Mail, ChevronDown, ChevronUp, Copy, Check, Terminal } from 'lucide-react';
import { SwiftMessage } from '../api/types';
import { tradeApi } from '../api/tradeApi';

interface Props {
  instrumentId: string;
}

export const SwiftMessageViewer: React.FC<Props> = ({ instrumentId }) => {
  const [messages, setMessages] = useState<SwiftMessage[]>([]);
  const [loading, setLoading] = useState(true);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  useEffect(() => {
    const loadMessages = async () => {
      try {
        const res = await tradeApi.getSwiftMessages(instrumentId);
        const list = res.swiftMessageList || [];
        setMessages(list);
        if (list.length > 0) {
          setExpandedId(list[0].swiftMessageId);
        }
      } catch (err) {
        // SWIFT messages load failure is non-critical for view
      } finally {
        setLoading(false);
      }
    };
    loadMessages();
  }, [instrumentId]);

  const copyToClipboard = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  if (loading) return <div className="p-4 text-slate-400 animate-pulse">Loading SWIFT messages...</div>;
  
  if (messages.length === 0) {
    return (
      <div className="swift-empty-state">
        <Terminal size={32} className="text-slate-600 mb-2" />
        <p>No SWIFT messages generated for this instrument yet.</p>
        <span className="text-xs text-slate-500">Messages are auto-triggered upon approval or discrepancy authorization.</span>
      </div>
    );
  }

  return (
    <div className="swift-viewer-stack">
      {messages.map((msg) => (
        <div key={msg.swiftMessageId} className={`swift-msg-card ${expandedId === msg.swiftMessageId ? 'expanded' : ''}`}>
          <div 
            className="swift-msg-header" 
            onClick={() => setExpandedId(expandedId === msg.swiftMessageId ? null : msg.swiftMessageId)}
          >
            <div className="header-left">
              <div className="icon-box">
                <Mail size={16} />
              </div>
              <div className="type-info">
                <span className="msg-type">{msg.messageType}</span>
                <span className="msg-date">{new Date(msg.generatedDate).toLocaleString()}</span>
              </div>
            </div>
            <div className="header-right">
              <span className={`status-pill-small ${msg.messageStatusId?.toLowerCase()}`}>
                {msg.messageStatusId?.replace('SWIFT_MSG_', '')}
              </span>
              <div className="chevron-box">
                {expandedId === msg.swiftMessageId ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
              </div>
            </div>
          </div>
          
          {expandedId === msg.swiftMessageId && (
            <div className="swift-msg-content">
              <div className="content-toolbar">
                <div className="toolbar-info">RAW MESSAGE CONTENT</div>
                <button 
                  className={`copy-btn ${copiedId === msg.swiftMessageId ? 'copied' : ''}`} 
                  onClick={(e) => { e.stopPropagation(); copyToClipboard(msg.messageContent, msg.swiftMessageId); }}
                >
                  {copiedId === msg.swiftMessageId ? <Check size={12} /> : <Copy size={12} />}
                  <span>{copiedId === msg.swiftMessageId ? 'Copied' : 'Copy'}</span>
                </button>
              </div>
              <div className="swift-pre-container">
                <pre className="raw-swift">
                  {msg.messageContent}
                </pre>
              </div>
            </div>
          )}
        </div>
      ))}
    </div>
  );
};
