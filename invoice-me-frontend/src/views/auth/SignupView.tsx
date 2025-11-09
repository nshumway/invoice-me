import React from 'react';
import { Link } from 'react-router-dom';
import { SignupViewModel } from '../../viewmodels/auth/SignupViewModel';

export const SignupView: React.FC = () => {
  const vm = SignupViewModel();

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center p-6">
      <div className="max-w-md w-full bg-white rounded-lg shadow-md p-8">
        <h1 className="text-3xl font-bold text-center mb-6">Create Account</h1>

        <form onSubmit={vm.handleSubmit} className="space-y-4">
          {/* Email */}
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

          {/* Password */}
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

          {/* First Name */}
          <div>
            <label className="block text-sm font-medium mb-2">First Name</label>
            <input
              type="text"
              value={vm.firstName}
              onChange={e => vm.setFirstName(e.target.value)}
              className={`w-full border rounded px-3 py-2 ${
                vm.errors.firstName ? 'border-red-500' : 'border-gray-300'
              }`}
            />
            {vm.errors.firstName && (
              <p className="text-red-500 text-sm mt-1">{vm.errors.firstName}</p>
            )}
          </div>

          {/* Last Name */}
          <div>
            <label className="block text-sm font-medium mb-2">Last Name</label>
            <input
              type="text"
              value={vm.lastName}
              onChange={e => vm.setLastName(e.target.value)}
              className={`w-full border rounded px-3 py-2 ${
                vm.errors.lastName ? 'border-red-500' : 'border-gray-300'
              }`}
            />
            {vm.errors.lastName && (
              <p className="text-red-500 text-sm mt-1">{vm.errors.lastName}</p>
            )}
          </div>

          {/* Submit Error */}
          {vm.errors.submit && (
            <div className="bg-red-50 border border-red-300 rounded p-3">
              <p className="text-red-700 text-sm">{vm.errors.submit}</p>
            </div>
          )}

          {/* Submit Button */}
          <button
            type="submit"
            disabled={vm.isSubmitting}
            className="w-full bg-blue-600 text-white px-6 py-3 rounded hover:bg-blue-700 disabled:opacity-50"
          >
            {vm.isSubmitting ? 'Creating Account...' : 'Sign Up'}
          </button>
        </form>

        <p className="text-center text-gray-600 mt-6">
          Already have an account?{' '}
          <Link to="/login" className="text-blue-600 hover:underline">
            Log in
          </Link>
        </p>
      </div>
    </div>
  );
};
