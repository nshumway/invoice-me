import React from 'react';
import { Link } from 'react-router-dom';
import { LoginViewModel } from '../../viewmodels/auth/LoginViewModel';
import { Button, Input, Card, Alert } from '../../components/shared';

export const LoginView: React.FC = () => {
  const vm = LoginViewModel();

  return (
    <div className="min-h-screen bg-gray-900 flex items-center justify-center p-4 sm:p-6">
      <div className="max-w-md w-full">
        <Card>
          <h1 className="text-3xl font-bold text-center mb-6 text-gray-100">Log In</h1>

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
              autoComplete="current-password"
            />

            {vm.errors.submit && (
              <Alert variant="error">
                <p className="text-sm">{vm.errors.submit}</p>
              </Alert>
            )}

            <Button type="submit" disabled={vm.isSubmitting} fullWidth size="lg">
              {vm.isSubmitting ? 'Logging In...' : 'Log In'}
            </Button>
          </form>

          <p className="text-center text-gray-400 mt-6">
            Don't have an account?{' '}
            <Link to="/signup" className="text-primary-400 hover:text-primary-300 hover:underline">
              Sign up
            </Link>
          </p>
        </Card>
      </div>
    </div>
  );
};
