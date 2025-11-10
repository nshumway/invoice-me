import React from 'react';
import { Link } from 'react-router-dom';
import { LoginViewModel } from '../../viewmodels/auth/LoginViewModel';

export const LoginView: React.FC = () => {
  const vm = LoginViewModel();

  return (
    <div className="min-h-screen bg-gray-900 dark:bg-gray-950 flex items-center justify-center p-6">
      <div className="max-w-md w-full bg-gray-800 dark:bg-gray-900 rounded-lg shadow-xl border border-gray-700 p-8">
        <h1 className="text-3xl font-bold text-center mb-6 text-gray-100">Log In</h1>

        <form onSubmit={vm.handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium mb-2 text-gray-200">Email</label>
            <input
              type="email"
              value={vm.email}
              onChange={e => vm.setEmail(e.target.value)}
              className={`w-full bg-gray-700 text-gray-100 border rounded px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
                vm.errors.email ? 'border-red-500' : 'border-gray-600'
              }`}
            />
            {vm.errors.email && <p className="text-red-400 text-sm mt-1">{vm.errors.email}</p>}
          </div>

          <div>
            <label className="block text-sm font-medium mb-2 text-gray-200">Password</label>
            <input
              type="password"
              value={vm.password}
              onChange={e => vm.setPassword(e.target.value)}
              className={`w-full bg-gray-700 text-gray-100 border rounded px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
                vm.errors.password ? 'border-red-500' : 'border-gray-600'
              }`}
            />
            {vm.errors.password && (
              <p className="text-red-400 text-sm mt-1">{vm.errors.password}</p>
            )}
          </div>

          {vm.errors.submit && (
            <div className="bg-red-900/50 border border-red-700 rounded p-3">
              <p className="text-red-300 text-sm">{vm.errors.submit}</p>
            </div>
          )}

          <button
            type="submit"
            disabled={vm.isSubmitting}
            className="w-full bg-blue-600 text-white px-6 py-3 rounded hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {vm.isSubmitting ? 'Logging In...' : 'Log In'}
          </button>
        </form>

        <p className="text-center text-gray-400 mt-6">
          Don't have an account?{' '}
          <Link to="/signup" className="text-blue-400 hover:text-blue-300 hover:underline">
            Sign up
          </Link>
        </p>
      </div>
    </div>
  );
};
