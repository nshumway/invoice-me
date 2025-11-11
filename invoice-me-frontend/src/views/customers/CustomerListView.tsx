import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { CustomerListViewModel } from '../../viewmodels/customers/CustomerListViewModel';
import { Button, Alert } from '../../components/shared';
import { useIsMobile } from '../../hooks/useMediaQuery';

export const CustomerListView: React.FC = () => {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const vm = CustomerListViewModel();
  const isMobile = useIsMobile();

  return (
    <div className="min-h-screen bg-gray-900 p-4 sm:p-6">
      <div className="container mx-auto">
        <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4 mb-6">
          <h1 className="text-2xl sm:text-3xl font-bold text-gray-100">Customers</h1>
          <div className="flex flex-wrap gap-2 sm:gap-3">
            <Button
              onClick={() => navigate('/invoices')}
              variant="success"
              size={isMobile ? 'sm' : 'md'}
            >
              Invoices
            </Button>
            <Button onClick={vm.handleCreateNew} variant="primary" size={isMobile ? 'sm' : 'md'}>
              Create Customer
            </Button>
            <Button onClick={logout} variant="secondary" size={isMobile ? 'sm' : 'md'}>
              Logout
            </Button>
          </div>
        </div>

        {vm.isLoading && (
          <div className="text-center py-8">
            <p className="text-gray-400">Loading customers...</p>
          </div>
        )}

        {vm.isError && (
          <Alert variant="error" className="my-4">
            <p className="text-sm">{vm.errorMessage}</p>
          </Alert>
        )}

        {vm.customers && vm.customers.length === 0 && (
          <div className="text-center py-12 bg-gray-800 border border-gray-700 rounded">
            <p className="text-gray-400 mb-4">No customers yet</p>
            <Button onClick={vm.handleCreateNew} variant="primary">
              Create Your First Customer
            </Button>
          </div>
        )}

        {vm.customers && vm.customers.length > 0 && (
          <>
            {/* Mobile: Card Layout */}
            {isMobile ? (
              <div className="space-y-4">
                {vm.customers.map(customer => (
                  <div
                    key={customer.id}
                    onClick={() => vm.handleRowClick(customer.id)}
                    className="bg-gray-800 border border-gray-700 rounded-lg p-4 cursor-pointer hover:bg-gray-750 transition-colors"
                  >
                    <h3 className="font-semibold text-lg text-gray-100">{customer.companyName}</h3>
                    <p className="text-gray-400 text-sm mt-1">
                      {customer.email || <span className="text-gray-500">—</span>}
                    </p>
                    <div className="mt-3 pt-3 border-t border-gray-700">
                      <p className="text-sm text-gray-400">Outstanding</p>
                      <p className="text-lg font-semibold font-mono text-gray-100">
                        ${parseFloat(customer.totalOutstanding).toFixed(2)}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              /* Desktop: Table Layout */
              <div className="bg-gray-800 rounded-lg shadow-xl border border-gray-700 overflow-hidden">
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead className="bg-gray-900/50 border-b border-gray-700">
                      <tr>
                        <th className="text-left p-4 font-medium text-gray-300">Company Name</th>
                        <th className="text-left p-4 font-medium text-gray-300">Email</th>
                        <th className="text-right p-4 font-medium text-gray-300">Outstanding</th>
                      </tr>
                    </thead>
                    <tbody>
                      {vm.customers.map(customer => (
                        <tr
                          key={customer.id}
                          onClick={() => vm.handleRowClick(customer.id)}
                          className="border-b border-gray-700 hover:bg-gray-700/50 cursor-pointer transition-colors"
                        >
                          <td className="p-4 text-gray-100">{customer.companyName}</td>
                          <td className="p-4 text-gray-400">
                            {customer.email || <span className="text-gray-500">—</span>}
                          </td>
                          <td className="p-4 text-right font-mono text-gray-200">
                            ${parseFloat(customer.totalOutstanding).toFixed(2)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};
