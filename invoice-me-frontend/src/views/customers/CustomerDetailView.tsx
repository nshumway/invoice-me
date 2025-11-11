import React from 'react';
import { CustomerDetailViewModel } from '../../viewmodels/customers/CustomerDetailViewModel';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { Button, Alert, Card, StatusBadge } from '../../components/shared';
import { useAuth } from '../../hooks/useAuth';
import { useIsMobile } from '../../hooks/useMediaQuery';

export const CustomerDetailView: React.FC = () => {
  const { logout } = useAuth();
  const isMobile = useIsMobile();
  const vm = CustomerDetailViewModel();

  if (vm.isLoading) {
    return (
      <div className="min-h-screen bg-gray-900 p-4 sm:p-6">
        <div className="max-w-4xl mx-auto">
          <div className="text-center py-8">
            <p className="text-gray-400">Loading customer...</p>
          </div>
        </div>
      </div>
    );
  }

  if (vm.isError) {
    return (
      <div className="min-h-screen bg-gray-900 p-4 sm:p-6">
        <div className="max-w-4xl mx-auto">
          <Alert variant="error" className="mb-4">
            <p className="text-sm">{vm.errorMessage}</p>
          </Alert>
          <button
            onClick={vm.handleBack}
            className="text-primary-400 hover:text-primary-300 hover:underline"
          >
            ← Back to Customers
          </button>
        </div>
      </div>
    );
  }

  if (!vm.customer) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gray-900 p-4 sm:p-6">
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:justify-between sm:items-start gap-4 mb-6">
          <div>
            <button
              onClick={vm.handleBack}
              className="text-primary-400 hover:text-primary-300 hover:underline mb-2"
            >
              ← Back to Customers
            </button>
            <h1 className="text-2xl sm:text-3xl font-bold text-gray-100">
              {vm.customer.companyName}
            </h1>
          </div>
          <div className="flex gap-3">
            <Button onClick={vm.handleEdit} variant="primary">
              Edit
            </Button>
            <Button onClick={vm.handleDelete} variant="danger">
              Delete
            </Button>
            <Button onClick={logout} variant="secondary">
              Logout
            </Button>
          </div>
        </div>

        {/* Customer Details */}
        <Card className="space-y-6">
          {/* Contact Information */}
          <div>
            <h2 className="text-xl font-semibold mb-4 text-gray-100">Contact Information</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-400 mb-1">Contact Name</label>
                <p className="text-gray-200">
                  {vm.customer.contactFirstName || vm.customer.contactLastName
                    ? `${vm.customer.contactFirstName || ''} ${vm.customer.contactLastName || ''}`.trim()
                    : '—'}
                </p>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-400 mb-1">Email</label>
                <p className="text-gray-200">{vm.customer.email}</p>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-400 mb-1">Phone</label>
                <p className="text-gray-200">{vm.customer.phone || '—'}</p>
              </div>
            </div>
          </div>

          {/* Address */}
          {(vm.customer.addressLine1 ||
            vm.customer.city ||
            vm.customer.state ||
            vm.customer.zipCode ||
            vm.customer.country) && (
            <div>
              <h2 className="text-xl font-semibold mb-4 text-gray-100">Address</h2>
              <div className="text-gray-200">
                {vm.customer.addressLine1 && <p>{vm.customer.addressLine1}</p>}
                {vm.customer.addressLine2 && <p>{vm.customer.addressLine2}</p>}
                {(vm.customer.city || vm.customer.state || vm.customer.zipCode) && (
                  <p>
                    {vm.customer.city}
                    {vm.customer.city && vm.customer.state ? ', ' : ''}
                    {vm.customer.state} {vm.customer.zipCode}
                  </p>
                )}
                {vm.customer.country && <p>{vm.customer.country}</p>}
              </div>
            </div>
          )}

          {/* Invoices */}
          <div>
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-semibold text-gray-100">Invoices</h2>
              <Button onClick={() => vm.handleCreateInvoice()} variant="primary" size="sm">
                Create Invoice
              </Button>
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
              <button
                onClick={() => vm.handleFilterByStatus('DRAFT')}
                className={`p-4 rounded border transition-all ${
                  vm.invoiceStatusFilter === 'DRAFT'
                    ? 'bg-gray-600 border-gray-500 ring-2 ring-gray-400'
                    : 'bg-gray-700 border-gray-600 hover:bg-gray-650'
                }`}
              >
                <p className="text-sm text-gray-400 mb-1">Draft Invoices</p>
                <p className="text-2xl font-bold text-gray-100">{vm.customer.draftInvoiceCount}</p>
              </button>
              <button
                onClick={() => vm.handleFilterByStatus('SENT')}
                className={`p-4 rounded border transition-all ${
                  vm.invoiceStatusFilter === 'SENT'
                    ? 'bg-primary-800/50 border-primary-600 ring-2 ring-primary-500'
                    : 'bg-primary-900/30 border-primary-700/50 hover:bg-primary-800/40'
                }`}
              >
                <p className="text-sm text-primary-300 mb-1">Sent Invoices</p>
                <p className="text-2xl font-bold text-primary-100">
                  {vm.customer.sentInvoiceCount}
                </p>
              </button>
              <button
                onClick={() => vm.handleFilterByStatus('PAID')}
                className={`p-4 rounded border transition-all ${
                  vm.invoiceStatusFilter === 'PAID'
                    ? 'bg-success-800/50 border-success-600 ring-2 ring-success-500'
                    : 'bg-success-900/30 border-success-700/50 hover:bg-success-800/40'
                }`}
              >
                <p className="text-sm text-success-300 mb-1">Paid Invoices</p>
                <p className="text-2xl font-bold text-success-100">
                  {vm.customer.paidInvoiceCount}
                </p>
              </button>
              <div className="bg-warning-900/30 p-4 rounded border border-warning-700/50">
                <p className="text-sm text-warning-300 mb-1">Outstanding (Sent)</p>
                <p className="text-2xl font-bold text-warning-100">
                  ${parseFloat(vm.customer.totalOutstanding).toFixed(2)}
                </p>
              </div>
            </div>
          </div>

          {/* Customer Invoices */}
          {vm.invoiceStatusFilter && (
            <div className="border-t border-gray-700 pt-6">
              <h2 className="text-xl font-semibold text-gray-100 mb-4">
                {vm.invoiceStatusFilter.charAt(0) + vm.invoiceStatusFilter.slice(1).toLowerCase()}{' '}
                Invoices
              </h2>

              {vm.invoicesLoading ? (
                <p className="text-gray-400">Loading invoices...</p>
              ) : vm.invoices && vm.invoices.length === 0 ? (
                <p className="text-gray-400 text-center py-4">
                  No {vm.invoiceStatusFilter.toLowerCase()} invoices found
                </p>
              ) : (
                <>
                  {/* Mobile: Card Layout */}
                  {isMobile ? (
                    <div className="space-y-4">
                      {vm.invoices?.map(invoice => (
                        <div
                          key={invoice.id}
                          onClick={() => vm.handleViewInvoice(invoice.id)}
                          className="bg-gray-700/50 border border-gray-600 rounded-lg p-4 cursor-pointer hover:bg-gray-700 transition-colors"
                        >
                          <div className="flex justify-between items-start mb-3">
                            <div>
                              <h3 className="font-semibold text-lg text-gray-100">
                                {invoice.invoiceNumber}
                              </h3>
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
                              <p className="text-gray-200 font-mono">
                                ${invoice.amountPaid.toFixed(2)}
                              </p>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    /* Desktop: Table Layout */
                    <div className="bg-gray-700/50 rounded-lg border border-gray-600 overflow-hidden">
                      <div className="overflow-x-auto">
                        <table className="w-full">
                          <thead className="bg-gray-800/50 border-b border-gray-600">
                            <tr>
                              <th className="text-left p-4 font-medium text-gray-300">Invoice #</th>
                              <th className="text-left p-4 font-medium text-gray-300">Date</th>
                              <th className="text-left p-4 font-medium text-gray-300">Status</th>
                              <th className="text-right p-4 font-medium text-gray-300">Total</th>
                              <th className="text-right p-4 font-medium text-gray-300">Paid</th>
                            </tr>
                          </thead>
                          <tbody>
                            {vm.invoices?.map(invoice => (
                              <tr
                                key={invoice.id}
                                onClick={() => vm.handleViewInvoice(invoice.id)}
                                className="border-b border-gray-600 hover:bg-gray-700/70 cursor-pointer transition-colors"
                              >
                                <td className="p-4 font-medium text-gray-100">
                                  {invoice.invoiceNumber}
                                </td>
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
          )}

          {/* Metadata */}
          <div className="border-t border-gray-700 pt-4">
            <h2 className="text-xl font-semibold mb-4 text-gray-100">Metadata</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
              <div>
                <label className="block text-gray-400 mb-1">Created At</label>
                <p className="text-gray-200">{new Date(vm.customer.createdAt).toLocaleString()}</p>
              </div>
              <div>
                <label className="block text-gray-400 mb-1">Last Modified</label>
                <p className="text-gray-200">
                  {new Date(vm.customer.lastModifiedAt).toLocaleString()}
                </p>
              </div>
              <div>
                <label className="block text-gray-400 mb-1">Version</label>
                <p className="text-gray-200">{vm.customer.version}</p>
              </div>
            </div>
          </div>
        </Card>

        {/* Delete Confirmation Dialog */}
        <ConfirmDialog
          isOpen={vm.showDeleteDialog}
          title="Delete Customer"
          message={`Are you sure you want to delete ${vm.customer.companyName}? This action cannot be undone.`}
          confirmLabel="Delete"
          cancelLabel="Cancel"
          onConfirm={vm.handleConfirmDelete}
          onCancel={vm.handleCancelDelete}
          isLoading={vm.isDeleting}
        />
      </div>
    </div>
  );
};
