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
        return 'bg-gray-100 text-gray-800';
      case 'SENT':
        return 'bg-blue-100 text-blue-800';
      case 'PAID':
        return 'bg-green-100 text-green-800';
      case 'OVERDUE':
        return 'bg-red-100 text-red-800';
      case 'CANCELLED':
        return 'bg-gray-100 text-gray-600';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="container mx-auto">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-3xl font-bold">Invoices</h1>
          <div className="flex gap-3">
            <button
              onClick={() => navigate('/customers')}
              className="bg-purple-600 text-white px-4 py-2 rounded hover:bg-purple-700"
            >
              Customers
            </button>
            <button
              onClick={vm.handleCreateNew}
              className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
            >
              Create Invoice
            </button>
            <button
              onClick={logout}
              className="bg-gray-300 text-gray-700 px-4 py-2 rounded hover:bg-gray-400"
            >
              Logout
            </button>
          </div>
        </div>

        {vm.customerFilter && (
          <div className="mb-4 flex items-center gap-2">
            <span className="text-sm text-gray-600">Filtered by customer</span>
            <button
              onClick={vm.handleClearFilter}
              className="text-sm text-blue-600 hover:text-blue-800 underline"
            >
              Clear filter
            </button>
          </div>
        )}

        {vm.isLoading && (
          <div className="text-center py-8">
            <p className="text-gray-600">Loading invoices...</p>
          </div>
        )}

        {vm.isError && (
          <div className="bg-red-50 border border-red-300 rounded p-4 my-4">
            <p className="text-red-700">{vm.errorMessage}</p>
          </div>
        )}

        {vm.invoices && vm.invoices.length === 0 && (
          <div className="text-center py-12 bg-gray-50 rounded">
            <p className="text-gray-600 mb-4">No invoices yet</p>
            <button
              onClick={vm.handleCreateNew}
              className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700"
            >
              Create Your First Invoice
            </button>
          </div>
        )}

        {vm.invoices && vm.invoices.length > 0 && (
          <div className="bg-white rounded-lg shadow overflow-hidden">
            <table className="w-full">
              <thead className="bg-gray-50 border-b">
                <tr>
                  <th className="text-left p-4 font-medium text-gray-700">Invoice Number</th>
                  <th className="text-left p-4 font-medium text-gray-700">Customer</th>
                  <th className="text-left p-4 font-medium text-gray-700">Date</th>
                  <th className="text-left p-4 font-medium text-gray-700">Status</th>
                  <th className="text-right p-4 font-medium text-gray-700">Total</th>
                  <th className="text-right p-4 font-medium text-gray-700">Paid</th>
                </tr>
              </thead>
              <tbody>
                {vm.invoices.map(invoice => (
                  <tr
                    key={invoice.id}
                    onClick={() => vm.handleViewInvoice(invoice.id)}
                    className="border-b hover:bg-gray-50 cursor-pointer"
                  >
                    <td className="p-4 font-medium">{invoice.invoiceNumber}</td>
                    <td className="p-4">{invoice.customerName}</td>
                    <td className="p-4 text-gray-600">
                      {invoice.invoiceDate ? (
                        new Date(invoice.invoiceDate).toLocaleDateString()
                      ) : (
                        <span className="text-gray-400">—</span>
                      )}
                    </td>
                    <td className="p-4">
                      <span
                        className={`px-2 py-1 rounded text-sm ${getStatusColor(invoice.status)}`}
                      >
                        {invoice.status}
                      </span>
                    </td>
                    <td className="p-4 text-right font-mono">${invoice.total.toFixed(2)}</td>
                    <td className="p-4 text-right font-mono">${invoice.amountPaid.toFixed(2)}</td>
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
