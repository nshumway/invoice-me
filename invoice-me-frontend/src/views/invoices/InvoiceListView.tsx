import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { InvoiceListViewModel } from '../../viewmodels/invoices/InvoiceListViewModel';
import { Button, Alert, StatusBadge } from '../../components/shared';
import { useIsMobile } from '../../hooks/useMediaQuery';

export const InvoiceListView: React.FC = () => {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const vm = InvoiceListViewModel();
  const isMobile = useIsMobile();

  return (
    <div className="min-h-screen bg-gray-900 p-4 sm:p-6">
      <div className="container mx-auto">
        <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4 mb-6">
          <h1 className="text-2xl sm:text-3xl font-bold text-gray-100">Invoices</h1>
          <div className="flex flex-wrap gap-2 sm:gap-3">
            <Button
              onClick={() => navigate('/customers')}
              variant="secondary"
              size={isMobile ? 'sm' : 'md'}
            >
              Customers
            </Button>
            <Button onClick={vm.handleCreateNew} variant="primary" size={isMobile ? 'sm' : 'md'}>
              Create Invoice
            </Button>
            <Button onClick={logout} variant="secondary" size={isMobile ? 'sm' : 'md'}>
              Logout
            </Button>
          </div>
        </div>

        {/* Status Filter Buttons */}
        <div className="mb-4 flex flex-wrap gap-2">
          <Button
            onClick={() => vm.handleFilterByStatus(undefined)}
            variant={vm.statusFilter === undefined ? 'primary' : 'secondary'}
            size="sm"
          >
            All
          </Button>
          <Button
            onClick={() => vm.handleFilterByStatus('DRAFT')}
            variant={vm.statusFilter === 'DRAFT' ? 'primary' : 'secondary'}
            size="sm"
          >
            Draft
          </Button>
          <Button
            onClick={() => vm.handleFilterByStatus('SENT')}
            variant={vm.statusFilter === 'SENT' ? 'primary' : 'secondary'}
            size="sm"
          >
            Sent
          </Button>
          <Button
            onClick={() => vm.handleFilterByStatus('PAID')}
            variant={vm.statusFilter === 'PAID' ? 'primary' : 'secondary'}
            size="sm"
          >
            Paid
          </Button>
        </div>

        {vm.customerFilter && (
          <div className="mb-4 flex items-center gap-2">
            <span className="text-sm text-gray-400">Filtered by customer</span>
            <button
              onClick={vm.handleClearFilter}
              className="text-sm text-primary-400 hover:text-primary-300 underline"
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
          <Alert variant="error" className="my-4">
            <p className="text-sm">{vm.errorMessage}</p>
          </Alert>
        )}

        {vm.invoices && vm.invoices.length === 0 && (
          <div className="text-center py-12 bg-gray-800 border border-gray-700 rounded">
            <p className="text-gray-400 mb-4">No invoices yet</p>
            <Button onClick={vm.handleCreateNew} variant="primary">
              Create Your First Invoice
            </Button>
          </div>
        )}

        {vm.invoices && vm.invoices.length > 0 && (
          <>
            {/* Mobile: Card Layout */}
            {isMobile ? (
              <div className="space-y-4">
                {vm.invoices.map(invoice => (
                  <div
                    key={invoice.id}
                    onClick={() => vm.handleViewInvoice(invoice.id)}
                    className="bg-gray-800 border border-gray-700 rounded-lg p-4 cursor-pointer hover:bg-gray-750 transition-colors"
                  >
                    <div className="flex justify-between items-start mb-3">
                      <div>
                        <h3 className="font-semibold text-lg text-gray-100">
                          {invoice.invoiceNumber}
                        </h3>
                        <p className="text-gray-400 text-sm">{invoice.customerName}</p>
                      </div>
                      <StatusBadge status={invoice.status} />
                    </div>
                    <div className="grid grid-cols-2 gap-3 text-sm">
                      <div>
                        <p className="text-gray-400">Date</p>
                        <p className="text-gray-200">
                          {invoice.invoiceDate ? (
                            new Date(invoice.invoiceDate).toLocaleDateString()
                          ) : (
                            <span className="text-gray-500">—</span>
                          )}
                        </p>
                      </div>
                      <div>
                        <p className="text-gray-400">Total</p>
                        <p className="text-gray-200 font-mono">${invoice.total.toFixed(2)}</p>
                      </div>
                      <div>
                        <p className="text-gray-400">Paid</p>
                        <p className="text-gray-200 font-mono">${invoice.amountPaid.toFixed(2)}</p>
                      </div>
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
                        <th className="text-left p-4 font-medium text-gray-300">Invoice #</th>
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
                            <StatusBadge status={invoice.status} />
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
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};
