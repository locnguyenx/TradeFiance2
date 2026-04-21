# Customer Management Pages Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create customer management pages (list, detail, forms), Redux slice with async thunks, and component tests

**Architecture:** Ant Design components for UI, Redux Toolkit for state management, React Testing Library for tests. Pages will be in `src/pages/customers/` following the task requirements.

**Tech Stack:** React 18, TypeScript, Ant Design 6, Redux Toolkit, React Testing Library, React Router 7

---

### Task 1: Enhance Redux Slice with Async Thunks

**Files:**
- Modify: `src/store/slices/customerSlice.ts`
- Test: `src/store/slices/customerSlice.test.ts`

- [ ] **Step 1: Write failing tests for async thunks**

```typescript
import { configureStore } from '@reduxjs/toolkit';
import { describe, it, expect, beforeEach } from 'vitest';
import customerReducer, {
  fetchCustomers,
  createCorporate,
  createSME,
  createIndividual,
  updateCustomer,
  setCustomers,
  setSelectedCustomer,
  setLoading,
  setError,
} from '@/store/slices/customerSlice';

describe('customerSlice async thunks', () => {
  let store: ReturnType<typeof configureStore>;

  beforeEach(() => {
    store = configureStore({
      reducer: { customer: customerReducer },
    });
  });

  it('should have fetchCustomers thunk', () => {
    expect(typeof fetchCustomers).toBe('function');
  });

  it('should have createCorporate thunk', () => {
    expect(typeof createCorporate).toBe('function');
  });

  it('should have createSME thunk', () => {
    expect(typeof createSME).toBe('function');
  });

  it('should have createIndividual thunk', () => {
    expect(typeof createIndividual).toBe('function');
  });

  it('should have updateCustomer thunk', () => {
    expect(typeof updateCustomer).toBe('function');
  });

  it('should set loading state', () => {
    store.dispatch(setLoading(true));
    expect(store.getState().customer.loading).toBe(true);
  });

  it('should set error state', () => {
    store.dispatch(setError('Test error'));
    expect(store.getState().customer.error).toBe('Test error');
  });

  it('should set customers state', () => {
    const mockCustomers = [
      { id: 1, customerNumber: 'C001', customerType: 'INDIVIDUAL' as const, status: 'ACTIVE' as const, createdAt: '', createdBy: '', updatedAt: '', updatedBy: '', firstName: 'John', lastName: 'Doe', dateOfBirth: '1990-01-01', email: 'john@test.com', phoneNumber: '123', idNumber: 'ID123' }
    ];
    store.dispatch(setCustomers(mockCustomers as any));
    expect(store.getState().customer.customers).toHaveLength(1);
  });

  it('should set selected customer state', () => {
    const mockCustomer = { id: 1, customerNumber: 'C001', customerType: 'INDIVIDUAL' as const, status: 'ACTIVE' as const, createdAt: '', createdBy: '', updatedAt: '', updatedBy: '', firstName: 'John', lastName: 'Doe', dateOfBirth: '1990-01-01', email: 'john@test.com', phoneNumber: '123', idNumber: 'ID123' };
    store.dispatch(setSelectedCustomer(mockCustomer as any));
    expect(store.getState().customer.selectedCustomer).not.toBeNull();
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npm test -- src/store/slices/customerSlice.test.ts --run`
Expected: Tests fail because async thunks don't exist yet

- [ ] **Step 3: Implement async thunks in customerSlice.ts**

```typescript
import { createSlice, createAsyncThunk, type PayloadAction } from '@reduxjs/toolkit';
import { customerService } from '@/services/customerService';
import type { CustomerVariant, IndividualCustomer, SMECustomer, CorporateCustomer } from '@/types';

interface CustomerState {
  customers: CustomerVariant[];
  selectedCustomer: CustomerVariant | null;
  loading: boolean;
  error: string | null;
}

const initialState: CustomerState = {
  customers: [],
  selectedCustomer: null,
  loading: false,
  error: null,
};

export const fetchCustomers = createAsyncThunk(
  'customer/fetchAll',
  async (_, { rejectWithValue }) => {
    try {
      return await customerService.getAll();
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch customers');
    }
  }
);

export const createCorporate = createAsyncThunk(
  'customer/createCorporate',
  async (data: Omit<CorporateCustomer, 'id' | 'customerNumber' | 'createdAt' | 'createdBy' | 'updatedAt' | 'updatedBy'>, { rejectWithValue }) => {
    try {
      const response = await customerService.create(data as any);
      return response;
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to create corporate customer');
    }
  }
);

export const createSME = createAsyncThunk(
  'customer/createSME',
  async (data: Omit<SMECustomer, 'id' | 'customerNumber' | 'createdAt' | 'createdBy' | 'updatedAt' | 'updatedBy'>, { rejectWithValue }) => {
    try {
      const response = await customerService.create(data as any);
      return response;
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to create SME customer');
    }
  }
);

export const createIndividual = createAsyncThunk(
  'customer/createIndividual',
  async (data: Omit<IndividualCustomer, 'id' | 'customerNumber' | 'createdAt' | 'createdBy' | 'updatedAt' | 'updatedBy'>, { rejectWithValue }) => {
    try {
      const response = await customerService.create(data as any);
      return response;
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to create individual customer');
    }
  }
);

export const updateCustomer = createAsyncThunk(
  'customer/update',
  async ({ id, data }: { id: number; data: Partial<CustomerVariant> }, { rejectWithValue }) => {
    try {
      const response = await customerService.update(id, data as any);
      return response;
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to update customer');
    }
  }
);

const customerSlice = createSlice({
  name: 'customer',
  initialState,
  reducers: {
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.loading = action.payload;
    },
    setError: (state, action: PayloadAction<string | null>) => {
      state.error = action.payload;
    },
    setCustomers: (state, action: PayloadAction<CustomerVariant[]>) => {
      state.customers = action.payload;
    },
    setSelectedCustomer: (state, action: PayloadAction<CustomerVariant | null>) => {
      state.selectedCustomer = action.payload;
    },
    clearError: (state) => {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchCustomers.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchCustomers.fulfilled, (state, action) => {
        state.loading = false;
        state.customers = action.payload;
      })
      .addCase(fetchCustomers.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      })
      .addCase(createCorporate.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(createCorporate.fulfilled, (state) => {
        state.loading = false;
      })
      .addCase(createCorporate.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      })
      .addCase(createSME.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(createSME.fulfilled, (state) => {
        state.loading = false;
      })
      .addCase(createSME.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      })
      .addCase(createIndividual.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(createIndividual.fulfilled, (state) => {
        state.loading = false;
      })
      .addCase(createIndividual.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      })
      .addCase(updateCustomer.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(updateCustomer.fulfilled, (state, action) => {
        state.loading = false;
        state.selectedCustomer = action.payload;
      })
      .addCase(updateCustomer.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      });
  },
});

export const { setLoading, setError, setCustomers, setSelectedCustomer, clearError } = customerSlice.actions;
export default customerSlice.reducer;
```

- [ ] **Step 4: Update customerService.ts with create and update methods**

```typescript
import apiClient from './apiClient';
import type { CustomerVariant } from '@/types';

export const customerService = {
  getAll: async (): Promise<CustomerVariant[]> => {
    const response = await apiClient.get('/customers');
    return response.data;
  },

  getById: async (id: number): Promise<CustomerVariant> => {
    const response = await apiClient.get(`/customers/${id}`);
    return response.data;
  },

  create: async (data: Partial<CustomerVariant>): Promise<CustomerVariant> => {
    const response = await apiClient.post('/customers', data);
    return response.data;
  },

  update: async (id: number, data: Partial<CustomerVariant>): Promise<CustomerVariant> => {
    const response = await apiClient.put(`/customers/${id}`, data);
    return response.data;
  },
};
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `npm test -- src/store/slices/customerSlice.test.ts --run`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/store/slices/customerSlice.ts src/store/slices/customerSlice.test.ts src/services/customerService.ts
git commit -m "feat(customers): add async thunks to Redux slice"
```

---

### Task 2: Create CustomerListPage Component

**Files:**
- Create: `src/pages/customers/CustomerListPage.tsx`
- Create: `src/pages/customers/CustomerListPage.test.tsx`
- Modify: `src/App.tsx` (add routes)

- [ ] **Step 1: Write failing test**

```typescript
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import { MemoryRouter } from 'react-router-dom';
import CustomerListPage from './CustomerListPage';
import customerReducer from '@/store/slices/customerSlice';
import type { IndividualCustomer, SMECustomer, CorporateCustomer } from '@/types';

const mockCustomers: (IndividualCustomer | SMECustomer | CorporateCustomer)[] = [
  {
    id: 1,
    customerNumber: 'C001',
    customerType: 'INDIVIDUAL',
    status: 'ACTIVE',
    createdAt: '2024-01-01',
    createdBy: 'admin',
    updatedAt: '2024-01-01',
    updatedBy: 'admin',
    firstName: 'John',
    lastName: 'Doe',
    dateOfBirth: '1990-01-01',
    email: 'john@test.com',
    phoneNumber: '123456',
    idNumber: 'ID001',
  },
  {
    id: 2,
    customerNumber: 'C002',
    customerType: 'SME',
    status: 'ACTIVE',
    createdAt: '2024-01-01',
    createdBy: 'admin',
    updatedAt: '2024-01-01',
    updatedBy: 'admin',
    companyName: 'Acme Corp',
    registrationNumber: 'REG001',
    incorporationDate: '2020-01-01',
    email: 'acme@test.com',
    phoneNumber: '789',
    industry: 'Technology',
    numberOfEmployees: 50,
  },
];

const createTestStore = (preloadedState = {}) => {
  return configureStore({
    reducer: {
      customer: customerReducer,
    },
    preloadedState,
  });
};

const renderWithProviders = (store = createTestStore()) => {
  return render(
    <Provider store={store}>
      <MemoryRouter>
        <CustomerListPage />
      </MemoryRouter>
    </Provider>
  );
};

describe('CustomerListPage', () => {
  it('should render loading state initially', () => {
    const store = createTestStore({
      customer: { customers: [], selectedCustomer: null, loading: true, error: null },
    });
    renderWithProviders(store);
    expect(document.body.textContent).toContain('Loading');
  });

  it('should render customer table when loaded', async () => {
    const store = createTestStore({
      customer: { customers: mockCustomers, selectedCustomer: null, loading: false, error: null },
    });
    renderWithProviders(store);
    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeInTheDocument();
      expect(screen.getByText('Acme Corp')).toBeInTheDocument();
    });
  });

  it('should render search input', () => {
    const store = createTestStore({
      customer: { customers: mockCustomers, selectedCustomer: null, loading: false, error: null },
    });
    renderWithProviders(store);
    expect(screen.getByPlaceholderText(/search/i)).toBeInTheDocument();
  });

  it('should render type filter dropdown', () => {
    const store = createTestStore({
      customer: { customers: mockCustomers, selectedCustomer: null, loading: false, error: null },
    });
    renderWithProviders(store);
    expect(screen.getByText('Type')).toBeInTheDocument();
  });

  it('should render status filter dropdown', () => {
    const store = createTestStore({
      customer: { customers: mockCustomers, selectedCustomer: null, loading: false, error: null },
    });
    renderWithProviders(store);
    expect(screen.getByText('Status')).toBeInTheDocument();
  });

  it('should render pagination controls', () => {
    const store = createTestStore({
      customer: { customers: mockCustomers, selectedCustomer: null, loading: false, error: null },
    });
    renderWithProviders(store);
    expect(screen.getByText(/rows per page/i)).toBeInTheDocument();
  });

  it('should display customer type badges', async () => {
    const store = createTestStore({
      customer: { customers: mockCustomers, selectedCustomer: null, loading: false, error: null },
    });
    renderWithProviders(store);
    await waitFor(() => {
      expect(screen.getByText('INDIVIDUAL')).toBeInTheDocument();
      expect(screen.getByText('SME')).toBeInTheDocument();
    });
  });

  it('should display customer status badges', async () => {
    const store = createTestStore({
      customer: { customers: mockCustomers, selectedCustomer: null, loading: false, error: null },
    });
    renderWithProviders(store);
    await waitFor(() => {
      const statusBadges = screen.getAllByText('ACTIVE');
      expect(statusBadges.length).toBeGreaterThan(0);
    });
  });

  it('should render "Create Customer" button', () => {
    const store = createTestStore({
      customer: { customers: mockCustomers, selectedCustomer: null, loading: false, error: null },
    });
    renderWithProviders(store);
    expect(screen.getByText(/create customer/i)).toBeInTheDocument();
  });

  it('should render error message when error exists', () => {
    const store = createTestStore({
      customer: { customers: [], selectedCustomer: null, loading: false, error: 'Test error message' },
    });
    renderWithProviders(store);
    expect(screen.getByText('Test error message')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/pages/customers/CustomerListPage.test.tsx --run`
Expected: FAIL - component doesn't exist

- [ ] **Step 3: Create CustomerListPage component**

```typescript
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Table, Input, Select, Button, Space, Tag, Card, Alert, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { SearchOutlined, PlusOutlined } from '@ant-design/icons';
import type { RootState, AppDispatch } from '@/store';
import { fetchCustomers } from '@/store/slices/customerSlice';
import type { CustomerVariant, CustomerType, CustomerStatus } from '@/types';

const { Text } = Typography;

const CustomerListPage: React.FC = () => {
  const dispatch = useDispatch<AppDispatch>();
  const navigate = useNavigate();
  const { customers, loading, error } = useSelector((state: RootState) => state.customer);
  
  const [searchText, setSearchText] = useState('');
  const [typeFilter, setTypeFilter] = useState<CustomerType | undefined>();
  const [statusFilter, setStatusFilter] = useState<CustomerStatus | undefined>();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  useEffect(() => {
    dispatch(fetchCustomers());
  }, [dispatch]);

  const filteredCustomers = customers.filter((customer) => {
    const matchesSearch = searchText === '' || 
      customer.customerNumber.toLowerCase().includes(searchText.toLowerCase()) ||
      ('firstName' in customer && `${customer.firstName} ${customer.lastName}`.toLowerCase().includes(searchText.toLowerCase())) ||
      ('companyName' in customer && customer.companyName.toLowerCase().includes(searchText.toLowerCase()));
    
    const matchesType = !typeFilter || customer.customerType === typeFilter;
    const matchesStatus = !statusFilter || customer.status === statusFilter;
    
    return matchesSearch && matchesType && matchesStatus;
  });

  const paginatedCustomers = filteredCustomers.slice((page - 1) * pageSize, page * pageSize);

  const handleCreateCustomer = () => {
    navigate('/customers/new');
  };

  const handleViewCustomer = (id: number) => {
    navigate(`/customers/${id}`);
  };

  const getDisplayName = (customer: CustomerVariant): string => {
    if ('firstName' in customer) {
      return `${customer.firstName} ${customer.lastName}`;
    }
    return customer.companyName;
  };

  const getTypeBadgeColor = (type: CustomerType): string => {
    switch (type) {
      case 'INDIVIDUAL': return 'blue';
      case 'SME': return 'green';
      case 'CORPORATE': return 'purple';
      default: return 'default';
    }
  };

  const getStatusBadgeColor = (status: CustomerStatus): string => {
    switch (status) {
      case 'ACTIVE': return 'success';
      case 'PENDING': return 'warning';
      case 'SUSPENDED': return 'error';
      case 'CLOSED': return 'default';
      default: return 'default';
    }
  };

  const columns: ColumnsType<CustomerVariant> = [
    {
      title: 'Customer Number',
      dataIndex: 'customerNumber',
      key: 'customerNumber',
      width: 150,
    },
    {
      title: 'Name',
      key: 'name',
      width: 200,
      render: (_, record) => getDisplayName(record),
    },
    {
      title: 'Type',
      dataIndex: 'customerType',
      key: 'customerType',
      width: 120,
      render: (type: CustomerType) => (
        <Tag color={getTypeBadgeColor(type)}>{type}</Tag>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (status: CustomerStatus) => (
        <Tag color={getStatusBadgeColor(status)}>{status}</Tag>
      ),
    },
    {
      title: 'Email',
      key: 'email',
      width: 200,
      render: (_, record) => record.email,
    },
    {
      title: 'Created',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 120,
      render: (date: string) => new Date(date).toLocaleDateString(),
    },
    {
      title: 'Action',
      key: 'action',
      width: 100,
      render: (_, record) => (
        <Button type="link" onClick={() => handleViewCustomer(record.id)}>
          View
        </Button>
      ),
    },
  ];

  if (loading && customers.length === 0) {
    return (
      <Card>
        <Text>Loading...</Text>
      </Card>
    );
  }

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="large">
      <Space style={{ width: '100%', justifyContent: 'space-between' }}>
        <Space>
          <Input
            placeholder="Search by name or customer number"
            prefix={<SearchOutlined />}
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            style={{ width: 300 }}
            allowClear
          />
          <Select
            placeholder="Type"
            value={typeFilter}
            onChange={setTypeFilter}
            allowClear
            style={{ width: 150 }}
          >
            <Select.Option value="INDIVIDUAL">Individual</Select.Option>
            <Select.Option value="SME">SME</Select.Option>
            <Select.Option value="CORPORATE">Corporate</Select.Option>
          </Select>
          <Select
            placeholder="Status"
            value={statusFilter}
            onChange={setStatusFilter}
            allowClear
            style={{ width: 150 }}
          >
            <Select.Option value="PENDING">Pending</Select.Option>
            <Select.Option value="ACTIVE">Active</Select.Option>
            <Select.Option value="SUSPENDED">Suspended</Select.Option>
            <Select.Option value="CLOSED">Closed</Select.Option>
          </Select>
        </Space>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreateCustomer}>
          Create Customer
        </Button>
      </Space>

      {error && <Alert message={error} type="error" showIcon />}

      <Table
        columns={columns}
        dataSource={paginatedCustomers}
        rowKey="id"
        loading={loading}
        pagination={{
          current: page,
          pageSize: pageSize,
          total: filteredCustomers.length,
          showSizeChanger: true,
          showTotal: (total, range) => `${range[0]}-${range[1]} of ${total} customers`,
          onChange: (newPage, newPageSize) => {
            setPage(newPage);
            setPageSize(newPageSize);
          },
        }}
        onRow={(record) => ({
          onClick: () => handleViewCustomer(record.id),
          style: { cursor: 'pointer' },
        })}
      />
    </Space>
  );
};

export default CustomerListPage;
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npm test -- src/pages/customers/CustomerListPage.test.tsx --run`
Expected: PASS

- [ ] **Step 5: Update App.tsx with routes**

```typescript
import { Routes, Route, Navigate } from 'react-router-dom';
import { AppLayout } from '@/components/layout';
import { ConfigProvider } from 'antd';
import CustomerListPage from '@/pages/customers/CustomerListPage';

function App() {
  return (
    <ConfigProvider>
      <AppLayout>
        <Routes>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<div>Dashboard</div>} />
          <Route path="/customers" element={<CustomerListPage />} />
          <Route path="/customers/new" element={<div>Create Customer</div>} />
          <Route path="/customers/:id" element={<div>Customer Detail</div>} />
          <Route path="/accounts" element={<div>Accounts</div>} />
          <Route path="/transactions" element={<div>Transactions</div>} />
        </Routes>
      </AppLayout>
    </ConfigProvider>
  );
}

export default App;
```

- [ ] **Step 6: Commit**

```bash
git add src/pages/customers/CustomerListPage.tsx src/pages/customers/CustomerListPage.test.tsx src/App.tsx
git commit -m "feat(customers): add CustomerListPage with search, filter, pagination"
```

---

### Task 3: Create CustomerDetailPage Component

**Files:**
- Create: `src/pages/customers/CustomerDetailPage.tsx`
- Create: `src/pages/customers/CustomerDetailPage.test.tsx`
- Modify: `src/App.tsx` (update route to use component)

- [ ] **Step 1: Write failing test**

```typescript
import { render, screen, waitFor } from '@testing-library/react';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import CustomerDetailPage from './CustomerDetailPage';
import customerReducer from '@/store/slices/customerSlice';
import type { IndividualCustomer } from '@/types';

const mockCustomer: IndividualCustomer = {
  id: 1,
  customerNumber: 'C001',
  customerType: 'INDIVIDUAL',
  status: 'ACTIVE',
  createdAt: '2024-01-01',
  createdBy: 'admin',
  updatedAt: '2024-01-01',
  updatedBy: 'admin',
  firstName: 'John',
  lastName: 'Doe',
  dateOfBirth: '1990-01-01',
  email: 'john@test.com',
  phoneNumber: '123456',
  idNumber: 'ID001',
};

const createTestStore = (preloadedState = {}) => {
  return configureStore({
    reducer: {
      customer: customerReducer,
    },
    preloadedState,
  });
};

const renderWithProviders = (store = createTestStore()) => {
  return render(
    <Provider store={store}>
      <MemoryRouter initialEntries={['/customers/1']}>
        <Routes>
          <Route path="/customers/:id" element={<CustomerDetailPage />} />
        </Routes>
      </MemoryRouter>
    </Provider>
  );
};

describe('CustomerDetailPage', () => {
  it('should render Basic Info tab', () => {
    const store = createTestStore({
      customer: { customers: [mockCustomer], selectedCustomer: mockCustomer, loading: false, error: null },
    });
    renderWithProviders(store);
    expect(screen.getByText('Basic Information')).toBeInTheDocument();
  });

  it('should render KYC tab', () => {
    const store = createTestStore({
      customer: { customers: [mockCustomer], selectedCustomer: mockCustomer, loading: false, error: null },
    });
    renderWithProviders(store);
    expect(screen.getByText('KYC')).toBeInTheDocument();
  });

  it('should render Authorizations tab', () => {
    const store = createTestStore({
      customer: { customers: [mockCustomer], selectedCustomer: mockCustomer, loading: false, error: null },
    });
    renderWithProviders(store);
    expect(screen.getByText('Authorizations')).toBeInTheDocument();
  });

  it('should display customer number', () => {
    const store = createTestStore({
      customer: { customers: [mockCustomer], selectedCustomer: mockCustomer, loading: false, error: null },
    });
    renderWithProviders(store);
    expect(screen.getByText('C001')).toBeInTheDocument();
  });

  it('should display customer name', () => {
    const store = createTestStore({
      customer: { customers: [mockCustomer], selectedCustomer: mockCustomer, loading: false, error: null },
    });
    renderWithProviders(store);
    expect(screen.getByText('John Doe')).toBeInTheDocument();
  });

  it('should display customer email', () => {
    const store = createTestStore({
      customer: { customers: [mockCustomer], selectedCustomer: mockCustomer, loading: false, error: null },
    });
    renderWithProviders(store);
    expect(screen.getByText('john@test.com')).toBeInTheDocument();
  });

  it('should display customer type badge', () => {
    const store = createTestStore({
      customer: { customers: [mockCustomer], selectedCustomer: mockCustomer, loading: false, error: null },
    });
    renderWithProviders(store);
    expect(screen.getByText('INDIVIDUAL')).toBeInTheDocument();
  });

  it('should display customer status badge', () => {
    const store = createTestStore({
      customer: { customers: [mockCustomer], selectedCustomer: mockCustomer, loading: false, error: null },
    });
    renderWithProviders(store);
    expect(screen.getByText('ACTIVE')).toBeInTheDocument();
  });

  it('should render back button', () => {
    const store = createTestStore({
      customer: { customers: [mockCustomer], selectedCustomer: mockCustomer, loading: false, error: null },
    });
    renderWithProviders(store);
    expect(screen.getByText(/back/i)).toBeInTheDocument();
  });

  it('should render edit button', () => {
    const store = createTestStore({
      customer: { customers: [mockCustomer], selectedCustomer: mockCustomer, loading: false, error: null },
    });
    renderWithProviders(store);
    expect(screen.getByText(/edit/i)).toBeInTheDocument();
  });

  it('should show loading when customer is loading', () => {
    const store = createTestStore({
      customer: { customers: [], selectedCustomer: null, loading: true, error: null },
    });
    renderWithProviders(store);
    expect(document.body.textContent).toContain('Loading');
  });

  it('should show error when there is an error', () => {
    const store = createTestStore({
      customer: { customers: [], selectedCustomer: null, loading: false, error: 'Customer not found' },
    });
    renderWithProviders(store);
    expect(screen.getByText('Customer not found')).toBeInTheDocument();
  });

  it('should render basic info fields for individual customer', () => {
    const store = createTestStore({
      customer: { customers: [mockCustomer], selectedCustomer: mockCustomer, loading: false, error: null },
    });
    renderWithProviders(store);
    expect(screen.getByText('First Name')).toBeInTheDocument();
    expect(screen.getByText('Last Name')).toBeInTheDocument();
    expect(screen.getByText('Date of Birth')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/pages/customers/CustomerDetailPage.test.tsx --run`
Expected: FAIL - component doesn't exist

- [ ] **Step 3: Create CustomerDetailPage component**

```typescript
import React, { useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Card, Tabs, Tag, Button, Space, Descriptions, Alert, Typography, Spin } from 'antd';
import { ArrowLeftOutlined, EditOutlined } from '@ant-design/icons';
import type { RootState, AppDispatch } from '@/store';
import { fetchCustomers, setSelectedCustomer } from '@/store/slices/customerSlice';
import type { CustomerVariant, CustomerType, CustomerStatus } from '@/types';

const { Text, Title } = Typography;

const CustomerDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const dispatch = useDispatch<AppDispatch>();
  const { customers, selectedCustomer, loading, error } = useSelector(
    (state: RootState) => state.customer
  );

  useEffect(() => {
    if (!selectedCustomer || selectedCustomer.id !== Number(id)) {
      const customer = customers.find((c) => c.id === Number(id));
      if (customer) {
        dispatch(setSelectedCustomer(customer));
      } else {
        dispatch(fetchCustomers());
      }
    }
  }, [id, customers, selectedCustomer, dispatch]);

  const handleBack = () => {
    navigate('/customers');
  };

  const handleEdit = () => {
    navigate(`/customers/${id}/edit`);
  };

  const getDisplayName = (customer: CustomerVariant): string => {
    if ('firstName' in customer) {
      return `${customer.firstName} ${customer.lastName}`;
    }
    return customer.companyName;
  };

  const getTypeBadgeColor = (type: CustomerType): string => {
    switch (type) {
      case 'INDIVIDUAL': return 'blue';
      case 'SME': return 'green';
      case 'CORPORATE': return 'purple';
      default: return 'default';
    }
  };

  const getStatusBadgeColor = (status: CustomerStatus): string => {
    switch (status) {
      case 'ACTIVE': return 'success';
      case 'PENDING': return 'warning';
      case 'SUSPENDED': return 'error';
      case 'CLOSED': return 'default';
      default: return 'default';
    }
  };

  const renderBasicInfo = () => {
    if (!selectedCustomer) return null;

    return (
      <Descriptions bordered column={2}>
        <Descriptions.Item label="Customer Number">{selectedCustomer.customerNumber}</Descriptions.Item>
        <Descriptions.Item label="Customer Type">
          <Tag color={getTypeBadgeColor(selectedCustomer.customerType)}>{selectedCustomer.customerType}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="Status">
          <Tag color={getStatusBadgeColor(selectedCustomer.status)}>{selectedCustomer.status}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="Email">{selectedCustomer.email}</Descriptions.Item>
        <Descriptions.Item label="Phone Number">{selectedCustomer.phoneNumber}</Descriptions.Item>
        <Descriptions.Item label="Created At">
          {new Date(selectedCustomer.createdAt).toLocaleDateString()}
        </Descriptions.Item>
        
        {'firstName' in selectedCustomer && (
          <>
            <Descriptions.Item label="First Name">{selectedCustomer.firstName}</Descriptions.Item>
            <Descriptions.Item label="Last Name">{selectedCustomer.lastName}</Descriptions.Item>
            <Descriptions.Item label="Date of Birth">{selectedCustomer.dateOfBirth}</Descriptions.Item>
            <Descriptions.Item label="ID Number">{selectedCustomer.idNumber}</Descriptions.Item>
          </>
        )}
        
        {'companyName' in selectedCustomer && (
          <>
            <Descriptions.Item label="Company Name">{selectedCustomer.companyName}</Descriptions.Item>
            <Descriptions.Item label="Registration Number">{selectedCustomer.registrationNumber}</Descriptions.Item>
            <Descriptions.Item label="Incorporation Date">{selectedCustomer.incorporationDate}</Descriptions.Item>
            <Descriptions.Item label="Industry">{selectedCustomer.industry}</Descriptions.Item>
          </>
        )}

        {'numberOfEmployees' in selectedCustomer && (
          <Descriptions.Item label="Number of Employees">{selectedCustomer.numberOfEmployees}</Descriptions.Item>
        )}

        {'employeeCountRange' in selectedCustomer && (
          <Descriptions.Item label="Employee Count Range">{selectedCustomer.employeeCountRange}</Descriptions.Item>
        )}

        {'annualRevenue' in selectedCustomer && (
          <Descriptions.Item label="Annual Revenue">
            {selectedCustomer.annualRevenue} {selectedCustomer.revenueCurrency}
          </Descriptions.Item>
        )}
      </Descriptions>
    );
  };

  const renderKYC = () => {
    return (
      <Space direction="vertical" style={{ width: '100%' }} size="large">
        <Alert message="KYC verification status will be displayed here" type="info" />
        <Descriptions bordered column={2}>
          <Descriptions.Item label="ID Verification">Pending</Descriptions.Item>
          <Descriptions.Item label="Address Verification">Pending</Descriptions.Item>
          <Descriptions.Item label="Source of Funds">Pending</Descriptions.Item>
          <Descriptions.Item label="Risk Assessment">Pending</Descriptions.Item>
        </Descriptions>
      </Space>
    );
  };

  const renderAuthorizations = () => {
    return (
      <Space direction="vertical" style={{ width: '100%' }} size="large">
        <Alert message="Authorization history will be displayed here" type="info" />
        <Descriptions bordered column={2}>
          <Descriptions.Item label="Account Signatory">Yes</Descriptions.Item>
          <Descriptions.Item label="Transaction Authority">View Only</Descriptions.Item>
          <Descriptions.Item label="Account Management">Full Access</Descriptions.Item>
          <Descriptions.Item label="API Access">Enabled</Descriptions.Item>
        </Descriptions>
      </Space>
    );
  };

  if (loading && !selectedCustomer) {
    return (
      <Card>
        <Space style={{ width: '100%', justifyContent: 'center' }}>
          <Spin size="large" />
          <Text>Loading...</Text>
        </Space>
      </Card>
    );
  }

  if (error) {
    return (
      <Card>
        <Alert message={error} type="error" showIcon />
        <Button onClick={handleBack} style={{ marginTop: 16 }}>Back</Button>
      </Card>
    );
  }

  if (!selectedCustomer) {
    return (
      <Card>
        <Alert message="Customer not found" type="warning" showIcon />
        <Button onClick={handleBack} style={{ marginTop: 16 }}>Back</Button>
      </Card>
    );
  }

  const items = [
    { key: 'basic', label: 'Basic Information', children: renderBasicInfo() },
    { key: 'kyc', label: 'KYC', children: renderKYC() },
    { key: 'auth', label: 'Authorizations', children: renderAuthorizations() },
  ];

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="large">
      <Space style={{ width: '100%', justifyContent: 'space-between' }}>
        <Button icon={<ArrowLeftOutlined />} onClick={handleBack}>
          Back
        </Button>
        <Button type="primary" icon={<EditOutlined />} onClick={handleEdit}>
          Edit
        </Button>
      </Space>

      <Card>
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <Title level={4}>{getDisplayName(selectedCustomer)}</Title>
          <Space>
            <Tag color={getTypeBadgeColor(selectedCustomer.customerType)}>{selectedCustomer.customerType}</Tag>
            <Tag color={getStatusBadgeColor(selectedCustomer.status)}>{selectedCustomer.status}</Tag>
          </Space>
        </Space>
      </Card>

      <Card>
        <Tabs items={items} />
      </Card>
    </Space>
  );
};

export default CustomerDetailPage;
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npm test -- src/pages/customers/CustomerDetailPage.test.tsx --run`
Expected: PASS

- [ ] **Step 5: Update App.tsx with proper route**

```typescript
import CustomerDetailPage from '@/pages/customers/CustomerDetailPage';

// In Routes:
<Route path="/customers/:id" element={<CustomerDetailPage />} />
```

- [ ] **Step 6: Commit**

```bash
git add src/pages/customers/CustomerDetailPage.tsx src/pages/customers/CustomerDetailPage.test.tsx src/App.tsx
git commit -m "feat(customers): add CustomerDetailPage with tabbed interface"
```

---

### Task 4: Create IndividualCustomerForm Component

**Files:**
- Create: `src/pages/customers/IndividualCustomerForm.tsx`
- Create: `src/pages/customers/IndividualCustomerForm.test.tsx`

- [ ] **Step 1: Write failing test**

```typescript
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import { MemoryRouter } from 'react-router-dom';
import IndividualCustomerForm from './IndividualCustomerForm';
import customerReducer from '@/store/slices/customerSlice';

const createTestStore = () => {
  return configureStore({
    reducer: {
      customer: customerReducer,
    },
    preloadedState: {
      customer: { customers: [], selectedCustomer: null, loading: false, error: null },
    },
  });
};

const renderWithProviders = (store = createTestStore()) => {
  return render(
    <Provider store={store}>
      <MemoryRouter>
        <IndividualCustomerForm />
      </MemoryRouter>
    </Provider>
  );
};

describe('IndividualCustomerForm', () => {
  it('should render form with all fields', () => {
    renderWithProviders();
    expect(screen.getByLabelText(/first name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/last name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/date of birth/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/phone number/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/id number/i)).toBeInTheDocument();
  });

  it('should render submit button', () => {
    renderWithProviders();
    expect(screen.getByText(/submit/i)).toBeInTheDocument();
  });

  it('should render cancel button', () => {
    renderWithProviders();
    expect(screen.getByText(/cancel/i)).toBeInTheDocument();
  });

  it('should accept input in all fields', async () => {
    const user = userEvent.setup();
    renderWithProviders();

    await user.type(screen.getByLabelText(/first name/i), 'John');
    await user.type(screen.getByLabelText(/last name/i), 'Doe');
    await user.type(screen.getByLabelText(/email/i), 'john@example.com');
    await user.type(screen.getByLabelText(/phone number/i), '1234567890');
    await user.type(screen.getByLabelText(/id number/i), 'ID123456');

    expect(screen.getByLabelText(/first name/i)).toHaveValue('John');
    expect(screen.getByLabelText(/last name/i)).toHaveValue('Doe');
    expect(screen.getByLabelText(/email/i)).toHaveValue('john@example.com');
  });

  it('should show validation errors for required fields', async () => {
    const user = userEvent.setup();
    renderWithProviders();

    await user.click(screen.getByText(/submit/i));

    await waitFor(() => {
      expect(screen.getByText(/first name is required/i)).toBeInTheDocument();
      expect(screen.getByText(/last name is required/i)).toBeInTheDocument();
    });
  });

  it('should show validation error for invalid email', async () => {
    const user = userEvent.setup();
    renderWithProviders();

    await user.type(screen.getByLabelText(/email/i), 'invalid-email');
    await user.click(screen.getByText(/submit/i));

    await waitFor(() => {
      expect(screen.getByText(/invalid email/i)).toBeInTheDocument();
    });
  });

  it('should display loading state when submitting', () => {
    const store = createTestStore();
    renderWithProviders(store);
    expect(screen.queryByText(/submitting/i)).not.toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/pages/customers/IndividualCustomerForm.test.tsx --run`
Expected: FAIL - component doesn't exist

- [ ] **Step 3: Create IndividualCustomerForm component**

```typescript
import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Form, Input, DatePicker, Button, Card, Space, message } from 'antd';
import type { FormProps } from 'antd';
import type { RootState, AppDispatch } from '@/store';
import { createIndividual, clearError } from '@/store/slices/customerSlice';
import type { IndividualCustomer } from '@/types';

interface IndividualCustomerFormProps {
  initialValues?: Partial<IndividualCustomer>;
  isEdit?: boolean;
}

type FormValues = Omit<IndividualCustomer, 'id' | 'customerNumber' | 'customerType' | 'status' | 'createdAt' | 'createdBy' | 'updatedAt' | 'updatedBy'>;

const IndividualCustomerForm: React.FC<IndividualCustomerFormProps> = ({
  initialValues,
  isEdit = false,
}) => {
  const dispatch = useDispatch<AppDispatch>();
  const navigate = useNavigate();
  const { loading, error } = useSelector((state: RootState) => state.customer);
  const [form] = Form.useForm<FormValues>();

  useEffect(() => {
    if (error) {
      message.error(error);
      dispatch(clearError());
    }
  }, [error, dispatch]);

  useEffect(() => {
    if (initialValues) {
      form.setFieldsValue(initialValues);
    }
  }, [initialValues, form]);

  const onFinish: FormProps<FormValues>['onFinish'] = async (values) => {
    try {
      await dispatch(createIndividual(values)).unwrap();
      message.success('Individual customer created successfully');
      navigate('/customers');
    } catch (err) {
      message.error('Failed to create customer');
    }
  };

  const onCancel = () => {
    navigate('/customers');
  };

  return (
    <Card title={isEdit ? 'Edit Individual Customer' : 'Create Individual Customer'}>
      <Form
        form={form}
        layout="vertical"
        onFinish={onFinish}
        initialValues={initialValues}
      >
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <Space style={{ width: '100%' }} size="large">
            <Form.Item
              name="firstName"
              label="First Name"
              rules={[{ required: true, message: 'First name is required' }]}
              style={{ flex: 1 }}
            >
              <Input />
            </Form.Item>

            <Form.Item
              name="lastName"
              label="Last Name"
              rules={[{ required: true, message: 'Last name is required' }]}
              style={{ flex: 1 }}
            >
              <Input />
            </Form.Item>
          </Space>

          <Space style={{ width: '100%' }} size="large">
            <Form.Item
              name="dateOfBirth"
              label="Date of Birth"
              rules={[{ required: true, message: 'Date of birth is required' }]}
              style={{ flex: 1 }}
            >
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>

            <Form.Item
              name="email"
              label="Email"
              rules={[
                { required: true, message: 'Email is required' },
                { type: 'email', message: 'Invalid email' },
              ]}
              style={{ flex: 1 }}
            >
              <Input />
            </Form.Item>
          </Space>

          <Space style={{ width: '100%' }} size="large">
            <Form.Item
              name="phoneNumber"
              label="Phone Number"
              rules={[{ required: true, message: 'Phone number is required' }]}
              style={{ flex: 1 }}
            >
              <Input />
            </Form.Item>

            <Form.Item
              name="idNumber"
              label="ID Number"
              rules={[{ required: true, message: 'ID number is required' }]}
              style={{ flex: 1 }}
            >
              <Input />
            </Form.Item>
          </Space>

          <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
            <Button onClick={onCancel}>Cancel</Button>
            <Button type="primary" htmlType="submit" loading={loading}>
              {loading ? 'Submitting...' : 'Submit'}
            </Button>
          </Space>
        </Space>
      </Form>
    </Card>
  );
};

export default IndividualCustomerForm;
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npm test -- src/pages/customers/IndividualCustomerForm.test.tsx --run`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/pages/customers/IndividualCustomerForm.tsx src/pages/customers/IndividualCustomerForm.test.tsx
git commit -m "feat(customers): add IndividualCustomerForm with validation"
```

---

### Task 5: Create SMECustomerForm Component

**Files:**
- Create: `src/pages/customers/SMECustomerForm.tsx`
- Create: `src/pages/customers/SMECustomerForm.test.tsx`

- [ ] **Step 1: Write failing test**

```typescript
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import { MemoryRouter } from 'react-router-dom';
import SMECustomerForm from './SMECustomerForm';
import customerReducer from '@/store/slices/customerSlice';

const createTestStore = () => {
  return configureStore({
    reducer: {
      customer: customerReducer,
    },
    preloadedState: {
      customer: { customers: [], selectedCustomer: null, loading: false, error: null },
    },
  });
};

const renderWithProviders = (store = createTestStore()) => {
  return render(
    <Provider store={store}>
      <MemoryRouter>
        <SMECustomerForm />
      </MemoryRouter>
    </Provider>
  );
};

describe('SMECustomerForm', () => {
  it('should render form with all fields', () => {
    renderWithProviders();
    expect(screen.getByLabelText(/company name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/registration number/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/incorporation date/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/industry/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/phone number/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/number of employees/i)).toBeInTheDocument();
  });

  it('should render submit button', () => {
    renderWithProviders();
    expect(screen.getByText(/submit/i)).toBeInTheDocument();
  });

  it('should render cancel button', () => {
    renderWithProviders();
    expect(screen.getByText(/cancel/i)).toBeInTheDocument();
  });

  it('should accept input in company fields', async () => {
    const user = userEvent.setup();
    renderWithProviders();

    await user.type(screen.getByLabelText(/company name/i), 'Acme Corp');
    await user.type(screen.getByLabelText(/registration number/i), 'REG123');
    await user.type(screen.getByLabelText(/email/i), 'acme@test.com');
    await user.type(screen.getByLabelText(/industry/i), 'Technology');

    expect(screen.getByLabelText(/company name/i)).toHaveValue('Acme Corp');
  });

  it('should show validation errors for required fields', async () => {
    const user = userEvent.setup();
    renderWithProviders();

    await user.click(screen.getByText(/submit/i));

    await waitFor(() => {
      expect(screen.getByText(/company name is required/i)).toBeInTheDocument();
    });
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/pages/customers/SMECustomerForm.test.tsx --run`
Expected: FAIL - component doesn't exist

- [ ] **Step 3: Create SMECustomerForm component**

```typescript
import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Form, Input, DatePicker, Button, Card, Space, message, InputNumber } from 'antd';
import type { FormProps } from 'antd';
import type { RootState, AppDispatch } from '@/store';
import { createSME, clearError } from '@/store/slices/customerSlice';
import type { SMECustomer } from '@/types';

interface SMECustomerFormProps {
  initialValues?: Partial<SMECustomer>;
  isEdit?: boolean;
}

type FormValues = Omit<SMECustomer, 'id' | 'customerNumber' | 'customerType' | 'status' | 'createdAt' | 'createdBy' | 'updatedAt' | 'updatedBy'>;

const SMECustomerForm: React.FC<SMECustomerFormProps> = ({
  initialValues,
  isEdit = false,
}) => {
  const dispatch = useDispatch<AppDispatch>();
  const navigate = useNavigate();
  const { loading, error } = useSelector((state: RootState) => state.customer);
  const [form] = Form.useForm<FormValues>();

  useEffect(() => {
    if (error) {
      message.error(error);
      dispatch(clearError());
    }
  }, [error, dispatch]);

  useEffect(() => {
    if (initialValues) {
      form.setFieldsValue(initialValues);
    }
  }, [initialValues, form]);

  const onFinish: FormProps<FormValues>['onFinish'] = async (values) => {
    try {
      await dispatch(createSME(values)).unwrap();
      message.success('SME customer created successfully');
      navigate('/customers');
    } catch (err) {
      message.error('Failed to create customer');
    }
  };

  const onCancel = () => {
    navigate('/customers');
  };

  return (
    <Card title={isEdit ? 'Edit SME Customer' : 'Create SME Customer'}>
      <Form
        form={form}
        layout="vertical"
        onFinish={onFinish}
        initialValues={initialValues}
      >
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <Space style={{ width: '100%' }} size="large">
            <Form.Item
              name="companyName"
              label="Company Name"
              rules={[{ required: true, message: 'Company name is required' }]}
              style={{ flex: 1 }}
            >
              <Input />
            </Form.Item>

            <Form.Item
              name="registrationNumber"
              label="Registration Number"
              rules={[{ required: true, message: 'Registration number is required' }]}
              style={{ flex: 1 }}
            >
              <Input />
            </Form.Item>
          </Space>

          <Space style={{ width: '100%' }} size="large">
            <Form.Item
              name="incorporationDate"
              label="Incorporation Date"
              rules={[{ required: true, message: 'Incorporation date is required' }]}
              style={{ flex: 1 }}
            >
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>

            <Form.Item
              name="industry"
              label="Industry"
              rules={[{ required: true, message: 'Industry is required' }]}
              style={{ flex: 1 }}
            >
              <Input />
            </Form.Item>
          </Space>

          <Space style={{ width: '100%' }} size="large">
            <Form.Item
              name="numberOfEmployees"
              label="Number of Employees"
              rules={[{ required: true, message: 'Number of employees is required' }]}
              style={{ flex: 1 }}
            >
              <InputNumber min={1} style={{ width: '100%' }} />
            </Form.Item>

            <Form.Item
              name="email"
              label="Email"
              rules={[
                { required: true, message: 'Email is required' },
                { type: 'email', message: 'Invalid email' },
              ]}
              style={{ flex: 1 }}
            >
              <Input />
            </Form.Item>
          </Space>

          <Form.Item
            name="phoneNumber"
            label="Phone Number"
            rules={[{ required: true, message: 'Phone number is required' }]}
          >
            <Input />
          </Form.Item>

          <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
            <Button onClick={onCancel}>Cancel</Button>
            <Button type="primary" htmlType="submit" loading={loading}>
              {loading ? 'Submitting...' : 'Submit'}
            </Button>
          </Space>
        </Space>
      </Form>
    </Card>
  );
};

export default SMECustomerForm;
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npm test -- src/pages/customers/SMECustomerForm.test.tsx --run`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/pages/customers/SMECustomerForm.tsx src/pages/customers/SMECustomerForm.test.tsx
git commit -m "feat(customers): add SMECustomerForm with validation"
```

---

### Task 6: Create CorporateCustomerForm Component

**Files:**
- Create: `src/pages/customers/CorporateCustomerForm.tsx`
- Create: `src/pages/customers/CorporateCustomerForm.test.tsx`

- [ ] **Step 1: Write failing test**

```typescript
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import { MemoryRouter } from 'react-router-dom';
import CorporateCustomerForm from './CorporateCustomerForm';
import customerReducer from '@/store/slices/customerSlice';

const createTestStore = () => {
  return configureStore({
    reducer: {
      customer: customerReducer,
    },
    preloadedState: {
      customer: { customers: [], selectedCustomer: null, loading: false, error: null },
    },
  });
};

const renderWithProviders = (store = createTestStore()) => {
  return render(
    <Provider store={store}>
      <MemoryRouter>
        <CorporateCustomerForm />
      </MemoryRouter>
    </Provider>
  );
};

describe('CorporateCustomerForm', () => {
  it('should render form with all fields', () => {
    renderWithProviders();
    expect(screen.getByLabelText(/company name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/registration number/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/incorporation date/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/industry/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/phone number/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/employee count range/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/annual revenue/i)).toBeInTheDocument();
  });

  it('should render submit button', () => {
    renderWithProviders();
    expect(screen.getByText(/submit/i)).toBeInTheDocument();
  });

  it('should render cancel button', () => {
    renderWithProviders();
    expect(screen.getByText(/cancel/i)).toBeInTheDocument();
  });

  it('should accept input in company fields', async () => {
    const user = userEvent.setup();
    renderWithProviders();

    await user.type(screen.getByLabelText(/company name/i), 'Acme Corp');
    await user.type(screen.getByLabelText(/registration number/i), 'REG123');
    await user.type(screen.getByLabelText(/email/i), 'acme@test.com');

    expect(screen.getByLabelText(/company name/i)).toHaveValue('Acme Corp');
  });

  it('should show validation errors for required fields', async () => {
    const user = userEvent.setup();
    renderWithProviders();

    await user.click(screen.getByText(/submit/i));

    await waitFor(() => {
      expect(screen.getByText(/company name is required/i)).toBeInTheDocument();
    });
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/pages/customers/CorporateCustomerForm.test.tsx --run`
Expected: FAIL - component doesn't exist

- [ ] **Step 3: Create CorporateCustomerForm component**

```typescript
import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Form, Input, DatePicker, Button, Card, Space, message, InputNumber, Select } from 'antd';
import type { FormProps } from 'antd';
import type { RootState, AppDispatch } from '@/store';
import { createCorporate, clearError } from '@/store/slices/customerSlice';
import type { CorporateCustomer } from '@/types';

interface CorporateCustomerFormProps {
  initialValues?: Partial<CorporateCustomer>;
  isEdit?: boolean;
}

type FormValues = Omit<CorporateCustomer, 'id' | 'customerNumber' | 'customerType' | 'status' | 'createdAt' | 'createdBy' | 'updatedAt' | 'updatedBy'>;

const employeeCountRanges = [
  '1-50',
  '51-100',
  '101-250',
  '251-500',
  '501-1000',
  '1001-5000',
  '5001-10000',
  '10000+',
];

const currencies = ['USD', 'EUR', 'GBP', 'JPY', 'CNY', 'INR'];

const CorporateCustomerForm: React.FC<CorporateCustomerFormProps> = ({
  initialValues,
  isEdit = false,
}) => {
  const dispatch = useDispatch<AppDispatch>();
  const navigate = useNavigate();
  const { loading, error } = useSelector((state: RootState) => state.customer);
  const [form] = Form.useForm<FormValues>();

  useEffect(() => {
    if (error) {
      message.error(error);
      dispatch(clearError());
    }
  }, [error, dispatch]);

  useEffect(() => {
    if (initialValues) {
      form.setFieldsValue(initialValues);
    }
  }, [initialValues, form]);

  const onFinish: FormProps<FormValues>['onFinish'] = async (values) => {
    try {
      await dispatch(createCorporate(values)).unwrap();
      message.success('Corporate customer created successfully');
      navigate('/customers');
    } catch (err) {
      message.error('Failed to create customer');
    }
  };

  const onCancel = () => {
    navigate('/customers');
  };

  return (
    <Card title={isEdit ? 'Edit Corporate Customer' : 'Create Corporate Customer'}>
      <Form
        form={form}
        layout="vertical"
        onFinish={onFinish}
        initialValues={initialValues}
      >
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <Space style={{ width: '100%' }} size="large">
            <Form.Item
              name="companyName"
              label="Company Name"
              rules={[{ required: true, message: 'Company name is required' }]}
              style={{ flex: 1 }}
            >
              <Input />
            </Form.Item>

            <Form.Item
              name="registrationNumber"
              label="Registration Number"
              rules={[{ required: true, message: 'Registration number is required' }]}
              style={{ flex: 1 }}
            >
              <Input />
            </Form.Item>
          </Space>

          <Space style={{ width: '100%' }} size="large">
            <Form.Item
              name="incorporationDate"
              label="Incorporation Date"
              rules={[{ required: true, message: 'Incorporation date is required' }]}
              style={{ flex: 1 }}
            >
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>

            <Form.Item
              name="industry"
              label="Industry"
              rules={[{ required: true, message: 'Industry is required' }]}
              style={{ flex: 1 }}
            >
              <Input />
            </Form.Item>
          </Space>

          <Space style={{ width: '100%' }} size="large">
            <Form.Item
              name="employeeCountRange"
              label="Employee Count Range"
              rules={[{ required: true, message: 'Employee count range is required' }]}
              style={{ flex: 1 }}
            >
              <Select>
                {employeeCountRanges.map((range) => (
                  <Select.Option key={range} value={range}>
                    {range}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>

            <Form.Item
              name="email"
              label="Email"
              rules={[
                { required: true, message: 'Email is required' },
                { type: 'email', message: 'Invalid email' },
              ]}
              style={{ flex: 1 }}
            >
              <Input />
            </Form.Item>
          </Space>

          <Space style={{ width: '100%' }} size="large">
            <Form.Item
              name="phoneNumber"
              label="Phone Number"
              rules={[{ required: true, message: 'Phone number is required' }]}
              style={{ flex: 1 }}
            >
              <Input />
            </Form.Item>

            <Form.Item
              name="revenueCurrency"
              label="Revenue Currency"
              rules={[{ required: true, message: 'Currency is required' }]}
              style={{ flex: 1 }}
            >
              <Select>
                {currencies.map((currency) => (
                  <Select.Option key={currency} value={currency}>
                    {currency}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
          </Space>

          <Form.Item
            name="annualRevenue"
            label="Annual Revenue"
            rules={[{ required: true, message: 'Annual revenue is required' }]}
          >
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>

          <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
            <Button onClick={onCancel}>Cancel</Button>
            <Button type="primary" htmlType="submit" loading={loading}>
              {loading ? 'Submitting...' : 'Submit'}
            </Button>
          </Space>
        </Space>
      </Form>
    </Card>
  );
};

export default CorporateCustomerForm;
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npm test -- src/pages/customers/CorporateCustomerForm.test.tsx --run`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/pages/customers/CorporateCustomerForm.tsx src/pages/customers/CorporateCustomerForm.test.tsx
git commit -m "feat(customers): add CorporateCustomerForm with validation"
```

---

### Task 7: Final Build and Verification

- [ ] **Step 1: Run npm run build to verify no TypeScript errors**

Run: `npm run build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run all tests to ensure everything passes**

Run: `npm test -- --run`
Expected: All tests pass

- [ ] **Step 3: Commit final changes**

```bash
git add -A
git commit -m "feat(customers): complete customer management pages with tests"
```

---

## Summary

**Files Created:**
- `src/pages/customers/CustomerListPage.tsx` - List page with search, filter, pagination
- `src/pages/customers/CustomerListPage.test.tsx` - Tests for list page
- `src/pages/customers/CustomerDetailPage.tsx` - Detail page with tabs
- `src/pages/customers/CustomerDetailPage.test.tsx` - Tests for detail page
- `src/pages/customers/IndividualCustomerForm.tsx` - Individual customer form
- `src/pages/customers/IndividualCustomerForm.test.tsx` - Tests for individual form
- `src/pages/customers/SMECustomerForm.tsx` - SME customer form
- `src/pages/customers/SMECustomerForm.test.tsx` - Tests for SME form
- `src/pages/customers/CorporateCustomerForm.tsx` - Corporate customer form
- `src/pages/customers/CorporateCustomerForm.test.tsx` - Tests for corporate form
- `src/store/slices/customerSlice.ts` - Redux slice with async thunks
- `src/store/slices/customerSlice.test.ts` - Tests for Redux slice
- `src/services/customerService.ts` - Updated service with create/update methods

**Files Modified:**
- `src/App.tsx` - Added customer routes
- `src/types/customer.types.ts` - Types already defined

**Dependencies:**
- React Testing Library (userEvent)
- Vitest (for testing)
