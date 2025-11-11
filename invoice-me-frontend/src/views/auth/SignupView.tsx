import React from 'react';
import { Link } from 'react-router-dom';
import { SignupViewModel } from '../../viewmodels/auth/SignupViewModel';
import { Button, Input, Card, Alert } from '../../components/shared';

export const SignupView: React.FC = () => {
  const vm = SignupViewModel();

  return (
    <div className="min-h-screen bg-gray-900 flex items-center justify-center p-4 sm:p-6">
      <div className="max-w-md w-full">
        <Card>
          <h1 className="text-3xl font-bold text-center mb-6 text-gray-100">Create Account</h1>

          <form onSubmit={vm.handleSubmit} className="space-y-4">
            <Input
              type="email"
              label="Email"
              value={vm.email}
              onChange={e => vm.setEmail(e.target.value)}
              error={vm.errors.email}
              required
              autoComplete="email"
            />

            <Input
              type="password"
              label="Password"
              value={vm.password}
              onChange={e => vm.setPassword(e.target.value)}
              error={vm.errors.password}
              required
              autoComplete="new-password"
            />

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input
                type="text"
                label="First Name"
                value={vm.firstName}
                onChange={e => vm.setFirstName(e.target.value)}
                error={vm.errors.firstName}
                required
                autoComplete="given-name"
              />

              <Input
                type="text"
                label="Last Name"
                value={vm.lastName}
                onChange={e => vm.setLastName(e.target.value)}
                error={vm.errors.lastName}
                required
                autoComplete="family-name"
              />
            </div>

            {vm.errors.submit && (
              <Alert variant="error">
                <p className="text-sm">{vm.errors.submit}</p>
              </Alert>
            )}

            <Button type="submit" disabled={vm.isSubmitting} fullWidth size="lg">
              {vm.isSubmitting ? 'Creating Account...' : 'Sign Up'}
            </Button>
          </form>

          <p className="text-center text-gray-400 mt-6">
            Already have an account?{' '}
            <Link to="/login" className="text-primary-400 hover:text-primary-300 hover:underline">
              Log in
            </Link>
          </p>
        </Card>
      </div>
    </div>
  );
};
