import React from 'react';
import { useParams } from 'react-router-dom';
import { InvoiceDetailViewModel } from '../../viewmodels/invoices/InvoiceDetailViewModel';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { RecordPaymentForm } from '../../components/payments/RecordPaymentForm';
import { PaymentsTable } from '../../components/payments/PaymentsTable';
import { AddLineItemForm } from '../../components/line-items/AddLineItemForm';
import { LineItemsTable } from '../../components/line-items/LineItemsTable';

export const InvoiceDetailView: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const vm = InvoiceDetailViewModel(id!);

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

  if (vm.isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 p-6 flex items-center justify-center">
        <p className="text-gray-600">Loading invoice...</p>
      </div>
    );
  }

  if (vm.isError) {
    return (
      <div className="min-h-screen bg-gray-50 p-6">
        <div className="container mx-auto max-w-4xl">
          <div className="bg-red-50 border border-red-300 rounded p-4">
            <p className="text-red-700">{vm.errorMessage}</p>
          </div>
          <button onClick={vm.handleBack} className="mt-4 text-blue-600 hover:text-blue-800">
            ← Back to Invoices
          </button>
        </div>
      </div>
    );
  }

  if (!vm.invoice) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="container mx-auto max-w-4xl">
        <div className="mb-4">
          <button onClick={vm.handleBack} className="text-blue-600 hover:text-blue-800">
            ← Back to Invoices
          </button>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          {/* Header */}
          <div className="flex justify-between items-start mb-6 pb-4 border-b">
            <div>
              <h1 className="text-3xl font-bold mb-2">{vm.invoice.invoiceNumber}</h1>
              <span className={`px-3 py-1 rounded text-sm ${getStatusColor(vm.invoice.status)}`}>
                {vm.invoice.status}
              </span>
            </div>
            <div className="text-right">
              <p className="text-sm text-gray-500">Created</p>
              <p className="text-sm">{new Date(vm.invoice.createdAt).toLocaleDateString()}</p>
            </div>
          </div>

          {/* Customer Info */}
          <div className="mb-6">
            <h2 className="text-lg font-semibold mb-2">Customer</h2>
            <p className="text-xl">{vm.invoice.customerName}</p>
          </div>

          {/* Invoice Details */}
          <div className="grid grid-cols-2 gap-4 mb-6">
            <div>
              <p className="text-sm text-gray-500">Invoice Date</p>
              <p className="text-lg">
                {vm.invoice.invoiceDate
                  ? new Date(vm.invoice.invoiceDate).toLocaleDateString()
                  : 'Not sent yet'}
              </p>
            </div>
            <div>
              <p className="text-sm text-gray-500">Last Modified</p>
              <p className="text-lg">{new Date(vm.invoice.lastModifiedAt).toLocaleDateString()}</p>
            </div>
          </div>

          {/* Financial Summary */}
          <div className="bg-gray-50 rounded p-4 mb-6">
            <div className="grid grid-cols-3 gap-4">
              <div>
                <p className="text-sm text-gray-500">Total</p>
                <p className="text-2xl font-bold">${vm.invoice.total.toFixed(2)}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Amount Paid</p>
                <p className="text-2xl font-bold text-green-600">
                  ${vm.invoice.amountPaid.toFixed(2)}
                </p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Balance</p>
                <p className="text-2xl font-bold text-blue-600">
                  ${(vm.invoice.total - vm.invoice.amountPaid).toFixed(2)}
                </p>
              </div>
            </div>
          </div>

          {/* Line Items Section */}
          <div className="mb-6">
            <h2 className="text-lg font-semibold mb-4">Line Items</h2>

            <LineItemsTable
              lineItems={vm.lineItems}
              canEdit={vm.canEdit}
              onUpdate={request => vm.updateLineItemMutation.mutate(request)}
              onDelete={(lineItemId, version) =>
                vm.deleteLineItemMutation.mutate({ lineItemId, version })
              }
              isSubmitting={
                vm.updateLineItemMutation.isPending || vm.deleteLineItemMutation.isPending
              }
            />

            <div className="mt-4">
              <AddLineItemForm
                invoiceId={id!}
                onAdd={request => vm.createLineItemMutation.mutate(request)}
                isSubmitting={vm.createLineItemMutation.isPending}
                canEdit={vm.canEdit}
              />
            </div>
          </div>

          {/* Payments Section */}
          {vm.canRecordPayment && (
            <div className="mb-6">
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-lg font-semibold">Payments</h2>
                {!vm.showPaymentForm && (
                  <button
                    onClick={vm.handleRecordPayment}
                    className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700"
                  >
                    Record Payment
                  </button>
                )}
              </div>

              {vm.showPaymentForm && (
                <div className="mb-4">
                  <RecordPaymentForm
                    invoice={vm.invoice}
                    onSuccess={vm.handlePaymentSuccess}
                    onCancel={vm.handleCancelPayment}
                  />
                </div>
              )}

              {vm.paymentsLoading ? (
                <p className="text-gray-600">Loading payments...</p>
              ) : (
                <PaymentsTable payments={vm.payments} onRowClick={vm.handlePaymentClick} />
              )}
            </div>
          )}

          {/* Notes Section */}
          <div className="mb-6">
            <div className="flex justify-between items-center mb-2">
              <h2 className="text-lg font-semibold">Notes</h2>
              {vm.canEdit && !vm.isEditing && (
                <button
                  onClick={vm.handleEdit}
                  className="text-sm text-blue-600 hover:text-blue-800"
                >
                  Edit
                </button>
              )}
            </div>

            {vm.isEditing ? (
              <div>
                <textarea
                  value={vm.notes}
                  onChange={e => vm.setNotes(e.target.value)}
                  rows={4}
                  className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                {vm.updateErrorMessage && (
                  <div className="bg-red-50 border border-red-300 rounded p-3 mt-2">
                    <p className="text-red-700 text-sm">{vm.updateErrorMessage}</p>
                  </div>
                )}
                <div className="flex gap-2 mt-2">
                  <button
                    onClick={vm.handleSaveEdit}
                    disabled={vm.isSubmitting}
                    className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
                  >
                    {vm.isSubmitting ? 'Saving...' : 'Save'}
                  </button>
                  <button
                    onClick={vm.handleCancelEdit}
                    disabled={vm.isSubmitting}
                    className="bg-gray-300 text-gray-700 px-4 py-2 rounded hover:bg-gray-400 disabled:bg-gray-200 disabled:cursor-not-allowed"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            ) : (
              <p className="text-gray-700">
                {vm.invoice.notes || <span className="text-gray-400 italic">No notes</span>}
              </p>
            )}
          </div>

          {/* Actions */}
          <div className="flex gap-3 pt-4 border-t">
            {vm.canMarkAsSent && (
              <button
                onClick={vm.handleMarkAsSent}
                disabled={vm.isSubmitting}
                className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
              >
                {vm.isSubmitting ? 'Processing...' : 'Mark as Sent'}
              </button>
            )}
            {vm.canDelete && (
              <button
                onClick={vm.handleDelete}
                disabled={vm.isSubmitting}
                className="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
              >
                Delete
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        isOpen={vm.showDeleteConfirm}
        title="Delete Invoice"
        message={`Are you sure you want to delete invoice ${vm.invoice.invoiceNumber}? This action cannot be undone.`}
        confirmLabel="Delete"
        cancelLabel="Cancel"
        onConfirm={vm.handleConfirmDelete}
        onCancel={vm.handleCancelDelete}
      />
    </div>
  );
};
