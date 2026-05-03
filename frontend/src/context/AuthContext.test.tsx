import React from 'react';
import { render, screen, act, waitFor } from '@testing-library/react';
import { AuthProvider, useAuth } from './AuthContext';
import { tradeApi } from '../api/tradeApi';
import { ToastProvider } from './ToastContext';

// Mock tradeApi
jest.mock('../api/tradeApi', () => ({
  tradeApi: {
    getCurrentUser: jest.fn(),
    login: jest.fn(),
    logout: jest.fn(),
  },
}));

const TestComponent = () => {
  const { user, loading, login, logout, isAuthenticated } = useAuth();
  
  if (loading) return <div>Loading...</div>;
  
  return (
    <div>
      <div data-testid="auth-status">{isAuthenticated ? 'Authenticated' : 'Not Authenticated'}</div>
      <div data-testid="user-id">{user?.userId}</div>
      <button onClick={() => login('test', 'pass')}>Login</button>
      <button onClick={() => logout()}>Logout</button>
    </div>
  );
};

describe('AuthContext', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('loads user on initialization', async () => {
    (tradeApi.getCurrentUser as jest.Mock).mockResolvedValue({ userId: '101', username: 'testuser', roles: [] });
    
    render(
      <ToastProvider>
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      </ToastProvider>
    );
    
    expect(screen.getByText('Loading...')).toBeInTheDocument();
    
    await waitFor(() => {
      expect(screen.getByTestId('auth-status')).toHaveTextContent('Authenticated');
    });
    
    expect(screen.getByTestId('user-id')).toHaveTextContent('101');
    expect(tradeApi.getCurrentUser).toHaveBeenCalledTimes(1);
  });

  it('handles failed initial load', async () => {
    (tradeApi.getCurrentUser as jest.Mock).mockResolvedValue({});
    
    render(
      <ToastProvider>
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      </ToastProvider>
    );
    
    await waitFor(() => {
      expect(screen.getByTestId('auth-status')).toHaveTextContent('Not Authenticated');
    });
  });

  it('handles login success', async () => {
    (tradeApi.getCurrentUser as jest.Mock).mockResolvedValueOnce({}); // Initial load
    (tradeApi.login as jest.Mock).mockResolvedValue({ loggedIn: true });
    (tradeApi.getCurrentUser as jest.Mock).mockResolvedValueOnce({ userId: '202', roles: [] }); // After login
    
    render(
      <ToastProvider>
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      </ToastProvider>
    );
    
    await waitFor(() => {
      expect(screen.getByTestId('auth-status')).toHaveTextContent('Not Authenticated');
    });
    
    act(() => {
      screen.getByText('Login').click();
    });
    
    await waitFor(() => {
      expect(screen.getByTestId('auth-status')).toHaveTextContent('Authenticated');
    });
    
    expect(screen.getByTestId('user-id')).toHaveTextContent('202');
  });

  it('handles logout', async () => {
    (tradeApi.getCurrentUser as jest.Mock).mockResolvedValue({ userId: '101', roles: [] });
    (tradeApi.logout as jest.Mock).mockResolvedValue({});
    
    render(
      <ToastProvider>
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      </ToastProvider>
    );
    
    await waitFor(() => {
      expect(screen.getByTestId('auth-status')).toHaveTextContent('Authenticated');
    });
    
    act(() => {
      screen.getByText('Logout').click();
    });
    
    await waitFor(() => {
      expect(screen.getByTestId('auth-status')).toHaveTextContent('Not Authenticated');
    });
    
    expect(tradeApi.logout).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId('user-id')).toHaveTextContent('');
  });
});
