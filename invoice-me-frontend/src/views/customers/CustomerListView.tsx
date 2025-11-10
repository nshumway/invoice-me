import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { CustomerListViewModel } from '../../viewmodels/customers/CustomerListViewModel';

export const CustomerListView: React.FC = () => {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const vm = CustomerListViewModel();

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="container mx-auto">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-3xl font-bold">Customers</h1>
          <div className="flex gap-3">
            <button
              onClick={() => navigate('/invoices')}
              className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700"
            >
              Invoices
            </button>
            <button
              onClick={vm.handleCreateNew}
              className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
            >
              Create Customer
            </button>
            <button
              onClick={logout}
              className="bg-gray-300 text-gray-700 px-4 py-2 rounded hover:bg-gray-400"
            >
              Logout
            </button>
          </div>
        </div>

        {vm.isLoading && (
          <div className="text-center py-8">
            <p className="text-gray-600">Loading customers...</p>
          </div>
        )}

        {vm.isError && (
          <div className="bg-red-50 border border-red-300 rounded p-4 my-4">
            <p className="text-red-700">{vm.errorMessage}</p>
          </div>
        )}

        {vm.customers && vm.customers.length === 0 && (
          <div className="text-center py-12 bg-gray-50 rounded">
            <p className="text-gray-600 mb-4">No customers yet</p>
            <button
              onClick={vm.handleCreateNew}
              className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700"
            >
              Create Your First Customer
            </button>
          </div>
        )}

        {vm.customers && vm.customers.length > 0 && (
          <div className="bg-white rounded-lg shadow overflow-hidden">
            <table className="w-full">
              <thead className="bg-gray-50 border-b">
                <tr>
                  <th className="text-left p-4 font-medium text-gray-700">Company Name</th>
                  <th className="text-left p-4 font-medium text-gray-700">Email</th>
                  <th className="text-right p-4 font-medium text-gray-700">Outstanding</th>
                </tr>
              </thead>
              <tbody>
                {vm.customers.map(customer => (
                  <tr
                    key={customer.id}
                    onClick={() => vm.handleRowClick(customer.id)}
                    className="border-b hover:bg-gray-50 cursor-pointer"
                  >
                    <td className="p-4">{customer.companyName}</td>
                    <td className="p-4 text-gray-600">
                      {customer.email || <span className="text-gray-400">—</span>}
                    </td>
                    <td className="p-4 text-right font-mono">
                      ${parseFloat(customer.totalOutstanding).toFixed(2)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
