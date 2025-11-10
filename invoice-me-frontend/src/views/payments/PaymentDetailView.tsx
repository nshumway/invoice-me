import React from 'react';
import { useParams } from 'react-router-dom';
import { usePaymentDetailViewModel } from '../../viewmodels/payments/PaymentDetailViewModel';
import { PAYMENT_METHOD_LABELS } from '../../models/Payment';

export const PaymentDetailView: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const vm = usePaymentDetailViewModel(id!);

  if (vm.isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 p-6 flex items-center justify-center">
        <p className="text-gray-600">Loading payment...</p>
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
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="container mx-auto max-w-4xl">
        <div className="mb-4">
          <button onClick={vm.handleBack} className="text-blue-600 hover:text-blue-800">
            ← Back to Invoice
          </button>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          {/* Header */}
          <div className="mb-6 pb-4 border-b">
            <h1 className="text-3xl font-bold mb-2">Payment Details</h1>
            <p className="text-gray-600">Payment ID: {vm.payment.id}</p>
          </div>

          {/* Payment Amount */}
          <div className="bg-green-50 rounded p-6 mb-6">
            <p className="text-sm text-gray-600 mb-2">Payment Amount</p>
            <p className="text-4xl font-bold text-green-700">
              ${parseFloat(vm.payment.amount).toFixed(2)}
            </p>
          </div>

          {/* Payment Information */}
          <div className="grid grid-cols-2 gap-6 mb-6">
            <div>
              <p className="text-sm text-gray-500 mb-1">Payment Date</p>
              <p className="text-lg font-semibold">
                {new Date(vm.payment.paymentDate).toLocaleDateString()}
              </p>
            </div>
            <div>
              <p className="text-sm text-gray-500 mb-1">Payment Method</p>
              <p className="text-lg font-semibold">
                {PAYMENT_METHOD_LABELS[vm.payment.paymentMethod]}
              </p>
            </div>
            <div>
              <p className="text-sm text-gray-500 mb-1">Reference Number</p>
              <p className="text-lg font-semibold">
                {vm.payment.referenceNumber || <span className="text-gray-400">—</span>}
              </p>
            </div>
            <div>
              <p className="text-sm text-gray-500 mb-1">Customer</p>
              <p className="text-lg font-semibold">{vm.payment.customerName}</p>
            </div>
          </div>

          {/* Notes */}
          {vm.payment.notes && (
            <div className="mb-6">
              <p className="text-sm text-gray-500 mb-1">Notes</p>
              <p className="text-gray-700 bg-gray-50 rounded p-4">{vm.payment.notes}</p>
            </div>
          )}

          {/* Audit Information */}
          <div className="border-t pt-6">
            <h2 className="text-lg font-semibold mb-4">Audit Information</h2>
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <p className="text-gray-500">Created At</p>
                <p>{new Date(vm.payment.createdAt).toLocaleString()}</p>
              </div>
              <div>
                <p className="text-gray-500">Last Modified At</p>
                <p>{new Date(vm.payment.lastModifiedAt).toLocaleString()}</p>
              </div>
              <div>
                <p className="text-gray-500">Version</p>
                <p>{vm.payment.version}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
