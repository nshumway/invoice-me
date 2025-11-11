import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { LoadingProvider } from './contexts/LoadingContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { LoginView } from './views/auth/LoginView';
import { SignupView } from './views/auth/SignupView';
import { CustomerListView } from './views/customers/CustomerListView';
import { CustomerFormView } from './views/customers/CustomerFormView';
import { CustomerDetailView } from './views/customers/CustomerDetailView';
import { InvoiceListView } from './views/invoices/InvoiceListView';
import { InvoiceFormView } from './views/invoices/InvoiceFormView';
import { InvoiceDetailView } from './views/invoices/InvoiceDetailView';
import { PaymentDetailView } from './views/payments/PaymentDetailView';

function App() {
  return (
    <LoadingProvider>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginView />} />
          <Route path="/signup" element={<SignupView />} />

          <Route element={<ProtectedRoute />}>
            <Route path="/" element={<Navigate to="/customers" replace />} />
            <Route path="/customers" element={<CustomerListView />} />
            <Route path="/customers/new" element={<CustomerFormView />} />
            <Route path="/customers/:id" element={<CustomerDetailView />} />
            <Route path="/customers/:id/edit" element={<CustomerFormView />} />
            <Route path="/invoices" element={<InvoiceListView />} />
            <Route path="/invoices/new" element={<InvoiceFormView />} />
            <Route path="/invoices/:id" element={<InvoiceDetailView />} />
            <Route path="/payments/:id" element={<PaymentDetailView />} />
          </Route>
        </Routes>
      </AuthProvider>
    </LoadingProvider>
  );
}

export default App;
