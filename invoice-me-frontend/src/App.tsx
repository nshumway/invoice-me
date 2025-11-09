import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { LoginView } from './views/auth/LoginView';
import { SignupView } from './views/auth/SignupView';
import { CustomerListView } from './views/customers/CustomerListView';

function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginView />} />
        <Route path="/signup" element={<SignupView />} />

        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<Navigate to="/customers" replace />} />
          <Route path="/customers" element={<CustomerListView />} />
        </Route>
      </Routes>
    </AuthProvider>
  );
}

export default App;
