import React from 'react';
import { useAuth } from '../../hooks/useAuth';

export const CustomerListView: React.FC = () => {
  const { user, logout } = useAuth();

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="container mx-auto">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-3xl font-bold">Customers</h1>
          <button
            onClick={logout}
            className="bg-gray-300 text-gray-700 px-4 py-2 rounded hover:bg-gray-400"
          >
            Logout
          </button>
        </div>
        <p>Welcome, {user?.firstName}! Customer list coming in Phase 3.</p>
      </div>
    </div>
  );
};
