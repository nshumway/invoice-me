import React from 'react';
import { Link } from 'react-router-dom';
import { SignupViewModel } from '../../viewmodels/auth/SignupViewModel';

export const SignupView: React.FC = () => {
  const vm = SignupViewModel();

  return (
    <div className="min-h-screen bg-gray-900 dark:bg-gray-950 flex items-center justify-center p-6">
      <div className="max-w-md w-full bg-gray-800 dark:bg-gray-900 rounded-lg shadow-xl border border-gray-700 p-8">
        <h1 className="text-3xl font-bold text-center mb-6 text-gray-100">Create Account</h1>

        <form onSubmit={vm.handleSubmit} className="space-y-4">
          {/* Email */}
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

          {/* Password */}
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

          {/* First Name */}
          <div>
            <label className="block text-sm font-medium mb-2 text-gray-200">First Name</label>
            <input
              type="text"
              value={vm.firstName}
              onChange={e => vm.setFirstName(e.target.value)}
              className={`w-full bg-gray-700 text-gray-100 border rounded px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
                vm.errors.firstName ? 'border-red-500' : 'border-gray-600'
              }`}
            />
            {vm.errors.firstName && (
              <p className="text-red-400 text-sm mt-1">{vm.errors.firstName}</p>
            )}
          </div>

          {/* Last Name */}
          <div>
            <label className="block text-sm font-medium mb-2 text-gray-200">Last Name</label>
            <input
              type="text"
              value={vm.lastName}
              onChange={e => vm.setLastName(e.target.value)}
              className={`w-full bg-gray-700 text-gray-100 border rounded px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
                vm.errors.lastName ? 'border-red-500' : 'border-gray-600'
              }`}
            />
            {vm.errors.lastName && (
              <p className="text-red-400 text-sm mt-1">{vm.errors.lastName}</p>
            )}
          </div>

          {/* Submit Error */}
          {vm.errors.submit && (
            <div className="bg-red-900/50 border border-red-700 rounded p-3">
              <p className="text-red-300 text-sm">{vm.errors.submit}</p>
            </div>
          )}

          {/* Submit Button */}
          <button
            type="submit"
            disabled={vm.isSubmitting}
            className="w-full bg-blue-600 text-white px-6 py-3 rounded hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {vm.isSubmitting ? 'Creating Account...' : 'Sign Up'}
          </button>
        </form>

        <p className="text-center text-gray-400 mt-6">
          Already have an account?{' '}
          <Link to="/login" className="text-blue-400 hover:text-blue-300 hover:underline">
            Log in
          </Link>
        </p>
      </div>
    </div>
  );
};
