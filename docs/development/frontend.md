# InvoiceMe - Frontend Architecture

## Table of Contents
- [Overview](#overview)
- [Technology Stack](#technology-stack)
- [Architecture Pattern: MVVM](#architecture-pattern-mvvm)
- [Project Structure](#project-structure)
- [Core Libraries](#core-libraries)
- [Routing Strategy](#routing-strategy)
- [State Management](#state-management)
- [API Integration](#api-integration)
- [Form Handling](#form-handling)
- [UI Components](#ui-components)
- [Authentication Flow](#authentication-flow)
- [Error Handling](#error-handling)
- [Key Principles](#key-principles)

---

## Overview

The InvoiceMe frontend is a React-based single-page application (SPA) that provides a clean, responsive interface for managing customers, invoices, and payments. The architecture follows the Model-View-ViewModel (MVVM) pattern with clear separation of concerns.

**Core Features:**
- Customer management (create, update, delete, list)
- Invoice management with line items
- Payment tracking
- Authentication (login/signup)
- Optimistic locking UI feedback
- Context-aware action buttons

---

## Technology Stack

### Core Framework
- **React 18+** - UI framework with hooks
- **TypeScript** - Type safety and developer experience
- **Vite** - Build tool and dev server

### Styling
- **Tailwind CSS** - Utility-first CSS framework
- **Tailwind Forms Plugin** - Better form styling defaults
- **Custom Design System** - Consistent spacing, colors, typography

### Data & State
- **React Query (TanStack Query)** - Server state management and caching
- **React Context** - Minimal UI state (auth, theme, modals)

### Routing
- **React Router v6** - Browser history mode for clean URLs

### Forms & Validation
- **Manual form handling** - Controlled inputs with React state
- **Manual validation** - Inline validation matching backend rules

---

## Architecture Pattern: MVVM

### Model (Data Layer)
**Location:** `src/models/`

Represents the shape of our data as it comes from the backend API.

```typescript
// src/models/Customer.ts
export interface Customer {
  // Entity ID and audit fields
  id: string;
  createdAt: string;
  createdBy: string;
  lastModifiedAt: string;
  lastModifiedBy: string;
  version: number;

  // Customer fields
  companyName: string;
  contactFirstName: string | null;
  contactLastName: string | null;
  email: string;
  phone: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  city: string | null;
  state: string | null;
  zipCode: string | null;
  country: string | null;

  // Read-only computed fields
  draftInvoiceCount: number;
  sentInvoiceCount: number;
  paidInvoiceCount: number;
  totalOutstanding: string; // BigDecimal as string
}

export interface CustomerListItem {
  id: string;
  companyName: string;
  email: string;
  totalOutstanding: string;
}

export interface CreateCustomerRequest {
  companyName: string;
  email: string;
  contactFirstName?: string;
  contactLastName?: string;
  phone?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  zipCode?: string;
  country?: string;
}

export interface UpdateCustomerRequest extends CreateCustomerRequest {
  id: string;
  version: number;
}

export interface DeleteCustomerRequest {
  id: string;
  version: number;
}
```

**Responsibilities:**
- Define TypeScript interfaces matching backend DTOs
- No business logic
- Pure data structures
- Export types for use across application

### View (Presentation Layer)
**Location:** `src/views/` and `src/components/`

React components that render UI. Views are organized by feature.

```typescript
// src/views/customers/CustomerListView.tsx
import { CustomerListViewModel } from './CustomerListViewModel';

export const CustomerListView: React.FC = () => {
  const vm = CustomerListViewModel();

  return (
    <div className="container mx-auto p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold">Customers</h1>
        <button
          onClick={vm.handleCreateNew}
          className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
        >
          Create Customer
        </button>
      </div>

      {vm.isLoading && <div>Loading...</div>}
      {vm.isError && <div className="text-red-600">{vm.errorMessage}</div>}

      {vm.customers && (
        <table className="w-full border-collapse">
          <thead>
            <tr className="border-b">
              <th className="text-left p-3">Company Name</th>
              <th className="text-left p-3">Email</th>
              <th className="text-right p-3">Outstanding</th>
              <th className="text-right p-3">Actions</th>
            </tr>
          </thead>
          <tbody>
            {vm.customers.map((customer) => (
              <tr key={customer.id} className="border-b hover:bg-gray-50">
                <td className="p-3">{customer.companyName}</td>
                <td className="p-3">{customer.email || '—'}</td>
                <td className="p-3 text-right">${customer.totalOutstanding}</td>
                <td className="p-3 text-right">
                  <button
                    onClick={() => vm.handleEdit(customer.id)}
                    className="text-blue-600 hover:underline"
                  >
                    Edit
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
};
```

**Responsibilities:**
- Render UI based on ViewModel state
- Handle user interactions (clicks, typing)
- Delegate all logic to ViewModel
- NO business logic or API calls
- Pure presentation

### ViewModel (Logic Layer)
**Location:** `src/viewmodels/`

Custom React hooks that contain all UI logic, state management, and API interactions.

```typescript
// src/viewmodels/CustomerListViewModel.ts
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { customerApi } from '../api/customerApi';
import { CustomerListItem } from '../models/Customer';

export const CustomerListViewModel = () => {
  const navigate = useNavigate();

  // Query for customer list
  const {
    data: customers,
    isLoading,
    isError,
    error,
  } = useQuery<CustomerListItem[]>({
    queryKey: ['customers', 'list'],
    queryFn: customerApi.listAll,
  });

  // Computed properties
  const errorMessage = isError
    ? error instanceof Error
      ? error.message
      : 'Failed to load customers'
    : null;

  // Actions
  const handleCreateNew = () => {
    navigate('/customers/new');
  };

  const handleEdit = (customerId: string) => {
    navigate(`/customers/${customerId}`);
  };

  // Expose state and actions to view
  return {
    customers,
    isLoading,
    isError,
    errorMessage,
    handleCreateNew,
    handleEdit,
  };
};
```

**Responsibilities:**
- Manage component-level state
- Coordinate with React Query for server state
- Handle user actions (navigation, mutations)
- Expose clean interface to View
- Transform data for presentation
- Validation logic

---

## Project Structure

```
invoice-me-frontend/
├── public/
│   └── index.html
├── src/
│   ├── main.tsx                 # App entry point
│   ├── App.tsx                  # Root component with routing
│   │
│   ├── models/                  # Data models (TypeScript interfaces)
│   │   ├── Customer.ts
│   │   ├── Invoice.ts
│   │   ├── LineItem.ts
│   │   ├── Payment.ts
│   │   ├── User.ts
│   │   └── ApiResponse.ts
│   │
│   ├── api/                     # API client layer
│   │   ├── client.ts            # Axios instance with interceptors
│   │   ├── customerApi.ts       # Customer endpoints
│   │   ├── invoiceApi.ts        # Invoice endpoints
│   │   ├── lineItemApi.ts       # LineItem endpoints
│   │   ├── paymentApi.ts        # Payment endpoints
│   │   └── authApi.ts           # Auth endpoints (login/signup)
│   │
│   ├── viewmodels/              # ViewModels (custom hooks)
│   │   ├── customers/
│   │   │   ├── CustomerListViewModel.ts
│   │   │   ├── CustomerDetailViewModel.ts
│   │   │   └── CustomerFormViewModel.ts
│   │   ├── invoices/
│   │   │   ├── InvoiceListViewModel.ts
│   │   │   ├── InvoiceDetailViewModel.ts
│   │   │   └── InvoiceFormViewModel.ts
│   │   ├── payments/
│   │   │   ├── PaymentListViewModel.ts
│   │   │   └── PaymentFormViewModel.ts
│   │   └── auth/
│   │       ├── LoginViewModel.ts
│   │       └── SignupViewModel.ts
│   │
│   ├── views/                   # Page-level views
│   │   ├── customers/
│   │   │   ├── CustomerListView.tsx
│   │   │   ├── CustomerDetailView.tsx
│   │   │   └── CustomerFormView.tsx
│   │   ├── invoices/
│   │   │   ├── InvoiceListView.tsx
│   │   │   ├── InvoiceDetailView.tsx
│   │   │   └── InvoiceFormView.tsx
│   │   ├── payments/
│   │   │   ├── PaymentListView.tsx
│   │   │   └── PaymentFormView.tsx
│   │   └── auth/
│   │       ├── LoginView.tsx
│   │       └── SignupView.tsx
│   │
│   ├── components/              # Reusable UI components
│   │   ├── layout/
│   │   │   ├── TopBar.tsx       # Navigation + context actions
│   │   │   ├── Navigation.tsx   # Nav links (customers/invoices/payments)
│   │   │   └── PageLayout.tsx   # Consistent page wrapper
│   │   ├── common/
│   │   │   ├── Button.tsx
│   │   │   ├── Input.tsx
│   │   │   ├── Select.tsx
│   │   │   ├── Modal.tsx
│   │   │   ├── ErrorMessage.tsx
│   │   │   ├── LoadingSpinner.tsx
│   │   │   └── ConfirmDialog.tsx
│   │   └── domain/
│   │       ├── CustomerCard.tsx
│   │       ├── InvoiceStatusBadge.tsx
│   │       └── PaymentMethodBadge.tsx
│   │
│   ├── context/                 # React Context for global UI state
│   │   ├── AuthContext.tsx      # Current user, JWT token
│   │   └── ModalContext.tsx     # Modal stack management
│   │
│   ├── hooks/                   # Shared custom hooks
│   │   ├── useAuth.ts           # Auth state + logout
│   │   ├── useOptimisticLock.ts # Version mismatch handling
│   │   └── useDebounce.ts       # Debounce for search inputs
│   │
│   ├── utils/                   # Helper functions
│   │   ├── formatters.ts        # Date, currency formatting
│   │   ├── validators.ts        # Reusable validation functions
│   │   └── constants.ts         # App-wide constants
│   │
│   └── styles/
│       ├── index.css            # Tailwind imports + global styles
│       └── tailwind.config.js   # Tailwind configuration
│
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

---

## Core Libraries

### React Query (TanStack Query)

**Purpose:** Manage server state with automatic caching, refetching, and synchronization.

**Configuration:**
```typescript
// src/main.tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5, // 5 minutes
      cacheTime: 1000 * 60 * 30, // 30 minutes
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

ReactDOM.createRoot(document.getElementById('root')!).render(
  <QueryClientProvider client={queryClient}>
    <App />
  </QueryClientProvider>
);
```

**Usage Patterns:**

**Queries (Reads):**
```typescript
// Fetch single customer
const { data, isLoading, isError } = useQuery({
  queryKey: ['customers', customerId],
  queryFn: () => customerApi.getById(customerId),
});

// Fetch customer list
const { data: customers } = useQuery({
  queryKey: ['customers', 'list'],
  queryFn: customerApi.listAll,
});
```

**Mutations (Writes):**
```typescript
// Create customer
const createMutation = useMutation({
  mutationFn: customerApi.create,
  onSuccess: (response) => {
    // Invalidate customer list to refetch
    queryClient.invalidateQueries({ queryKey: ['customers', 'list'] });
    // Navigate to detail view
    navigate(`/customers/${response.data.id}`);
  },
  onError: (error) => {
    // Show error message
    setErrorMessage(error.message);
  },
});

// Usage
createMutation.mutate(formData);
```

**Invalidation Strategy:**
- After create: invalidate list query
- After update: invalidate detail + list queries
- After delete: invalidate list query
- No optimistic updates (wait for server confirmation)

### Tailwind CSS

**Purpose:** Utility-first CSS framework for rapid UI development.

**Configuration:**
```javascript
// tailwind.config.js
module.exports = {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#eff6ff',
          500: '#3b82f6',
          600: '#2563eb',
          700: '#1d4ed8',
        },
        danger: {
          500: '#ef4444',
          600: '#dc2626',
        },
      },
    },
  },
  plugins: [require('@tailwindcss/forms')],
};
```

**Design System:**
- **Spacing:** Use Tailwind's default scale (4px base)
- **Colors:** Primary blue, danger red, neutral grays
- **Typography:** Default system font stack
- **Components:** Build reusable components with consistent Tailwind classes

---

## Routing Strategy

### React Router v6 (Browser History)

**URL Structure:**
```
/login                           → LoginView
/signup                          → SignupView

/customers                       → CustomerListView
/customers/new                   → CustomerFormView (create mode)
/customers/:id                   → CustomerDetailView
/customers/:id/edit              → CustomerFormView (edit mode)

/invoices                        → InvoiceListView
/invoices/new                    → InvoiceFormView (create mode)
/invoices/:id                    → InvoiceDetailView
/invoices/:id/edit               → InvoiceFormView (edit mode)

/payments                        → PaymentListView
/payments/new                    → PaymentFormView (create mode)
/payments/:id                    → PaymentDetailView
```

**Router Setup:**
```typescript
// src/App.tsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';

export const App: React.FC = () => {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          {/* Public routes */}
          <Route path="/login" element={<LoginView />} />
          <Route path="/signup" element={<SignupView />} />

          {/* Protected routes */}
          <Route element={<ProtectedRoute />}>
            <Route path="/" element={<Navigate to="/customers" replace />} />

            <Route path="/customers" element={<CustomerListView />} />
            <Route path="/customers/new" element={<CustomerFormView />} />
            <Route path="/customers/:id" element={<CustomerDetailView />} />
            <Route path="/customers/:id/edit" element={<CustomerFormView />} />

            <Route path="/invoices" element={<InvoiceListView />} />
            <Route path="/invoices/new" element={<InvoiceFormView />} />
            <Route path="/invoices/:id" element={<InvoiceDetailView />} />
            <Route path="/invoices/:id/edit" element={<InvoiceFormView />} />

            <Route path="/payments" element={<PaymentListView />} />
            <Route path="/payments/new" element={<PaymentFormView />} />
            <Route path="/payments/:id" element={<PaymentDetailView />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
};
```

**Protected Route:**
```typescript
// src/components/ProtectedRoute.tsx
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { PageLayout } from './layout/PageLayout';

export const ProtectedRoute: React.FC = () => {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return (
    <PageLayout>
      <Outlet />
    </PageLayout>
  );
};
```

---

## State Management

### Server State: React Query
- All data from backend API
- Automatic caching and synchronization
- Managed via `useQuery` and `useMutation`

### UI State: React Context (Minimal)
- **AuthContext:** Current user, JWT token, logout function
- **ModalContext:** Modal stack for dialogs and confirmations

### Component State: useState
- Form input values
- Local UI flags (show/hide, expanded/collapsed)
- Validation errors

**AuthContext Example:**
```typescript
// src/context/AuthContext.tsx
interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (token: string, user: User) => void;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(
    () => localStorage.getItem('auth_token')
  );
  const [user, setUser] = useState<User | null>(
    () => JSON.parse(localStorage.getItem('user') || 'null')
  );

  const login = (newToken: string, newUser: User) => {
    setToken(newToken);
    setUser(newUser);
    localStorage.setItem('auth_token', newToken);
    localStorage.setItem('user', JSON.stringify(newUser));
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem('auth_token');
    localStorage.removeItem('user');
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
```

---

## API Integration

### Axios Client with Interceptors

```typescript
// src/api/client.ts
import axios, { AxiosError } from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor: Add JWT token
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('auth_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: Handle common errors
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      // Unauthorized - clear auth and redirect to login
      localStorage.removeItem('auth_token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

### API Service Layer

```typescript
// src/api/customerApi.ts
import { apiClient } from './client';
import {
  Customer,
  CustomerListItem,
  CreateCustomerRequest,
  UpdateCustomerRequest,
  DeleteCustomerRequest,
} from '../models/Customer';
import { ApiResponse } from '../models/ApiResponse';

export const customerApi = {
  // GET /api/customers
  listAll: async (): Promise<CustomerListItem[]> => {
    const response = await apiClient.get<ApiResponse<CustomerListItem[]>>('/customers');
    return response.data.data;
  },

  // GET /api/customers/:id
  getById: async (id: string): Promise<Customer> => {
    const response = await apiClient.get<ApiResponse<Customer>>(`/customers/${id}`);
    return response.data.data;
  },

  // POST /api/customers
  create: async (request: CreateCustomerRequest): Promise<Customer> => {
    const response = await apiClient.post<ApiResponse<Customer>>('/customers', request);
    return response.data.data;
  },

  // PUT /api/customers/:id
  update: async (request: UpdateCustomerRequest): Promise<Customer> => {
    const response = await apiClient.put<ApiResponse<Customer>>(
      `/customers/${request.id}`,
      request
    );
    return response.data.data;
  },

  // DELETE /api/customers/:id?version=X
  delete: async (request: DeleteCustomerRequest): Promise<void> => {
    await apiClient.delete(`/customers/${request.id}`, {
      params: { version: request.version },
    });
  },
};
```

### ApiResponse Wrapper

```typescript
// src/models/ApiResponse.ts
export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  errors?: Record<string, string>;
}
```

---

## Form Handling

### Manual Form State with React Hooks

**Strategy:**
- Use `useState` for each form field
- Manual onChange handlers
- Inline validation on blur or submit
- Validation functions match backend rules

**Example: Customer Form ViewModel**

```typescript
// src/viewmodels/customers/CustomerFormViewModel.ts
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { customerApi } from '../../api/customerApi';
import { CreateCustomerRequest, UpdateCustomerRequest } from '../../models/Customer';

export const CustomerFormViewModel = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const isEditMode = !!id;

  // Fetch existing customer if editing
  const { data: existingCustomer } = useQuery({
    queryKey: ['customers', id],
    queryFn: () => customerApi.getById(id!),
    enabled: isEditMode,
  });

  // Form fields
  const [companyName, setCompanyName] = useState('');
  const [email, setEmail] = useState('');
  const [contactFirstName, setContactFirstName] = useState('');
  const [phone, setPhone] = useState('');
  // ... other fields

  // Validation errors
  const [errors, setErrors] = useState<Record<string, string>>({});

  // Load existing data into form
  useEffect(() => {
    if (existingCustomer) {
      setCompanyName(existingCustomer.companyName);
      setEmail(existingCustomer.email || '');
      setContactFirstName(existingCustomer.contactFirstName || '');
      setPhone(existingCustomer.phone || '');
      // ... other fields
    }
  }, [existingCustomer]);

  // Validation
  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!companyName.trim()) {
      newErrors.companyName = 'Company name is required';
    }

    if (!email.trim()) {
      newErrors.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      newErrors.email = 'Must be a valid email address';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Create mutation
  const createMutation = useMutation({
    mutationFn: customerApi.create,
    onSuccess: (customer) => {
      queryClient.invalidateQueries({ queryKey: ['customers', 'list'] });
      navigate(`/customers/${customer.id}`);
    },
    onError: (error: any) => {
      if (error.response?.data?.errors) {
        setErrors(error.response.data.errors);
      }
    },
  });

  // Update mutation
  const updateMutation = useMutation({
    mutationFn: customerApi.update,
    onSuccess: (customer) => {
      queryClient.invalidateQueries({ queryKey: ['customers', id] });
      queryClient.invalidateQueries({ queryKey: ['customers', 'list'] });
      navigate(`/customers/${customer.id}`);
    },
    onError: (error: any) => {
      if (error.response?.data?.errors) {
        setErrors(error.response.data.errors);
      }
    },
  });

  // Submit handler
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!validate()) return;

    const formData: CreateCustomerRequest = {
      companyName,
      email,
      contactFirstName: contactFirstName || undefined,
      phone: phone || undefined,
      // ... other fields
    };

    if (isEditMode && existingCustomer) {
      const updateData: UpdateCustomerRequest = {
        ...formData,
        id: existingCustomer.id,
        version: existingCustomer.version,
      };
      updateMutation.mutate(updateData);
    } else {
      createMutation.mutate(formData);
    }
  };

  const handleCancel = () => {
    navigate('/customers');
  };

  return {
    // Form state
    companyName,
    setCompanyName,
    email,
    setEmail,
    contactFirstName,
    setContactFirstName,
    phone,
    setPhone,
    // ... other fields

    // Validation
    errors,

    // Actions
    handleSubmit,
    handleCancel,

    // Loading states
    isSubmitting: createMutation.isPending || updateMutation.isPending,
    isEditMode,
  };
};
```

**Example: Customer Form View**

```typescript
// src/views/customers/CustomerFormView.tsx
import { CustomerFormViewModel } from '../../viewmodels/customers/CustomerFormViewModel';

export const CustomerFormView: React.FC = () => {
  const vm = CustomerFormViewModel();

  return (
    <div className="max-w-2xl mx-auto p-6">
      <h1 className="text-3xl font-bold mb-6">
        {vm.isEditMode ? 'Edit Customer' : 'Create Customer'}
      </h1>

      <form onSubmit={vm.handleSubmit} className="space-y-6">
        {/* Company Name */}
        <div>
          <label className="block text-sm font-medium mb-2">
            Company Name <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={vm.companyName}
            onChange={(e) => vm.setCompanyName(e.target.value)}
            className={`w-full border rounded px-3 py-2 ${
              vm.errors.companyName ? 'border-red-500' : 'border-gray-300'
            }`}
          />
          {vm.errors.companyName && (
            <p className="text-red-500 text-sm mt-1">{vm.errors.companyName}</p>
          )}
        </div>

        {/* Email */}
        <div>
          <label className="block text-sm font-medium mb-2">Email</label>
          <input
            type="email"
            value={vm.email}
            onChange={(e) => vm.setEmail(e.target.value)}
            className={`w-full border rounded px-3 py-2 ${
              vm.errors.email ? 'border-red-500' : 'border-gray-300'
            }`}
          />
          {vm.errors.email && (
            <p className="text-red-500 text-sm mt-1">{vm.errors.email}</p>
          )}
        </div>

        {/* Contact First Name */}
        <div>
          <label className="block text-sm font-medium mb-2">Contact First Name</label>
          <input
            type="text"
            value={vm.contactFirstName}
            onChange={(e) => vm.setContactFirstName(e.target.value)}
            className="w-full border border-gray-300 rounded px-3 py-2"
          />
        </div>

        {/* ... other fields ... */}

        {/* Actions */}
        <div className="flex gap-3">
          <button
            type="submit"
            disabled={vm.isSubmitting}
            className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
          >
            {vm.isSubmitting ? 'Saving...' : vm.isEditMode ? 'Save Changes' : 'Create Customer'}
          </button>
          <button
            type="button"
            onClick={vm.handleCancel}
            className="bg-gray-300 text-gray-700 px-6 py-2 rounded hover:bg-gray-400"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
};
```

---

## UI Components

### Top Bar with Context-Aware Actions

```typescript
// src/components/layout/TopBar.tsx
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { Navigation } from './Navigation';

export const TopBar: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { logout } = useAuth();

  // Determine context-aware actions based on current route
  const getContextActions = () => {
    const path = location.pathname;

    // On list views, show "Create New" button
    if (path === '/customers') {
      return (
        <button
          onClick={() => navigate('/customers/new')}
          className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
        >
          New Customer
        </button>
      );
    }

    if (path === '/invoices') {
      return (
        <button
          onClick={() => navigate('/invoices/new')}
          className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
        >
          New Invoice
        </button>
      );
    }

    if (path === '/payments') {
      return (
        <button
          onClick={() => navigate('/payments/new')}
          className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
        >
          New Payment
        </button>
      );
    }

    // On detail/edit views, no context actions needed
    return null;
  };

  return (
    <header className="bg-white border-b border-gray-200 sticky top-0 z-10">
      <div className="container mx-auto px-6 py-4">
        <div className="flex items-center justify-between">
          {/* Logo/Title */}
          <h1 className="text-2xl font-bold text-blue-600">InvoiceMe</h1>

          {/* Navigation */}
          <Navigation />

          {/* Context Actions + Logout */}
          <div className="flex items-center gap-3">
            {getContextActions()}
            <button
              onClick={logout}
              className="text-gray-600 hover:text-gray-900 px-4 py-2"
            >
              Logout
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};
```

```typescript
// src/components/layout/Navigation.tsx
import { NavLink } from 'react-router-dom';

export const Navigation: React.FC = () => {
  const linkClasses = ({ isActive }: { isActive: boolean }) =>
    `px-4 py-2 rounded transition ${
      isActive
        ? 'bg-blue-100 text-blue-700 font-medium'
        : 'text-gray-600 hover:bg-gray-100'
    }`;

  return (
    <nav className="flex gap-2">
      <NavLink to="/customers" className={linkClasses}>
        Customers
      </NavLink>
      <NavLink to="/invoices" className={linkClasses}>
        Invoices
      </NavLink>
      <NavLink to="/payments" className={linkClasses}>
        Payments
      </NavLink>
    </nav>
  );
};
```

### Reusable Components

**Button Component:**
```typescript
// src/components/common/Button.tsx
interface ButtonProps {
  children: ReactNode;
  onClick?: () => void;
  type?: 'button' | 'submit' | 'reset';
  variant?: 'primary' | 'secondary' | 'danger';
  disabled?: boolean;
}

export const Button: React.FC<ButtonProps> = ({
  children,
  onClick,
  type = 'button',
  variant = 'primary',
  disabled = false,
}) => {
  const baseClasses = 'px-4 py-2 rounded font-medium transition disabled:opacity-50';

  const variantClasses = {
    primary: 'bg-blue-600 text-white hover:bg-blue-700',
    secondary: 'bg-gray-300 text-gray-700 hover:bg-gray-400',
    danger: 'bg-red-600 text-white hover:bg-red-700',
  };

  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      className={`${baseClasses} ${variantClasses[variant]}`}
    >
      {children}
    </button>
  );
};
```

**Loading Spinner:**
```typescript
// src/components/common/LoadingSpinner.tsx
export const LoadingSpinner: React.FC = () => {
  return (
    <div className="flex justify-center items-center p-8">
      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600" />
    </div>
  );
};
```

---

## Authentication Flow

### Login Process

1. User visits `/login`
2. User submits credentials (email + password)
3. Frontend calls `POST /api/auth/login`
4. Backend returns JWT token + user info
5. Frontend stores token in localStorage
6. Frontend sets AuthContext state
7. Axios interceptor adds token to all future requests
8. User redirected to `/customers`

### Logout Process

1. User clicks "Logout" button
2. Frontend clears localStorage
3. Frontend clears AuthContext state
4. User redirected to `/login`

### Protected Routes

- All routes except `/login` and `/signup` require authentication
- `ProtectedRoute` component checks `isAuthenticated`
- If not authenticated, redirect to `/login`

---

## Error Handling

### API Error Response Format

Backend returns errors in consistent format:

```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "companyName": "Company name is required",
    "email": "Customer with this email already exists"
  }
}
```

### Frontend Error Handling Strategy

**Validation Errors (400):**
- Display field-level errors inline
- Highlight invalid fields with red border
- Show error message below field

**Not Found (404):**
- Show error page with "Resource not found" message
- Provide link back to list view

**Optimistic Lock Errors (409):**
- Show modal dialog: "This record was modified by another user"
- Offer to reload the page to see latest version
- Prevent data loss by showing diff if possible

**Unauthorized (401):**
- Intercepted globally
- Clear auth state
- Redirect to login

**Server Errors (500):**
- Show generic error message
- Log error to console
- Provide retry option

**Example: Error Display Component**

```typescript
// src/components/common/ErrorMessage.tsx
interface ErrorMessageProps {
  message: string;
  onRetry?: () => void;
}

export const ErrorMessage: React.FC<ErrorMessageProps> = ({ message, onRetry }) => {
  return (
    <div className="bg-red-50 border border-red-300 rounded p-4 my-4">
      <p className="text-red-700 mb-2">{message}</p>
      {onRetry && (
        <button
          onClick={onRetry}
          className="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700"
        >
          Retry
        </button>
      )}
    </div>
  );
};
```

---

## Key Principles

### 1. MVVM Separation
- **Models:** Pure data structures, no logic
- **Views:** Pure presentation, delegate to ViewModel
- **ViewModels:** All UI logic, state, validation, API calls

### 2. Wait for Server Confirmation
- NO optimistic updates
- Show loading state during mutations
- Update UI only after successful server response
- Clearer error handling, simpler rollback logic

### 3. Manual Form Handling
- `useState` for each form field
- Manual validation functions
- Inline error display
- More boilerplate, but explicit and simple

### 4. React Query for Server State
- Automatic caching and refetching
- Consistent loading/error states
- Query invalidation on mutations
- Centralized server state management

### 5. Optimistic Locking UI
- Always send `version` field on updates/deletes
- Handle version mismatch errors gracefully
- Show clear message when concurrent modification detected
- Provide reload option to see latest data

### 6. TypeScript Everywhere
- Strong typing for all models
- Type-safe API calls
- Catch errors at compile time
- Better IDE autocomplete

### 7. RESTful URLs
- Clean, readable URLs matching backend API
- `/customers/:id` not `/customer?id=123`
- Browser history mode for proper back/forward

### 8. Consistent Layout
- TopBar with navigation + context actions + logout
- PageLayout wrapper for all protected routes
- Consistent spacing and typography via Tailwind

### 9. Error Boundaries
- React error boundaries for unexpected errors
- Prevent entire app crash
- Show fallback UI with reload option

### 10. Accessibility
- Semantic HTML elements
- Proper labels for form inputs
- Keyboard navigation support
- ARIA attributes where needed

---

## Backend API Reference

### Base URL
```
http://localhost:8080/api
```

### Endpoints

**Authentication:**
- `POST /auth/login` - Login with email/password
- `POST /auth/signup` - Create new user account

**Customers:**
- `GET /customers` - List all customers (returns `CustomerListItemResponse[]`)
- `GET /customers/:id` - Get customer by ID (returns `CustomerResponse`)
- `POST /customers` - Create customer (accepts `CreateCustomerRequest`)
- `PUT /customers/:id` - Update customer (accepts `UpdateCustomerRequest`)
- `DELETE /customers/:id?version=X` - Delete customer (requires version)

**Invoices:**
- `GET /invoices` - List all invoices
- `GET /invoices/:id` - Get invoice by ID
- `POST /invoices` - Create invoice
- `PUT /invoices/:id` - Update invoice
- `DELETE /invoices/:id?version=X` - Delete invoice

**Line Items:**
- `GET /invoices/:invoiceId/line-items` - List line items for invoice
- `POST /invoices/:invoiceId/line-items` - Create line item
- `PUT /line-items/:id` - Update line item
- `DELETE /line-items/:id?version=X` - Delete line item

**Payments:**
- `GET /payments` - List all payments
- `GET /payments/:id` - Get payment by ID
- `POST /payments` - Create payment
- `DELETE /payments/:id?version=X` - Delete payment

### Response Format
All responses wrapped in `ApiResponse<T>`:
```typescript
{
  success: boolean;
  message?: string;
  data: T;
  errors?: Record<string, string>;
}
```

---

## Development Workflow

### Local Development
```bash
npm install
npm run dev
```

### Build for Production
```bash
npm run build
npm run preview
```

### Environment Variables
```
VITE_API_BASE_URL=http://localhost:8080/api
```

### Linting & Formatting
```bash
npm run lint
npm run format
```

---

## Future Enhancements

### Phase 1 (MVP)
- Basic CRUD for customers, invoices, payments
- Authentication (login/signup)
- Manual form validation
- Wait for server confirmation

### Phase 2
- Search and filtering on list views
- Pagination for large lists
- Export invoices to PDF
- Email invoice to customer

### Phase 3
- Advanced filtering (date ranges, status, amounts)
- Dashboard with charts and metrics
- Recurring invoices
- Multi-user support with roles

### Phase 4
- Real-time updates (WebSocket)
- Optimistic updates for better UX
- Offline support (PWA)
- Mobile app (React Native)
