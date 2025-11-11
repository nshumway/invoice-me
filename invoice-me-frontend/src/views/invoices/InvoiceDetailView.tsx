import React from 'react';
import { useParams } from 'react-router-dom';
import { InvoiceDetailViewModel } from '../../viewmodels/invoices/InvoiceDetailViewModel';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { RecordPaymentForm } from '../../components/payments/RecordPaymentForm';
import { PaymentsTable } from '../../components/payments/PaymentsTable';
import { AddLineItemForm } from '../../components/line-items/AddLineItemForm';
import { LineItemsTable } from '../../components/line-items/LineItemsTable';
import { Button, Alert, Card, StatusBadge } from '../../components/shared';
import { useAuth } from '../../hooks/useAuth';

export const InvoiceDetailView: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { logout } = useAuth();
  const vm = InvoiceDetailViewModel(id!);

  if (vm.isLoading) {
    return (
      <div className="min-h-screen bg-gray-900 p-4 sm:p-6">
        <div className="max-w-4xl mx-auto">
          <div className="text-center py-8">
            <p className="text-gray-400">Loading invoice...</p>
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
    <div className="min-h-screen bg-gray-900 p-4 sm:p-6">
      <div className="max-w-4xl mx-auto">
        {/* Header with Actions */}
        <div className="flex flex-col sm:flex-row sm:justify-between sm:items-start gap-4 mb-6">
          <div>
            <button
              onClick={vm.handleBack}
              className="text-primary-400 hover:text-primary-300 hover:underline mb-2"
            >
              ← Back to Invoices
            </button>
            <h1 className="text-2xl sm:text-3xl font-bold text-gray-100">
              {vm.invoice.invoiceNumber}
            </h1>
          </div>
          <div className="flex gap-3">
            {vm.canMarkAsSent && (
              <Button onClick={vm.handleMarkAsSent} disabled={vm.isSubmitting} variant="primary">
                {vm.isSubmitting ? 'Processing...' : 'Mark as Sent'}
              </Button>
            )}
            {vm.canDelete && (
              <Button onClick={vm.handleDelete} disabled={vm.isSubmitting} variant="danger">
                Delete
              </Button>
            )}
            <Button onClick={logout} variant="secondary">
              Logout
            </Button>
          </div>
        </div>

        <Card className="space-y-6">
          {/* Header */}
          <div className="flex flex-col sm:flex-row sm:justify-between sm:items-start gap-4 pb-4 border-b border-gray-700">
            <div>
              <StatusBadge status={vm.invoice.status} />
            </div>
            <div className="text-right">
              <p className="text-sm text-gray-400">Created</p>
              <p className="text-sm text-gray-200">
                {new Date(vm.invoice.createdAt).toLocaleDateString()}
              </p>
            </div>
          </div>

          {/* Customer Info */}
          <div>
            <h2 className="text-xl font-semibold mb-2 text-gray-100">Customer</h2>
            <p className="text-xl text-gray-200">{vm.invoice.customerName}</p>
          </div>

          {/* Invoice Details */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <p className="text-sm text-gray-400">Invoice Date</p>
              <p className="text-lg text-gray-200">
                {vm.invoice.invoiceDate
                  ? new Date(vm.invoice.invoiceDate).toLocaleDateString()
                  : 'Not sent yet'}
              </p>
            </div>
            <div>
              <p className="text-sm text-gray-400">Last Modified</p>
              <p className="text-lg text-gray-200">
                {new Date(vm.invoice.lastModifiedAt).toLocaleDateString()}
              </p>
            </div>
          </div>

          {/* Financial Summary */}
          <div className="bg-gray-700/50 border border-gray-600 rounded-lg p-4">
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div>
                <p className="text-sm text-gray-400">Total</p>
                <p className="text-2xl font-bold text-gray-100">${vm.invoice.total.toFixed(2)}</p>
              </div>
              <div>
                <p className="text-sm text-success-300">Amount Paid</p>
                <p className="text-2xl font-bold text-success-100">
                  ${vm.invoice.amountPaid.toFixed(2)}
                </p>
              </div>
              <div>
                <p className="text-sm text-primary-300">Balance</p>
                <p className="text-2xl font-bold text-primary-100">
                  ${(vm.invoice.total - vm.invoice.amountPaid).toFixed(2)}
                </p>
              </div>
            </div>
          </div>

          {/* Line Items Section */}
          <div>
            <h2 className="text-xl font-semibold mb-4 text-gray-100">Line Items</h2>

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
            <div>
              <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4 mb-4">
                <h2 className="text-xl font-semibold text-gray-100">Payments</h2>
                {!vm.showPaymentForm && (
                  <Button onClick={vm.handleRecordPayment} variant="success">
                    Record Payment
                  </Button>
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
                <p className="text-gray-400">Loading payments...</p>
              ) : (
                <PaymentsTable payments={vm.payments} onRowClick={vm.handlePaymentClick} />
              )}
            </div>
          )}

          {/* Notes Section */}
          <div>
            <div className="flex justify-between items-center mb-2">
              <h2 className="text-xl font-semibold text-gray-100">Notes</h2>
              {vm.canEdit && !vm.isEditing && (
                <button
                  onClick={vm.handleEdit}
                  className="text-sm text-primary-400 hover:text-primary-300 hover:underline"
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
                  className="w-full bg-gray-700 text-gray-100 border border-gray-600 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                  placeholder="Add notes..."
                />
                {vm.updateErrorMessage && (
                  <Alert variant="error" className="mt-2">
                    <p className="text-sm">{vm.updateErrorMessage}</p>
                  </Alert>
                )}
                <div className="flex flex-col sm:flex-row gap-2 mt-2">
                  <Button onClick={vm.handleSaveEdit} disabled={vm.isSubmitting} variant="primary">
                    {vm.isSubmitting ? 'Saving...' : 'Save'}
                  </Button>
                  <Button
                    onClick={vm.handleCancelEdit}
                    disabled={vm.isSubmitting}
                    variant="secondary"
                  >
                    Cancel
                  </Button>
                </div>
              </div>
            ) : (
              <p className="text-gray-200">
                {vm.invoice.notes || <span className="text-gray-400 italic">No notes</span>}
              </p>
            )}
          </div>
        </Card>
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
