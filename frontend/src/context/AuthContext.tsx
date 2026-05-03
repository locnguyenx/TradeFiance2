'use client';

import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { tradeApi } from '../api/tradeApi';
import { useToast } from './ToastContext';

interface User {
  userId: string;
  username: string;
  firstName?: string;
  lastName?: string;
  emailAddress?: string;
  roles: string[];
  delegationTierId?: string;
  customLimit?: number;
  currencyUomId?: string;
}

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<boolean>;
  logout: () => Promise<void>;
  isAuthenticated: boolean;
  hasRole: (role: string) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { showToast } = useToast();
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  const loadCurrentUser = useCallback(async () => {
    try {
      const userData = await tradeApi.getCurrentUser();
      if (userData && userData.userId) {
        setUser(userData);
      } else {
        setUser(null);
      }
    } catch (err) {
      console.error('Failed to load current user', err);
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadCurrentUser();
  }, [loadCurrentUser]);

  const login = async (username: string, password: string): Promise<boolean> => {
    setLoading(true);
    try {
      const result = await tradeApi.login(username, password);
      if (result.loggedIn) {
        await loadCurrentUser();
        showToast('success', 'Successfully signed in');
        return true;
      }
      showToast('error', 'Invalid username or password');
      return false;
    } catch (err) {
      console.error('Login failed', err);
      return false;
    } finally {
      setLoading(false);
    }
  };

  const logout = async () => {
    setLoading(true);
    try {
      await tradeApi.logout();
      setUser(null);
      showToast('info', 'You have been signed out');
    } catch (err) {
      console.error('Logout failed', err);
    } finally {
      setLoading(false);
    }
  };

  const hasRole = (role: string) => {
    return user?.roles?.includes(role) || false;
  };

  const value = {
    user,
    loading,
    login,
    logout,
    isAuthenticated: !!user,
    hasRole,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
