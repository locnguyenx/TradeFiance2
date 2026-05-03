'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '../../context/AuthContext';
import { Globe, Lock, User, AlertCircle, Loader2 } from 'lucide-react';

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const { login, isAuthenticated, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (isAuthenticated) {
      router.push('/transactions');
    }
  }, [isAuthenticated, router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    
    if (!username || !password) {
      setError('Please enter both username and password');
      return;
    }

    const success = await login(username, password);
    if (!success) {
      setError('Invalid username or password');
    }
  };

  if (loading && !error) {
    return (
      <div className="login-container flex items-center justify-center min-h-screen">
        <Loader2 className="animate-spin text-blue-600" size={48} />
      </div>
    );
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-header">
          <div className="logo-container">
            <Globe size={32} color="white" />
          </div>
          <h1>TRADEFINANCE</h1>
          <p>Global Trade & Supply Chain Platform</p>
        </div>

        <form className="login-form" onSubmit={handleSubmit}>
          {error && (
            <div className="error-alert">
              <AlertCircle size={18} />
              <span>{error}</span>
            </div>
          )}

          <div className="input-group">
            <label htmlFor="username">Username</label>
            <div className="input-wrapper">
              <User size={18} className="input-icon" />
              <input
                id="username"
                type="text"
                placeholder="Enter your username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                autoComplete="username"
              />
            </div>
          </div>

          <div className="input-group">
            <label htmlFor="password">Password</label>
            <div className="input-wrapper">
              <Lock size={18} className="input-icon" />
              <input
                id="password"
                type="password"
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
              />
            </div>
          </div>

          <button type="submit" className="login-btn" disabled={loading}>
            {loading ? <Loader2 className="animate-spin" size={18} /> : 'Sign In'}
          </button>
        </form>

        <div className="login-footer">
          <p>&copy; 2026 Antigravity Trade Systems</p>
          <div className="footer-links">
            <a href="#">Privacy Policy</a>
            <span className="dot">•</span>
            <a href="#">Support</a>
          </div>
        </div>
      </div>

      <style jsx>{`
        .login-page {
          min-h-screen: 100vh;
          display: flex;
          align-items: center;
          justify-content: center;
          background: radial-gradient(circle at top right, #1e293b, #0f172a);
          font-family: var(--font-inter), system-ui, sans-serif;
          padding: 1rem;
        }

        .login-card {
          width: 100%;
          max-width: 420px;
          background: rgba(255, 255, 255, 0.03);
          backdrop-filter: blur(12px);
          border: 1px solid rgba(255, 255, 255, 0.1);
          border-radius: 24px;
          padding: 2.5rem;
          box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
        }

        .login-header {
          text-align: center;
          margin-bottom: 2.5rem;
        }

        .logo-container {
          width: 56px;
          height: 56px;
          background: linear-gradient(135deg, #3b82f6, #1d4ed8);
          border-radius: 16px;
          display: flex;
          align-items: center;
          justify-content: center;
          margin: 0 auto 1.5rem;
          box-shadow: 0 8px 16px rgba(59, 130, 246, 0.3);
        }

        .login-header h1 {
          color: white;
          font-size: 1.5rem;
          font-weight: 800;
          letter-spacing: 0.1em;
          margin-bottom: 0.5rem;
        }

        .login-header p {
          color: #94a3b8;
          font-size: 0.875rem;
        }

        .login-form {
          display: flex;
          flex-direction: column;
          gap: 1.5rem;
        }

        .error-alert {
          background: rgba(239, 68, 68, 0.1);
          border: 1px solid rgba(239, 68, 68, 0.2);
          color: #fca5a5;
          padding: 0.75rem 1rem;
          border-radius: 12px;
          display: flex;
          align-items: center;
          gap: 0.75rem;
          font-size: 0.875rem;
        }

        .input-group {
          display: flex;
          flex-direction: column;
          gap: 0.5rem;
        }

        .input-group label {
          color: #e2e8f0;
          font-size: 0.8125rem;
          font-weight: 600;
        }

        .input-wrapper {
          position: relative;
          display: flex;
          align-items: center;
        }

        .input-icon {
          position: absolute;
          left: 1rem;
          color: #64748b;
        }

        .input-wrapper input {
          width: 100%;
          background: rgba(15, 23, 42, 0.6);
          border: 1px solid rgba(255, 255, 255, 0.1);
          border-radius: 12px;
          padding: 0.75rem 1rem 0.75rem 3rem;
          color: white;
          font-size: 0.9375rem;
          transition: all 0.2s;
          outline: none;
        }

        .input-wrapper input:focus {
          border-color: #3b82f6;
          background: rgba(15, 23, 42, 0.8);
          box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1);
        }

        .login-btn {
          margin-top: 0.5rem;
          background: linear-gradient(135deg, #3b82f6, #2563eb);
          color: white;
          border: none;
          border-radius: 12px;
          padding: 0.875rem;
          font-size: 0.9375rem;
          font-weight: 700;
          cursor: pointer;
          transition: all 0.2s;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .login-btn:hover:not(:disabled) {
          transform: translateY(-1px);
          box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
          background: linear-gradient(135deg, #60a5fa, #3b82f6);
        }

        .login-btn:active:not(:disabled) {
          transform: translateY(0);
        }

        .login-btn:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }

        .login-footer {
          margin-top: 3rem;
          text-align: center;
        }

        .login-footer p {
          color: #475569;
          font-size: 0.75rem;
          margin-bottom: 0.75rem;
        }

        .footer-links {
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 0.75rem;
        }

        .footer-links a {
          color: #64748b;
          font-size: 0.75rem;
          text-decoration: none;
          transition: color 0.2s;
        }

        .footer-links a:hover {
          color: #94a3b8;
        }

        .dot {
          color: #334155;
          font-size: 0.75rem;
        }
      `}</style>
    </div>
  );
}
