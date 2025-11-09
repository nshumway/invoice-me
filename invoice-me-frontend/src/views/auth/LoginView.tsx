import React from 'react';
import { Link } from 'react-router-dom';
import { LoginViewModel } from '../../viewmodels/auth/LoginViewModel';

export const LoginView: React.FC = () => {
  const vm = LoginViewModel();

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center p-6">
      <div className="max-w-md w-full bg-white rounded-lg shadow-md p-8">
        <h1 className="text-3xl font-bold text-center mb-6">Log In</h1>

        <form onSubmit={vm.handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium mb-2">Email</label>
            <input
              type="email"
              value={vm.email}
              onChange={e => vm.setEmail(e.target.value)}
              className={`w-full border rounded px-3 py-2 ${
                vm.errors.email ? 'border-red-500' : 'border-gray-300'
              }`}
            />
            {vm.errors.email && <p className="text-red-500 text-sm mt-1">{vm.errors.email}</p>}
          </div>

          <div>
            <label className="block text-sm font-medium mb-2">Password</label>
            <input
              type="password"
              value={vm.password}
              onChange={e => vm.setPassword(e.target.value)}
              className={`w-full border rounded px-3 py-2 ${
                vm.errors.password ? 'border-red-500' : 'border-gray-300'
              }`}
            />
            {vm.errors.password && (
              <p className="text-red-500 text-sm mt-1">{vm.errors.password}</p>
            )}
          </div>

          {vm.errors.submit && (
            <div className="bg-red-50 border border-red-300 rounded p-3">
              <p className="text-red-700 text-sm">{vm.errors.submit}</p>
            </div>
          )}

          <button
            type="submit"
            disabled={vm.isSubmitting}
            className="w-full bg-blue-600 text-white px-6 py-3 rounded hover:bg-blue-700 disabled:opacity-50"
          >
            {vm.isSubmitting ? 'Logging In...' : 'Log In'}
          </button>
        </form>

        <p className="text-center text-gray-600 mt-6">
          Don't have an account?{' '}
          <Link to="/signup" className="text-blue-600 hover:underline">
            Sign up
          </Link>
        </p>
      </div>
    </div>
  );
};
