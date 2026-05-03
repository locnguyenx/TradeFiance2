'use client';

import React from 'react';
import { useToast } from '../context/ToastContext';
import { 
  CheckCircle, 
  AlertCircle, 
  Info, 
  AlertTriangle, 
  X 
} from 'lucide-react';

export const ToastContainer: React.FC = () => {
  const { toasts, removeToast } = useToast();

  if (toasts.length === 0) return null;

  return (
    <div className="toast-container">
      {toasts.map((toast) => (
        <div key={toast.id} className={`toast toast-${toast.type}`}>
          <div className="toast-icon">
            {toast.type === 'success' && <CheckCircle size={20} />}
            {toast.type === 'error' && <AlertCircle size={20} />}
            {toast.type === 'info' && <Info size={20} />}
            {toast.type === 'warning' && <AlertTriangle size={20} />}
          </div>
          <div className="toast-message">{toast.message}</div>
          <button 
            className="toast-close" 
            onClick={() => removeToast(toast.id)}
          >
            <X size={16} />
          </button>
        </div>
      ))}

      <style jsx>{`
        .toast-container {
          position: fixed;
          top: 2rem;
          right: 2rem;
          z-index: 9999;
          display: flex;
          flex-direction: column;
          gap: 0.75rem;
          pointer-events: none;
        }

        .toast {
          pointer-events: auto;
          min-width: 320px;
          max-width: 480px;
          background: white;
          border-radius: 12px;
          padding: 1rem;
          display: flex;
          align-items: center;
          gap: 0.875rem;
          box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
          border: 1px solid #e2e8f0;
          animation: slideIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
        }

        @keyframes slideIn {
          from {
            transform: translateX(100%);
            opacity: 0;
          }
          to {
            transform: translateX(0);
            opacity: 1;
          }
        }

        .toast-success { border-left: 4px solid #10b981; }
        .toast-error { border-left: 4px solid #ef4444; }
        .toast-info { border-left: 4px solid #3b82f6; }
        .toast-warning { border-left: 4px solid #f59e0b; }

        .toast-icon {
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .toast-success .toast-icon { color: #10b981; }
        .toast-error .toast-icon { color: #ef4444; }
        .toast-info .toast-icon { color: #3b82f6; }
        .toast-warning .toast-icon { color: #f59e0b; }

        .toast-message {
          flex: 1;
          font-size: 0.875rem;
          font-weight: 500;
          color: #1e293b;
          line-height: 1.4;
        }

        .toast-close {
          background: transparent;
          border: none;
          color: #94a3b8;
          cursor: pointer;
          padding: 4px;
          border-radius: 4px;
          display: flex;
          transition: all 0.2s;
        }

        .toast-close:hover {
          background: #f1f5f9;
          color: #64748b;
        }
      `}</style>
    </div>
  );
};
