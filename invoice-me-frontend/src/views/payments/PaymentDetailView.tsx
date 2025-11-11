import React from 'react';
import { useParams } from 'react-router-dom';
import { usePaymentDetailViewModel } from '../../viewmodels/payments/PaymentDetailViewModel';
import { PAYMENT_METHOD_LABELS } from '../../models/Payment';
import { Card, Alert, Button } from '../../components/shared';
import { useAuth } from '../../hooks/useAuth';

export const PaymentDetailView: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { logout } = useAuth();
  const vm = usePaymentDetailViewModel(id!);

  if (vm.isLoading) {
    return (
      <div className="min-h-screen bg-gray-900 p-4 sm:p-6">
        <div className="max-w-4xl mx-auto">
          <div className="text-center py-8">
            <p className="text-gray-400">Loading payment...</p>
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
            ← Back
          </button>
        </div>
      </div>
    );
  }

  if (!vm.payment) {
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
              ← Back to Invoice
            </button>
            <h1 className="text-2xl sm:text-3xl font-bold text-gray-100">Payment Details</h1>
          </div>
          <div className="flex gap-3">
            <Button onClick={logout} variant="secondary">
              Logout
            </Button>
          </div>
        </div>

        <Card className="space-y-6">
          {/* Payment ID */}
          <div className="pb-4 border-b border-gray-700">
            <p className="text-gray-400">Payment ID: {vm.payment.id}</p>
          </div>

          {/* Payment Amount */}
          <div className="bg-success-900/30 border border-success-700/50 rounded-lg p-6">
            <p className="text-sm text-success-300 mb-2">Payment Amount</p>
            <p className="text-4xl font-bold text-success-100">
              ${parseFloat(vm.payment.amount).toFixed(2)}
            </p>
          </div>

          {/* Payment Information */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
            <div>
              <p className="text-sm text-gray-400 mb-1">Payment Date</p>
              <p className="text-lg font-semibold text-gray-200">
                {new Date(vm.payment.paymentDate).toLocaleDateString()}
              </p>
            </div>
            <div>
              <p className="text-sm text-gray-400 mb-1">Payment Method</p>
              <p className="text-lg font-semibold text-gray-200">
                {PAYMENT_METHOD_LABELS[vm.payment.paymentMethod]}
              </p>
            </div>
            <div>
              <p className="text-sm text-gray-400 mb-1">Reference Number</p>
              <p className="text-lg font-semibold text-gray-200">
                {vm.payment.referenceNumber || <span className="text-gray-400">—</span>}
              </p>
            </div>
            <div>
              <p className="text-sm text-gray-400 mb-1">Customer</p>
              <p className="text-lg font-semibold text-gray-200">{vm.payment.customerName}</p>
            </div>
          </div>

          {/* Notes */}
          {vm.payment.notes && (
            <div>
              <p className="text-sm text-gray-400 mb-1">Notes</p>
              <p className="text-gray-200 bg-gray-700/50 border border-gray-600 rounded-lg p-4">
                {vm.payment.notes}
              </p>
            </div>
          )}

          {/* Audit Information */}
          <div className="border-t border-gray-700 pt-6">
            <h2 className="text-xl font-semibold mb-4 text-gray-100">Audit Information</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
              <div>
                <p className="text-gray-400">Created At</p>
                <p className="text-gray-200">{new Date(vm.payment.createdAt).toLocaleString()}</p>
              </div>
              <div>
                <p className="text-gray-400">Last Modified At</p>
                <p className="text-gray-200">
                  {new Date(vm.payment.lastModifiedAt).toLocaleString()}
                </p>
              </div>
              <div>
                <p className="text-gray-400">Version</p>
                <p className="text-gray-200">{vm.payment.version}</p>
              </div>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
};
