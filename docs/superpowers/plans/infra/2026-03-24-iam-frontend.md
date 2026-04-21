# IAM Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement complete IAM frontend with authentication, user management, role management, activity monitoring, and thresholds for Internal Admin and Company Admin users.

**Architecture:** React 18 + TypeScript + Ant Design + Redux Toolkit. Role-based route protection with JWT authentication. Separate pages for System Admin (full IAM) and Company Admin (limited to company users/thresholds).

**Tech Stack:** React 18, TypeScript, Ant Design 6, Redux Toolkit, React Router DOM 7, Axios, Vitest

---

## Phase 1: Authentication (Priority: Critical)

### Task 1.1: Create Login Page

**Files:**
- Create: `frontend/src/pages/auth/LoginPage.tsx`

- [ ] **Step 1: Create LoginPage component**

```typescript
// frontend/src/pages/auth/LoginPage.tsx
import React, { useState } from 'react';
import { Form, Input, Button, Card, message } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { login } from '@/store/slices/authSlice';

const LoginPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const dispatch = useDispatch();

  const onFinish = async (values: { email: string; password: string }) => {
    setLoading(true);
    try {
      const result = await dispatch(login(values) as any);
      if (login.fulfilled.match(result)) {
        if (result.payload.mfaRequired) {
          navigate('/mfa-verify');
        } else {
          navigate('/dashboard');
        }
      }
    } catch (error) {
      message.error('Invalid email or password');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', background: '#f0f2f5' }}>
      <Card style={{ width: 400 }}>
        <h2 style={{ textAlign: 'center', marginBottom: 24 }}>Banking Portal</h2>
        <Form name="login" onFinish={onFinish}>
          <Form.Item name="email" rules={[{ required: true, message: 'Please enter your email' }]}>
            <Input prefix={<UserOutlined />} placeholder="Email" size="large" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: 'Please enter your password' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="Password" size="large" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block size="large">
              Sign In
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};

export default LoginPage;
```

- [ ] **Step 2: Run build to verify**

Run: `cd frontend && npm run build`
Expected: BUILD SUCCESSFUL

---

### Task 1.2: Create Auth Layout

**Files:**
- Create: `frontend/src/components/layout/AuthLayout.tsx`

- [ ] **Step 1: Create AuthLayout component**

```typescript
// frontend/src/components/layout/AuthLayout.tsx
import React from 'react';
import { Outlet } from 'react-router-dom';

const AuthLayout: React.FC = () => {
  return (
    <div style={{ minHeight: '100vh', background: '#f0f2f5' }}>
      <Outlet />
    </div>
  );
};

export default AuthLayout;
```

---

### Task 1.3: Update Auth Service

**Files:**
- Modify: `frontend/src/services/authService.ts`

- [ ] **Step 1: Update authService with correct API endpoints**

```typescript
// frontend/src/services/authService.ts
import axios from 'axios';
import type { LoginRequest, TokenResponse, RefreshTokenRequest, MfaEnrollResponse, MfaVerifyRequest } from '@/types/auth.types';

const API_BASE = '/api/auth';

const authAxios = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const authService = {
  login: async (request: LoginRequest): Promise<TokenResponse> => {
    const response = await authAxios.post('/login', request);
    return response.data;
  },

  refresh: async (request: RefreshTokenRequest): Promise<TokenResponse> => {
    const response = await authAxios.post('/refresh', request);
    return response.data;
  },

  logout: async (): Promise<void> => {
    // Backend may not have logout endpoint
  },

  enrollMfa: async (): Promise<MfaEnrollResponse> => {
    const response = await authAxios.post('/mfa/enroll');
    return response.data;
  },

  verifyMfa: async (request: MfaVerifyRequest): Promise<TokenResponse> => {
    const response = await authAxios.post('/mfa/verify', request);
    return response.data;
  },

  enableMfa: async (code: string): Promise<void> => {
    const response = await authAxios.post('/mfa/enable', { code });
    return response.data;
  },

  disableMfa: async (): Promise<void> => {
    const response = await authAxios.post('/mfa/disable');
    return response.data;
  },

  getMfaStatus: async (): Promise<{ enabled: boolean }> => {
    const response = await authAxios.get('/mfa/status');
    return response.data;
  },
};
```

---

### Task 1.4: Create ProtectedRoute Component

**Files:**
- Create: `frontend/src/components/common/ProtectedRoute.tsx`

- [ ] **Step 1: Create ProtectedRoute component**

```typescript
// frontend/src/components/common/ProtectedRoute.tsx
import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useSelector } from 'react-redux';
import type { RootState } from '@/store';
import { Spin } from 'antd';

interface ProtectedRouteProps {
  children: React.ReactNode;
  allowedRoles?: string[];
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, allowedRoles }) => {
  const { isAuthenticated, loading } = useSelector((state: RootState) => state.auth);
  const user = useSelector((state: RootState) => state.auth.user);
  const location = useLocation();

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Spin size="large" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles && user?.role && !allowedRoles.includes(user.role)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <>{children}</>;
};

export default ProtectedRoute;
```

---

### Task 1.5: Create useAuth Hook

**Files:**
- Create: `frontend/src/hooks/useAuth.ts`

- [ ] **Step 1: Create useAuth hook**

```typescript
// frontend/src/hooks/useAuth.ts
import { useSelector, useDispatch } from 'react-redux';
import type { RootState } from '@/store';
import { login, logout, refreshToken, verifyMfa } from '@/store/slices/authSlice';
import type { LoginRequest, MfaVerifyRequest } from '@/types/auth.types';

export const useAuth = () => {
  const dispatch = useDispatch();
  const { user, isAuthenticated, loading, error, mfaRequired } = useSelector(
    (state: RootState) => state.auth
  );

  const handleLogin = async (credentials: LoginRequest) => {
    return dispatch(login(credentials));
  };

  const handleLogout = async () => {
    return dispatch(logout());
  };

  const handleRefreshToken = async (refreshTokenValue: string) => {
    return dispatch(refreshToken({ refreshToken: refreshTokenValue }));
  };

  const handleVerifyMfa = async (code: string) => {
    return dispatch(verifyMfa({ code } as MfaVerifyRequest));
  };

  return {
    user,
    isAuthenticated,
    loading,
    error,
    mfaRequired,
    login: handleLogin,
    logout: handleLogout,
    refreshToken: handleRefreshToken,
    verifyMfa: handleVerifyMfa,
  };
};
```

---

### Task 1.6: Update App.tsx with Auth Routes

**Files:**
- Modify: `frontend/src/App.tsx`

- [ ] **Step 1: Add auth routes to App.tsx**

```typescript
// Add imports
import LoginPage from '@/pages/auth/LoginPage';
import MfaVerifyPage from '@/pages/auth/MfaVerifyPage';
import ProtectedRoute from '@/components/common/ProtectedRoute';
import AuthLayout from '@/components/layout/AuthLayout';

// Add routes in App.tsx
<Route element={<AuthLayout />}>
  <Route path="/login" element={<LoginPage />} />
  <Route path="/mfa-verify" element={<MfaVerifyPage />} />
</Route>
```

---

### Task 1.7: Create MFA Verify Page

**Files:**
- Create: `frontend/src/pages/auth/MfaVerifyPage.tsx`

- [ ] **Step 1: Create MfaVerifyPage**

```typescript
// frontend/src/pages/auth/MfaVerifyPage.tsx
import React, { useState } from 'react';
import { Form, Input, Button, Card, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { verifyMfa } from '@/store/slices/authSlice';
import type { RootState } from '@/store';

const MfaVerifyPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { error } = useSelector((state: RootState) => state.auth);

  const onFinish = async (values: { code: string }) => {
    setLoading(true);
    try {
      const result = await dispatch(verifyMfa({ code: values.code }) as any);
      if (verifyMfa.fulfilled.match(result)) {
        message.success('Verification successful');
        navigate('/dashboard');
      }
    } catch (error) {
      message.error('Invalid verification code');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', background: '#f0f2f5' }}>
      <Card style={{ width: 400 }}>
        <h2 style={{ textAlign: 'center', marginBottom: 24 }}>Two-Factor Authentication</h2>
        <p style={{ textAlign: 'center', marginBottom: 24, color: '#666' }}>
          Enter the 6-digit code from your authenticator app
        </p>
        <Form name="mfa-verify" onFinish={onFinish}>
          <Form.Item name="code" rules={[{ required: true, message: 'Please enter the code' }]}>
            <Input.OTP length={6} size="large" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block size="large">
              Verify
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};

export default MfaVerifyPage;
```

---

### Task 1.8: Update Header with Logout

**Files:**
- Modify: `frontend/src/components/layout/Header.tsx`

- [ ] **Step 1: Update Header with user menu and logout**

```typescript
// frontend/src/components/layout/Header.tsx
import React from 'react';
import { Layout, theme, Avatar, Dropdown, Space, message } from 'antd';
import type { MenuProps } from 'antd';
import { UserOutlined, LogoutOutlined, SettingOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { logout } from '@/store/slices/authSlice';
import type { RootState } from '@/store';

const { Header } = Layout;

const AppHeader: React.FC = () => {
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { user } = useSelector((state: RootState) => state.auth);

  const handleLogout = async () => {
    await dispatch(logout() as any);
    message.success('Logged out successfully');
    navigate('/login');
  };

  const userMenuItems: MenuProps['items'] = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: 'Profile',
      onClick: () => navigate('/profile'),
    },
    {
      key: 'settings',
      icon: <SettingOutlined />,
      label: 'Settings',
      onClick: () => navigate('/settings'),
    },
    { type: 'divider' },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: 'Logout',
      onClick: handleLogout,
    },
  ];

  return (
    <Header style={{ padding: '0 24px', background: token.colorBgContainer }}>
      <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', height: '100%' }}>
        <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
          <Space style={{ cursor: 'pointer' }}>
            <Avatar icon={<UserOutlined />} />
            <span>{user?.email || 'User'}</span>
          </Space>
        </Dropdown>
      </div>
    </Header>
  );
};

export default AppHeader;
```

---

### Task 1.9: Create Unauthorized Page

**Files:**
- Create: `frontend/src/pages/auth/UnauthorizedPage.tsx`

- [ ] **Step 1: Create UnauthorizedPage**

```typescript
// frontend/src/pages/auth/UnauthorizedPage.tsx
import React from 'react';
import { Result, Button } from 'antd';
import { useNavigate } from 'react-router-dom';

const UnauthorizedPage: React.FC = () => {
  const navigate = useNavigate();

  return (
    <Result
      status="403"
      title="Access Denied"
      subTitle="You do not have permission to access this page."
      extra={
        <Button type="primary" onClick={() => navigate('/dashboard')}>
          Back to Dashboard
        </Button>
      }
    />
  );
};

export default UnauthorizedPage;
```

---

## Phase 2: User Management (Priority: High)

### Task 2.1: Create User Service

**Files:**
- Create: `frontend/src/services/userService.ts`

- [ ] **Step 1: Create user service**

```typescript
// frontend/src/services/userService.ts
import apiClient from './apiClient';

export interface User {
  id: number;
  email: string;
  userType: string;
  status: string;
  mfaEnabled: boolean;
  companyId?: number;
  branchId?: number;
  departmentId?: number;
  createdAt: string;
}

export interface CreateUserRequest {
  email: string;
  password: string;
  userType: string;
  role?: string;
  companyId?: number;
  branchId?: number;
  departmentId?: number;
}

export const userService = {
  getUsers: async (params?: Record<string, unknown>) => {
    const response = await apiClient.get('/iam/users', { params });
    return response.data;
  },

  getUser: async (id: number) => {
    const response = await apiClient.get(`/iam/users/${id}`);
    return response.data;
  },

  createUser: async (data: CreateUserRequest) => {
    const response = await apiClient.post('/iam/users', data);
    return response.data;
  },

  updateUser: async (id: number, data: Partial<CreateUserRequest>) => {
    const response = await apiClient.put(`/iam/users/${id}`, data);
    return response.data;
  },

  activateUser: async (id: number) => {
    const response = await apiClient.post(`/iam/users/${id}/activate`);
    return response.data;
  },

  deactivateUser: async (id: number) => {
    const response = await apiClient.post(`/iam/users/${id}/deactivate`);
    return response.data;
  },

  getActiveUsers: async () => {
    const response = await apiClient.get('/iam/users/active');
    return response.data;
  },
};
```

---

### Task 2.2: Create Users Redux Slice

**Files:**
- Create: `frontend/src/store/slices/usersSlice.ts`

- [ ] **Step 1: Create usersSlice**

```typescript
// frontend/src/store/slices/usersSlice.ts
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { userService, User, CreateUserRequest } from '@/services/userService';

interface UsersState {
  users: User[];
  selectedUser: User | null;
  loading: boolean;
  error: string | null;
}

const initialState: UsersState = {
  users: [],
  selectedUser: null,
  loading: false,
  error: null,
};

export const fetchUsers = createAsyncThunk('users/fetchUsers', async () => {
  return await userService.getUsers();
});

export const fetchUser = createAsyncThunk('users/fetchUser', async (id: number) => {
  return await userService.getUser(id);
});

export const createUser = createAsyncThunk('users/createUser', async (data: CreateUserRequest) => {
  return await userService.createUser(data);
});

export const updateUser = createAsyncThunk('users/updateUser', async ({ id, data }: { id: number; data: Partial<CreateUserRequest> }) => {
  return await userService.updateUser(id, data);
});

export const activateUser = createAsyncThunk('users/activateUser', async (id: number) => {
  return await userService.activateUser(id);
});

export const deactivateUser = createAsyncThunk('users/deactivateUser', async (id: number) => {
  return await userService.deactivateUser(id);
});

const usersSlice = createSlice({
  name: 'users',
  initialState,
  reducers: {
    clearSelectedUser: (state) => {
      state.selectedUser = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchUsers.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchUsers.fulfilled, (state, action) => {
        state.loading = false;
        state.users = action.payload;
      })
      .addCase(fetchUsers.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message || 'Failed to fetch users';
      })
      .addCase(fetchUser.fulfilled, (state, action) => {
        state.selectedUser = action.payload;
      })
      .addCase(createUser.fulfilled, (state, action) => {
        state.users.push(action.payload);
      })
      .addCase(updateUser.fulfilled, (state, action) => {
        const index = state.users.findIndex((u) => u.id === action.payload.id);
        if (index !== -1) {
          state.users[index] = action.payload;
        }
      })
      .addCase(activateUser.fulfilled, (state, action) => {
        const index = state.users.findIndex((u) => u.id === action.payload.id);
        if (index !== -1) {
          state.users[index] = action.payload;
        }
      })
      .addCase(deactivateUser.fulfilled, (state, action) => {
        const index = state.users.findIndex((u) => u.id === action.payload.id);
        if (index !== -1) {
          state.users[index] = action.payload;
        }
      });
  },
});

export const { clearSelectedUser } = usersSlice.actions;
export default usersSlice.reducer;
```

---

### Task 2.3: Create UserListPage

**Files:**
- Create: `frontend/src/pages/iam/UserListPage.tsx`

- [ ] **Step 1: Create UserListPage**

```typescript
// frontend/src/pages/iam/UserListPage.tsx
import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Tag, message, Card, Input } from 'antd';
import { PlusOutlined, EditOutlined, StopOutlined, CheckOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { fetchUsers, activateUser, deactivateUser } from '@/store/slices/usersSlice';
import type { RootState } from '@/store';
import type { User } from '@/services/userService';

const UserListPage: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { users, loading } = useSelector((state: RootState) => state.users);
  const [searchText, setSearchText] = useState('');

  useEffect(() => {
    dispatch(fetchUsers() as any);
  }, [dispatch]);

  const handleActivate = async (id: number) => {
    try {
      await dispatch(activateUser(id) as any);
      message.success('User activated');
    } catch {
      message.error('Failed to activate user');
    }
  };

  const handleDeactivate = async (id: number) => {
    try {
      await dispatch(deactivateUser(id) as any);
      message.success('User deactivated');
    } catch {
      message.error('Failed to deactivate user');
    }
  };

  const filteredUsers = users.filter((user) =>
    user.email.toLowerCase().includes(searchText.toLowerCase())
  );

  const columns = [
    {
      title: 'Email',
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: 'User Type',
      dataIndex: 'userType',
      key: 'userType',
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'ACTIVE' ? 'green' : 'red'}>{status}</Tag>
      ),
    },
    {
      title: 'MFA Enabled',
      dataIndex: 'mfaEnabled',
      key: 'mfaEnabled',
      render: (enabled: boolean) => (
        <Tag color={enabled ? 'blue' : 'default'}>{enabled ? 'Yes' : 'No'}</Tag>
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: unknown, record: User) => (
        <Space>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => navigate(`/iam/users/${record.id}`)}
          >
            Edit
          </Button>
          {record.status === 'ACTIVE' ? (
            <Button
              type="link"
              danger
              icon={<StopOutlined />}
              onClick={() => handleDeactivate(record.id)}
            >
              Deactivate
            </Button>
          ) : (
            <Button
              type="link"
              icon={<CheckOutlined />}
              onClick={() => handleActivate(record.id)}
            >
              Activate
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Card
      title="User Management"
      extra={
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/iam/users/new')}>
          Add User
        </Button>
      }
    >
      <Input
        placeholder="Search users..."
        style={{ marginBottom: 16 }}
        value={searchText}
        onChange={(e) => setSearchText(e.target.value)}
      />
      <Table
        columns={columns}
        dataSource={filteredUsers}
        rowKey="id"
        loading={loading}
      />
    </Card>
  );
};

export default UserListPage;
```

---

### Task 2.4: Create UserFormPage

**Files:**
- Create: `frontend/src/pages/iam/UserFormPage.tsx`

- [ ] **Step 1: Create UserFormPage**

```typescript
// frontend/src/pages/iam/UserFormPage.tsx
import React, { useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, Form, Input, Select, Button, message } from 'antd';
import { useDispatch, useSelector } from 'react-redux';
import { createUser, fetchUser, updateUser, clearSelectedUser } from '@/store/slices/usersSlice';
import type { RootState } from '@/store';

const UserFormPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const [form] = Form.useForm();
  const { selectedUser, loading } = useSelector((state: RootState) => state.users);
  const isEdit = !!id;

  useEffect(() => {
    if (id) {
      dispatch(fetchUser(Number(id)) as any);
    }
    return () => {
      dispatch(clearSelectedUser());
    };
  }, [id, dispatch]);

  useEffect(() => {
    if (selectedUser && isEdit) {
      form.setFieldsValue(selectedUser);
    }
  }, [selectedUser, isEdit, form]);

  const onFinish = async (values: Record<string, unknown>) => {
    try {
      if (isEdit) {
        await dispatch(updateUser({ id: Number(id), data: values }) as any);
        message.success('User updated');
      } else {
        await dispatch(createUser(values as any) as any);
        message.success('User created');
      }
      navigate('/iam/users');
    } catch {
      message.error('Operation failed');
    }
  };

  return (
    <Card title={isEdit ? 'Edit User' : 'Create User'}>
      <Form form={form} layout="vertical" onFinish={onFinish}>
        <Form.Item
          name="email"
          label="Email"
          rules={[{ required: true, type: 'email' }]}
        >
          <Input />
        </Form.Item>

        {!isEdit && (
          <Form.Item
            name="password"
            label="Password"
            rules={[{ required: true, min: 8 }]}
          >
            <Input.Password />
          </Form.Item>
        )}

        <Form.Item name="userType" label="User Type" rules={[{ required: true }]}>
          <Select>
            <Select.Option value="INTERNAL">Internal</Select.Option>
            <Select.Option value="EXTERNAL">External</Select.Option>
          </Select>
        </Form.Item>

        <Form.Item name="role" label="Role">
          <Select>
            <Select.Option value="SYSTEM_ADMIN">System Admin</Select.Option>
            <Select.Option value="COMPANY_ADMIN">Company Admin</Select.Option>
            <Select.Option value="COMPANY_MAKER">Company Maker</Select.Option>
            <Select.Option value="COMPANY_CHECKER">Company Checker</Select.Option>
            <Select.Option value="COMPANY_VIEWER">Company Viewer</Select.Option>
          </Select>
        </Form.Item>

        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" loading={loading}>
              {isEdit ? 'Update' : 'Create'}
            </Button>
            <Button onClick={() => navigate('/iam/users')}>Cancel</Button>
          </Space>
        </Form.Item>
      </Form>
    </Card>
  );
};

export default UserFormPage;
```

---

### Task 2.5: Add IAM Routes to App.tsx

**Files:**
- Modify: `frontend/src/App.tsx`

- [ ] **Step 1: Add IAM routes**

```typescript
// Add imports
import UserListPage from '@/pages/iam/UserListPage';
import UserFormPage from '@/pages/iam/UserFormPage';
import UnauthorizedPage from '@/pages/auth/UnauthorizedPage';

// Add routes
<Route
  element={
    <ProtectedRoute allowedRoles={['SYSTEM_ADMIN']}>
      <AppLayout />
    </ProtectedRoute>
  }
>
  <Route path="/iam/users" element={<UserListPage />} />
  <Route path="/iam/users/new" element={<UserFormPage />} />
  <Route path="/iam/users/:id" element={<UserFormPage />} />
</Route>
```

---

### Task 2.6: Update Sidebar with Role-Based Menu

**Files:**
- Modify: `frontend/src/components/layout/Sidebar.tsx`

- [ ] **Step 1: Update Sidebar with IAM menu items**

```typescript
// frontend/src/components/layout/Sidebar.tsx
import React from 'react';
import { Layout, Menu } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import { useSelector } from 'react-redux';
import type { RootState } from '@/store';
import {
  DashboardOutlined,
  UserOutlined,
  TeamOutlined,
  HistoryOutlined,
  BankOutlined,
} from '@ant-design/icons';

const { Sider } = Layout;

const menuConfig = {
  SYSTEM_ADMIN: [
    { key: '/dashboard', icon: <DashboardOutlined />, label: 'Dashboard' },
    { key: '/iam/users', icon: <UserOutlined />, label: 'User Management' },
    { key: '/iam/roles', icon: <TeamOutlined />, label: 'Role Management' },
    { key: '/iam/activity', icon: <HistoryOutlined />, label: 'Activity' },
    { key: '/iam/thresholds', icon: <BankOutlined />, label: 'Thresholds' },
  ],
  COMPANY_ADMIN: [
    { key: '/dashboard', icon: <DashboardOutlined />, label: 'Dashboard' },
    { key: '/company/users', icon: <UserOutlined />, label: 'Users' },
    { key: '/company/thresholds', icon: <BankOutlined />, label: 'Thresholds' },
  ],
  DEFAULT: [
    { key: '/dashboard', icon: <DashboardOutlined />, label: 'Dashboard' },
  ],
};

const Sidebar: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useSelector((state: RootState) => state.auth);

  const role = user?.role || 'DEFAULT';
  const menuItems = menuConfig[role as keyof typeof menuConfig] || menuConfig.DEFAULT;

  return (
    <Sider width={250} theme="light">
      <div style={{ height: 64, display: 'flex', alignItems: 'center', justifyContent: 'center', borderBottom: '1px solid #f0f0f0' }}>
        <h2 style={{ margin: 0 }}>Banking</h2>
      </div>
      <Menu
        mode="inline"
        selectedKeys={[location.pathname]}
        items={menuItems}
        onClick={({ key }) => navigate(key)}
        style={{ borderRight: 0 }}
      />
    </Sider>
  );
};

export default Sidebar;
```

---

## Phase 3: Role Management (Priority: High)

### Task 3.1: Create Role Service

**Files:**
- Create: `frontend/src/services/roleService.ts`

- [ ] **Step 1: Create roleService**

```typescript
// frontend/src/services/roleService.ts
import apiClient from './apiClient';

export interface Role {
  id: number;
  name: string;
  description: string;
  type: 'SYSTEM' | 'CUSTOM';
  permissions: string[];
}

export interface CreateRoleRequest {
  name: string;
  description: string;
  permissions?: string[];
}

export const roleService = {
  getRoles: async () => {
    const response = await apiClient.get('/iam/roles');
    return response.data;
  },

  getRole: async (id: number) => {
    const response = await apiClient.get(`/iam/roles/${id}`);
    return response.data;
  },

  createRole: async (data: CreateRoleRequest) => {
    const response = await apiClient.post('/iam/roles', data);
    return response.data;
  },

  updateRole: async (id: number, data: CreateRoleRequest) => {
    const response = await apiClient.put(`/iam/roles/${id}`, data);
    return response.data;
  },

  deleteRole: async (id: number) => {
    const response = await apiClient.delete(`/iam/roles/${id}`);
    return response.data;
  },

  assignPermissions: async (id: number, permissions: string[]) => {
    const response = await apiClient.put(`/iam/roles/${id}/permissions`, permissions);
    return response.data;
  },

  getPermissions: async () => {
    const response = await apiClient.get('/iam/permissions');
    return response.data;
  },
};
```

---

### Task 3.2: Create RoleListPage

**Files:**
- Create: `frontend/src/pages/iam/RoleListPage.tsx`

- [ ] **Step 1: Create RoleListPage**

```typescript
// frontend/src/pages/iam/RoleListPage.tsx
import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Tag, Card, message, Modal } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { roleService, Role } from '@/services/roleService';

const RoleListPage: React.FC = () => {
  const navigate = useNavigate();
  const [roles, setRoles] = useState<Role[]>([]);
  const [loading, setLoading] = useState(false);
  const [permissions, setPermissions] = useState<string[]>([]);

  useEffect(() => {
    fetchRoles();
    fetchPermissions();
  }, []);

  const fetchRoles = async () => {
    setLoading(true);
    try {
      const data = await roleService.getRoles();
      setRoles(data);
    } catch {
      message.error('Failed to fetch roles');
    } finally {
      setLoading(false);
    }
  };

  const fetchPermissions = async () => {
    try {
      const data = await roleService.getPermissions();
      setPermissions(data.map((p: { name: string }) => p.name));
    } catch {
      // Ignore
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await roleService.deleteRole(id);
      message.success('Role deleted');
      fetchRoles();
    } catch {
      message.error('Failed to delete role');
    }
  };

  const columns = [
    { title: 'Name', dataIndex: 'name', key: 'name' },
    { title: 'Description', dataIndex: 'description', key: 'description' },
    {
      title: 'Type',
      dataIndex: 'type',
      key: 'type',
      render: (type: string) => (
        <Tag color={type === 'SYSTEM' ? 'blue' : 'green'}>{type}</Tag>
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: unknown, record: Role) => (
        <Space>
          <Button type="link" icon={<EyeOutlined />} onClick={() => navigate(`/iam/roles/${record.id}`)}>
            View
          </Button>
          {record.type === 'CUSTOM' && (
            <>
              <Button type="link" icon={<EditOutlined />} onClick={() => navigate(`/iam/roles/${record.id}/edit`)}>
                Edit
              </Button>
              <Button type="link" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record.id)}>
                Delete
              </Button>
            </>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Card
      title="Role Management"
      extra={
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/iam/roles/new')}>
          Create Role
        </Button>
      }
    >
      <Table columns={columns} dataSource={roles} rowKey="id" loading={loading} />
    </Card>
  );
};

export default RoleListPage;
```

---

### Task 3.3: Create RoleFormPage

**Files:**
- Create: `frontend/src/pages/iam/RoleFormPage.tsx`

- [ ] **Step 1: Create RoleFormPage**

```typescript
// frontend/src/pages/iam/RoleFormPage.tsx
import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, Form, Input, Select, Button, message, Checkbox } from 'antd';
import { roleService, CreateRoleRequest } from '@/services/roleService';

const RoleFormPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [permissions, setPermissions] = useState<string[]>([]);
  const isEdit = !!id;

  useEffect(() => {
    roleService.getPermissions().then((data) => {
      setPermissions(data.map((p: { name: string }) => p.name));
    });

    if (id) {
      roleService.getRole(Number(id)).then((role) => {
        form.setFieldsValue(role);
      });
    }
  }, [id, form]);

  const onFinish = async (values: Record<string, unknown>) => {
    setLoading(true);
    try {
      if (isEdit) {
        await roleService.updateRole(Number(id), values as CreateRoleRequest);
        message.success('Role updated');
      } else {
        await roleService.createRole(values as CreateRoleRequest);
        message.success('Role created');
      }
      navigate('/iam/roles');
    } catch {
      message.error('Operation failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card title={isEdit ? 'Edit Role' : 'Create Role'}>
      <Form form={form} layout="vertical" onFinish={onFinish}>
        <Form.Item name="name" label="Role Name" rules={[{ required: true }]}>
          <Input />
        </Form.Item>
        <Form.Item name="description" label="Description">
          <Input.TextArea />
        </Form.Item>
        <Form.Item name="permissions" label="Permissions">
          <Checkbox.Group options={permissions} />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" loading={loading}>
              {isEdit ? 'Update' : 'Create'}
            </Button>
            <Button onClick={() => navigate('/iam/roles')}>Cancel</Button>
          </Space>
        </Form.Item>
      </Form>
    </Card>
  );
};

export default RoleFormPage;
```

---

## Phase 4: Activity & Thresholds (Priority: Medium)

### Task 4.1: Create Activity Service

**Files:**
- Create: `frontend/src/services/activityService.ts`

- [ ] **Step 1: Create activityService**

```typescript
// frontend/src/services/activityService.ts
import apiClient from './apiClient';

export interface LoginHistory {
  id: number;
  userId: number;
  email: string;
  action: string;
  ipAddress: string;
  timestamp: string;
  success: boolean;
}

export interface FailedLoginAttempt {
  id: number;
  email: string;
  ipAddress: string;
  timestamp: string;
  attempts: number;
}

export const activityService = {
  getLoginHistory: async (params?: Record<string, unknown>) => {
    const response = await apiClient.get('/iam/activity/login-history', { params });
    return response.data;
  },

  getLoginHistoryByUser: async (userId: number) => {
    const response = await apiClient.get(`/iam/activity/login-history/user/${userId}`);
    return response.data;
  },

  getFailedLogins: async (email: string) => {
    const response = await apiClient.get('/iam/activity/failed-logins', { params: { email } });
    return response.data;
  },

  getFailedLoginCount: async (email: string, minutes?: number) => {
    const response = await apiClient.get('/iam/activity/failed-logins/count', { params: { email, minutes } });
    return response.data;
  },
};
```

---

### Task 4.2: Create ActivityPage

**Files:**
- Create: `frontend/src/pages/iam/ActivityPage.tsx`

- [ ] **Step 1: Create ActivityPage**

```typescript
// frontend/src/pages/iam/ActivityPage.tsx
import React, { useEffect, useState } from 'react';
import { Card, Table, Tabs, DatePicker, Input, Tag } from 'antd';
import { activityService, LoginHistory, FailedLoginAttempt } from '@/services/activityService';
import type { PageResponse } from '@/types';

const { RangePicker } = DatePicker;

const ActivityPage: React.FC = () => {
  const [loginHistory, setLoginHistory] = useState<LoginHistory[]>([]);
  const [failedLogins, setFailedLogins] = useState<FailedLoginAttempt[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchLoginHistory();
  }, []);

  const fetchLoginHistory = async () => {
    setLoading(true);
    try {
      const data = await activityService.getLoginHistory();
      setLoginHistory(data.content || data);
    } catch {
      // Handle error
    } finally {
      setLoading(false);
    }
  };

  const loginColumns = [
    { title: 'User', dataIndex: 'email', key: 'email' },
    { title: 'IP Address', dataIndex: 'ipAddress', key: 'ipAddress' },
    { title: 'Timestamp', dataIndex: 'timestamp', key: 'timestamp' },
    {
      title: 'Status',
      dataIndex: 'success',
      key: 'success',
      render: (success: boolean) => (
        <Tag color={success ? 'green' : 'red'}>{success ? 'Success' : 'Failed'}</Tag>
      ),
    },
  ];

  const failedColumns = [
    { title: 'Email', dataIndex: 'email', key: 'email' },
    { title: 'IP Address', dataIndex: 'ipAddress', key: 'ipAddress' },
    { title: 'Attempts', dataIndex: 'attempts', key: 'attempts' },
    { title: 'Timestamp', dataIndex: 'timestamp', key: 'timestamp' },
  ];

  const items = [
    {
      key: 'login-history',
      label: 'Login History',
      children: (
        <Table columns={loginColumns} dataSource={loginHistory} rowKey="id" loading={loading} />
      ),
    },
    {
      key: 'failed-logins',
      label: 'Failed Logins',
      children: (
        <Table columns={failedColumns} dataSource={failedLogins} rowKey="id" loading={loading} />
      ),
    },
  ];

  return (
    <Card title="Activity Monitoring">
      <Tabs items={items} />
    </Card>
  );
};

export default ActivityPage;
```

---

### Task 4.3: Create Threshold Service

**Files:**
- Create: `frontend/src/services/thresholdService.ts`

- [ ] **Step 1: Create thresholdService**

```typescript
// frontend/src/services/thresholdService.ts
import apiClient from './apiClient';

export interface Threshold {
  id: number;
  role: string;
  amount: number;
  currency: string;
  type: string;
}

export interface CreateThresholdRequest {
  role: string;
  amount: number;
  currency: string;
  type: string;
}

export const thresholdService = {
  getThresholds: async () => {
    const response = await apiClient.get('/iam/thresholds');
    return response.data;
  },

  getThreshold: async (id: number) => {
    const response = await apiClient.get(`/iam/thresholds/${id}`);
    return response.data;
  },

  createThreshold: async (data: CreateThresholdRequest) => {
    const response = await apiClient.post('/iam/thresholds', data);
    return response.data;
  },

  updateThreshold: async (id: number, amount: number) => {
    const response = await apiClient.put(`/iam/thresholds/${id}`, null, { params: { threshold: amount } });
    return response.data;
  },

  deleteThreshold: async (id: number) => {
    const response = await apiClient.delete(`/iam/thresholds/${id}`);
    return response.data;
  },
};
```

---

### Task 4.4: Create ThresholdListPage

**Files:**
- Create: `frontend/src/pages/iam/ThresholdListPage.tsx`

- [ ] **Step 1: Create ThresholdListPage**

```typescript
// frontend/src/pages/iam/ThresholdListPage.tsx
import React, { useEffect, useState } from 'react';
import { Card, Table, Button, Space, Modal, message } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { thresholdService, Threshold, CreateThresholdRequest } from '@/services/thresholdService';
import { useDispatch } from 'react-redux';

const ThresholdListPage: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const [thresholds, setThresholds] = useState<Threshold[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchThresholds();
  }, []);

  const fetchThresholds = async () => {
    setLoading(true);
    try {
      const data = await thresholdService.getThresholds();
      setThresholds(data);
    } catch {
      message.error('Failed to fetch thresholds');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await thresholdService.deleteThreshold(id);
      message.success('Threshold deleted');
      fetchThresholds();
    } catch {
      message.error('Failed to delete threshold');
    }
  };

  const columns = [
    { title: 'Role', dataIndex: 'role', key: 'role' },
    { title: 'Amount', dataIndex: 'amount', key: 'amount', render: (amt: number, rec: Threshold) => `${rec.currency} ${amt.toLocaleString()}` },
    { title: 'Type', dataIndex: 'type', key: 'type' },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: unknown, record: Threshold) => (
        <Space>
          <Button type="link" icon={<EditOutlined />} onClick={() => navigate(`/iam/thresholds/${record.id}/edit`)}>
            Edit
          </Button>
          <Button type="link" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record.id)}>
            Delete
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <Card
      title="Approval Thresholds"
      extra={
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/iam/thresholds/new')}>
          Add Threshold
        </Button>
      }
    >
      <Table columns={columns} dataSource={thresholds} rowKey="id" loading={loading} />
    </Card>
  );
};

export default ThresholdListPage;
```

---

## Phase 5: Profile & Settings (Priority: Low)

### Task 5.1: Create ProfilePage

**Files:**
- Create: `frontend/src/pages/profile/ProfilePage.tsx`

- [ ] **Step 1: Create ProfilePage**

```typescript
// frontend/src/pages/profile/ProfilePage.tsx
import React from 'react';
import { Card, Descriptions, Button, Tag, Space, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import type { RootState } from '@/store';
import { disableMfa } from '@/store/slices/authSlice';
import { authService } from '@/services/authService';

const ProfilePage: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { user } = useSelector((state: RootState) => state.auth);

  const handleDisableMfa = async () => {
    try {
      await authService.disableMfa();
      message.success('MFA disabled');
    } catch {
      message.error('Failed to disable MFA');
    }
  };

  return (
    <Card title="Profile">
      <Descriptions bordered column={1}>
        <Descriptions.Item label="Email">{user?.email}</Descriptions.Item>
        <Descriptions.Item label="Role">{user?.role}</Descriptions.Item>
        <Descriptions.Item label="MFA Status">
          <Tag color={user?.mfaEnabled ? 'green' : 'default'}>
            {user?.mfaEnabled ? 'Enabled' : 'Disabled'}
          </Tag>
        </Descriptions.Item>
      </Descriptions>
      <div style={{ marginTop: 16 }}>
        <Space>
          <Button type="primary" onClick={() => navigate('/profile/change-password')}>
            Change Password
          </Button>
          {!user?.mfaEnabled && (
            <Button onClick={() => navigate('/profile/mfa-setup')}>Enable MFA</Button>
          )}
          {user?.mfaEnabled && <Button danger onClick={handleDisableMfa}>Disable MFA</Button>}
        </Space>
      </div>
    </Card>
  );
};

export default ProfilePage;
```

---

### Task 5.2: Add Profile Routes

**Files:**
- Modify: `frontend/src/App.tsx`

- [ ] **Step 1: Add profile routes**

```typescript
// Add imports
import ProfilePage from '@/pages/profile/ProfilePage';

// Add routes (protected, any authenticated user)
<Route
  element={
    <ProtectedRoute>
      <AppLayout />
    </ProtectedRoute>
  }
>
  <Route path="/profile" element={<ProfilePage />} />
  <Route path="/profile/change-password" element={<ChangePasswordPage />} />
</Route>
```

---

## Phase 6: Company Admin Pages (Priority: Medium)

### Task 6.1: Create CompanyUserListPage

**Files:**
- Create: `frontend/src/pages/company/CompanyUserListPage.tsx`

- [ ] **Step 1: Create CompanyUserListPage**

```typescript
// frontend/src/pages/company/CompanyUserListPage.tsx
import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Tag, Card, message } from 'antd';
import { PlusOutlined, StopOutlined, CheckOutlined, EditOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { userService, User } from '@/services/userService';

const CompanyUserListPage: React.FC = () => {
  const navigate = useNavigate();
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const data = await userService.getUsers();
      setUsers(data);
    } catch {
      message.error('Failed to fetch users');
    } finally {
      setLoading(false);
    }
  };

  const handleDeactivate = async (id: number) => {
    try {
      await userService.deactivateUser(id);
      message.success('User deactivated');
      fetchUsers();
    } catch {
      message.error('Failed to deactivate user');
    }
  };

  const handleActivate = async (id: number) => {
    try {
      await userService.activateUser(id);
      message.success('User activated');
      fetchUsers();
    } catch {
      message.error('Failed to activate user');
    }
  };

  const columns = [
    { title: 'Email', dataIndex: 'email', key: 'email' },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => <Tag color={status === 'ACTIVE' ? 'green' : 'red'}>{status}</Tag>,
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: unknown, record: User) => (
        <Space>
          <Button type="link" icon={<EditOutlined />} onClick={() => navigate(`/company/users/${record.id}`)}>
            Edit
          </Button>
          {record.status === 'ACTIVE' ? (
            <Button type="link" danger icon={<StopOutlined />} onClick={() => handleDeactivate(record.id)}>
              Deactivate
            </Button>
          ) : (
            <Button type="link" icon={<CheckOutlined />} onClick={() => handleActivate(record.id)}>
              Activate
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Card
      title="Company Users"
      extra={
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/company/users/invite')}>
          Invite User
        </Button>
      }
    >
      <Table columns={columns} dataSource={users} rowKey="id" loading={loading} />
    </Card>
  );
};

export default CompanyUserListPage;
```

---

## Verification Commands

After implementing all phases, run these commands to verify:

```bash
# Build frontend
cd frontend && npm run build

# Run frontend tests
cd frontend && npm run test

# Run with coverage
cd frontend && npm run test:coverage
```

---

## Summary

This implementation plan covers:
- **Phase 1**: Authentication (Login, MFA, Protected Routes, Logout)
- **Phase 2**: User Management (List, Create, Edit, Activate/Deactivate)
- **Phase 3**: Role Management (CRUD, Permissions)
- **Phase 4**: Activity Monitoring & Thresholds
- **Phase 5**: Profile & Settings
- **Phase 6**: Company Admin Pages

Total: ~25 tasks spanning all IAM functionality
