'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { tradeApi } from '../../api/tradeApi';
import { 
  User, 
  Shield, 
  Lock, 
  Mail, 
  CreditCard, 
  CheckCircle2, 
  AlertCircle, 
  Loader2,
  ChevronRight
} from 'lucide-react';

export default function ProfilePage() {
  const { user } = useAuth();
  const { showToast } = useToast();
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const handlePasswordChange = async (e: React.FormEvent) => {
    e.preventDefault();

    if (newPassword !== confirmPassword) {
      showToast('error', 'New passwords do not match');
      return;
    }

    if (newPassword.length < 8) {
      showToast('error', 'Password must be at least 8 characters long');
      return;
    }

    setLoading(true);
    try {
      const result = await tradeApi.changePassword({
        oldPassword,
        newPassword,
        newPasswordVerify: confirmPassword
      });
      
      if (result.errors) {
        showToast('error', result.errors.join(', '));
      } else {
        showToast('success', 'Password updated successfully');
        setOldPassword('');
        setNewPassword('');
        setConfirmPassword('');
      }
    } catch (err: any) {
      showToast('error', err.body?.message || 'Failed to update password');
    } finally {
      setLoading(false);
    }
  };

  if (!user) return null;

  const tierLabels: Record<string, string> = {
    'TIER_1': 'Tier 1 - Maker',
    'TIER_2': 'Tier 2 - Junior Checker',
    'TIER_3': 'Tier 3 - Senior Checker',
    'TIER_4': 'Tier 4 - Executive / Dual-Auth',
  };

  return (
    <div className="profile-container">
      <header className="profile-header">
        <h1>User Profile</h1>
        <p>Manage your account settings and security preferences</p>
      </header>

      <div className="profile-grid">
        <section className="profile-card identity-section">
          <div className="card-header">
            <User size={20} className="header-icon" />
            <h2>Account Information</h2>
          </div>
          <div className="card-content">
            <div className="info-row">
              <div className="info-label">Full Name</div>
              <div className="info-value">{user.firstName} {user.lastName}</div>
            </div>
            <div className="info-row">
              <div className="info-label">Username</div>
              <div className="info-value">{user.username}</div>
            </div>
            <div className="info-row">
              <div className="info-label">Email Address</div>
              <div className="info-value">
                <Mail size={14} style={{ marginRight: '6px', opacity: 0.6 }} />
                {user.emailAddress}
              </div>
            </div>
            <div className="info-row">
              <div className="info-label">User ID</div>
              <div className="info-value code">#{user.userId}</div>
            </div>
          </div>
        </section>

        <section className="profile-card authority-section">
          <div className="card-header">
            <Shield size={20} className="header-icon" />
            <h2>Authority & Limits</h2>
          </div>
          <div className="card-content">
            <div className="authority-hero">
              <div className="tier-badge">
                {user.roles?.[0]?.replace('TRADE_', '') || 'USER'}
              </div>
              <div className="tier-name">{tierLabels[user.delegationTierId || 'TIER_1']}</div>
            </div>
            <div className="limit-display">
              <div className="limit-label">Approval Limit</div>
              <div className="limit-value">
                <CreditCard size={16} className="limit-icon" />
                <span>{user.currencyUomId} {user.customLimit?.toLocaleString(undefined, { minimumFractionDigits: 2 })}</span>
              </div>
            </div>
            <div className="authority-footer">
              <p>Your authority tier is assigned by the System Administrator based on your operational role.</p>
            </div>
          </div>
        </section>

        <section className="profile-card password-section">
          <div className="card-header">
            <Lock size={20} className="header-icon" />
            <h2>Security</h2>
          </div>
          <div className="card-content">
            <form onSubmit={handlePasswordChange} className="password-form">
              <h3>Change Password</h3>
              
              <div className="form-group">
                <label>Current Password</label>
                <input 
                  type="password" 
                  value={oldPassword}
                  onChange={(e) => setOldPassword(e.target.value)}
                  placeholder="••••••••"
                  required
                />
              </div>

              <div className="form-group">
                <label>New Password</label>
                <input 
                  type="password" 
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="Min 8 characters"
                  required
                />
              </div>

              <div className="form-group">
                <label>Confirm New Password</label>
                <input 
                  type="password" 
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Repeat new password"
                  required
                />
              </div>

              <button type="submit" className="save-btn" disabled={loading}>
                {loading ? <Loader2 className="animate-spin" size={18} /> : 'Update Password'}
              </button>
            </form>
          </div>
        </section>
      </div>

      <style jsx>{`
        .profile-container {
          max-width: 1200px;
          margin: 0 auto;
          padding: 2rem;
        }

        .profile-header {
          margin-bottom: 2.5rem;
        }

        .profile-header h1 {
          font-size: 2rem;
          font-weight: 800;
          color: #1e293b;
          margin-bottom: 0.5rem;
        }

        .profile-header p {
          color: #64748b;
          font-size: 1.1rem;
        }

        .profile-grid {
          display: grid;
          grid-template-columns: repeat(2, 1fr);
          gap: 2rem;
        }

        .profile-card {
          background: white;
          border-radius: 16px;
          border: 1px solid #e2e8f0;
          overflow: hidden;
          box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
        }

        .password-section {
          grid-column: span 2;
        }

        .card-header {
          padding: 1.25rem 1.5rem;
          background: #f8fafc;
          border-bottom: 1px solid #e2e8f0;
          display: flex;
          align-items: center;
          gap: 0.75rem;
        }

        .header-icon {
          color: #3b82f6;
        }

        .card-header h2 {
          font-size: 1rem;
          font-weight: 700;
          color: #1e293b;
          margin: 0;
        }

        .card-content {
          padding: 1.5rem;
        }

        .info-row {
          display: flex;
          justify-content: space-between;
          padding: 1rem 0;
          border-bottom: 1px solid #f1f5f9;
        }

        .info-row:last-child {
          border-bottom: none;
        }

        .info-label {
          color: #64748b;
          font-size: 0.875rem;
          font-weight: 500;
        }

        .info-value {
          color: #1e293b;
          font-size: 0.875rem;
          font-weight: 600;
          display: flex;
          align-items: center;
        }

        .info-value.code {
          font-family: ui-monospace, monospace;
          background: #f1f5f9;
          padding: 0.2rem 0.5rem;
          border-radius: 4px;
          font-size: 0.75rem;
        }

        .authority-hero {
          display: flex;
          flex-direction: column;
          align-items: center;
          padding: 1.5rem 0;
          gap: 1rem;
        }

        .tier-badge {
          background: linear-gradient(135deg, #3b82f6, #1d4ed8);
          color: white;
          padding: 0.5rem 1.25rem;
          border-radius: 99px;
          font-weight: 800;
          letter-spacing: 0.05em;
          font-size: 0.875rem;
          box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
        }

        .tier-name {
          font-weight: 700;
          color: #1e293b;
          font-size: 1.125rem;
        }

        .limit-display {
          margin-top: 1.5rem;
          background: #f0f9ff;
          border: 1px solid #bae6fd;
          border-radius: 12px;
          padding: 1.25rem;
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 0.5rem;
        }

        .limit-label {
          font-size: 0.75rem;
          text-transform: uppercase;
          font-weight: 700;
          color: #0369a1;
          letter-spacing: 0.05em;
        }

        .limit-value {
          display: flex;
          align-items: center;
          gap: 0.75rem;
          font-size: 1.5rem;
          font-weight: 800;
          color: #0c4a6e;
        }

        .limit-icon {
          color: #0ea5e9;
        }

        .authority-footer {
          margin-top: 2rem;
          padding-top: 1.5rem;
          border-top: 1px solid #f1f5f9;
        }

        .authority-footer p {
          font-size: 0.8125rem;
          color: #94a3b8;
          line-height: 1.5;
          text-align: center;
        }

        .password-form {
          max-width: 500px;
          margin: 0 auto;
        }

        .password-form h3 {
          font-size: 1.125rem;
          font-weight: 700;
          margin-bottom: 1.5rem;
          color: #1e293b;
        }

        .message-alert {
          padding: 0.75rem 1rem;
          border-radius: 8px;
          display: flex;
          align-items: center;
          gap: 0.75rem;
          font-size: 0.875rem;
          margin-bottom: 1.5rem;
        }

        .message-alert.success {
          background: #f0fdf4;
          border: 1px solid #bbf7d0;
          color: #166534;
        }

        .message-alert.error {
          background: #fef2f2;
          border: 1px solid #fecaca;
          color: #991b1b;
        }

        .form-group {
          margin-bottom: 1.25rem;
        }

        .form-group label {
          display: block;
          font-size: 0.875rem;
          font-weight: 600;
          color: #475569;
          margin-bottom: 0.5rem;
        }

        .form-group input {
          width: 100%;
          padding: 0.75rem;
          border: 1px solid #e2e8f0;
          border-radius: 8px;
          outline: none;
          transition: border-color 0.2s;
        }

        .form-group input:focus {
          border-color: #3b82f6;
          box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
        }

        .save-btn {
          width: 100%;
          background: #2563eb;
          color: white;
          border: none;
          border-radius: 8px;
          padding: 0.875rem;
          font-weight: 700;
          cursor: pointer;
          transition: background 0.2s;
          display: flex;
          align-items: center;
          justify-content: center;
          margin-top: 1rem;
        }

        .save-btn:hover:not(:disabled) {
          background: #1d4ed8;
        }

        .save-btn:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }
      `}</style>
    </div>
  );
}
