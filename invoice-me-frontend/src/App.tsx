import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { LoginView } from './views/auth/LoginView';
import { SignupView } from './views/auth/SignupView';
import { CustomerListView } from './views/customers/CustomerListView';
import { CustomerFormView } from './views/customers/CustomerFormView';
import { CustomerDetailView } from './views/customers/CustomerDetailView';

function App() {
  return (
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
        </Route>
      </Routes>
    </AuthProvider>
  );
}

export default App;
