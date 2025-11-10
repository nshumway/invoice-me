import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { InvoiceListViewModel } from '../../viewmodels/invoices/InvoiceListViewModel';

export const InvoiceListView: React.FC = () => {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const vm = InvoiceListViewModel();

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'DRAFT':
        return 'bg-gray-700 text-gray-300 border border-gray-600';
      case 'SENT':
        return 'bg-blue-900/50 text-blue-300 border border-blue-700';
      case 'PAID':
        return 'bg-green-900/50 text-green-300 border border-green-700';
      case 'OVERDUE':
        return 'bg-red-900/50 text-red-300 border border-red-700';
      case 'CANCELLED':
        return 'bg-gray-700 text-gray-400 border border-gray-600';
      default:
        return 'bg-gray-700 text-gray-300 border border-gray-600';
    }
  };

  return (
    <div className="min-h-screen bg-gray-900 p-6">
      <div className="container mx-auto">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-3xl font-bold text-gray-100">Invoices</h1>
          <div className="flex gap-3">
            <button
              onClick={() => navigate('/customers')}
              className="bg-purple-600 text-white px-4 py-2 rounded hover:bg-purple-700 transition-colors"
            >
              Customers
            </button>
            <button
              onClick={vm.handleCreateNew}
              className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition-colors"
            >
              Create Invoice
            </button>
            <button
              onClick={logout}
              className="bg-gray-700 text-gray-200 px-4 py-2 rounded hover:bg-gray-600 transition-colors"
            >
              Logout
            </button>
          </div>
        </div>

        {vm.customerFilter && (
          <div className="mb-4 flex items-center gap-2">
            <span className="text-sm text-gray-400">Filtered by customer</span>
            <button
              onClick={vm.handleClearFilter}
              className="text-sm text-blue-400 hover:text-blue-300 underline"
            >
              Clear filter
            </button>
          </div>
        )}

        {vm.isLoading && (
          <div className="text-center py-8">
            <p className="text-gray-400">Loading invoices...</p>
          </div>
        )}

        {vm.isError && (
          <div className="bg-red-900/50 border border-red-700 rounded p-4 my-4">
            <p className="text-red-300">{vm.errorMessage}</p>
          </div>
        )}

        {vm.invoices && vm.invoices.length === 0 && (
          <div className="text-center py-12 bg-gray-800 border border-gray-700 rounded">
            <p className="text-gray-400 mb-4">No invoices yet</p>
            <button
              onClick={vm.handleCreateNew}
              className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 transition-colors"
            >
              Create Your First Invoice
            </button>
          </div>
        )}

        {vm.invoices && vm.invoices.length > 0 && (
          <div className="bg-gray-800 rounded-lg shadow-xl border border-gray-700 overflow-hidden">
            <table className="w-full">
              <thead className="bg-gray-900/50 border-b border-gray-700">
                <tr>
                  <th className="text-left p-4 font-medium text-gray-300">Invoice Number</th>
                  <th className="text-left p-4 font-medium text-gray-300">Customer</th>
                  <th className="text-left p-4 font-medium text-gray-300">Date</th>
                  <th className="text-left p-4 font-medium text-gray-300">Status</th>
                  <th className="text-right p-4 font-medium text-gray-300">Total</th>
                  <th className="text-right p-4 font-medium text-gray-300">Paid</th>
                </tr>
              </thead>
              <tbody>
                {vm.invoices.map(invoice => (
                  <tr
                    key={invoice.id}
                    onClick={() => vm.handleViewInvoice(invoice.id)}
                    className="border-b border-gray-700 hover:bg-gray-700/50 cursor-pointer transition-colors"
                  >
                    <td className="p-4 font-medium text-gray-100">{invoice.invoiceNumber}</td>
                    <td className="p-4 text-gray-200">{invoice.customerName}</td>
                    <td className="p-4 text-gray-400">
                      {invoice.invoiceDate ? (
                        new Date(invoice.invoiceDate).toLocaleDateString()
                      ) : (
                        <span className="text-gray-500">—</span>
                      )}
                    </td>
                    <td className="p-4">
                      <span
                        className={`px-2 py-1 rounded text-sm ${getStatusColor(invoice.status)}`}
                      >
                        {invoice.status}
                      </span>
                    </td>
                    <td className="p-4 text-right font-mono text-gray-200">
                      ${invoice.total.toFixed(2)}
                    </td>
                    <td className="p-4 text-right font-mono text-gray-200">
                      ${invoice.amountPaid.toFixed(2)}
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
