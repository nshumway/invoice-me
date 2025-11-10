import React from 'react';
import type { Invoice } from '../../models/Invoice';
import type { PaymentMethod } from '../../models/Payment';
import { PAYMENT_METHOD_LABELS } from '../../models/Payment';
import { useRecordPaymentViewModel } from '../../viewmodels/payments/RecordPaymentViewModel';

interface RecordPaymentFormProps {
  invoice: Invoice;
  onSuccess: () => void;
  onCancel: () => void;
}

export const RecordPaymentForm: React.FC<RecordPaymentFormProps> = ({
  invoice,
  onSuccess,
  onCancel,
}) => {
  const vm = useRecordPaymentViewModel(invoice, onSuccess);

  return (
    <form onSubmit={vm.handleSubmit} className="bg-gray-50 rounded-lg p-6 space-y-4">
      <h3 className="text-xl font-semibold mb-4">Record Payment</h3>

      {/* Balance Due */}
      <div className="bg-blue-50 rounded-lg p-4">
        <p className="text-sm text-gray-700">Balance Due:</p>
        <p className="text-2xl font-bold text-blue-700">${vm.balanceDue}</p>
      </div>

      {/* Payment Date */}
      <div>
        <label className="block text-sm font-medium mb-1">
          Payment Date <span className="text-red-500">*</span>
        </label>
        <input
          type="date"
          value={vm.paymentDate}
          onChange={e => vm.setPaymentDate(e.target.value)}
          className={`w-full border rounded px-3 py-2 ${
            vm.errors.paymentDate ? 'border-red-500' : 'border-gray-300'
          }`}
        />
        {vm.errors.paymentDate && (
          <p className="text-red-500 text-sm mt-1">{vm.errors.paymentDate}</p>
        )}
      </div>

      {/* Amount */}
      <div>
        <label className="block text-sm font-medium mb-1">
          Amount <span className="text-red-500">*</span>
        </label>
        <input
          type="number"
          step="0.01"
          value={vm.amount}
          onChange={e => vm.setAmount(e.target.value)}
          placeholder="0.00"
          className={`w-full border rounded px-3 py-2 ${
            vm.errors.amount ? 'border-red-500' : 'border-gray-300'
          }`}
        />
        {vm.errors.amount && <p className="text-red-500 text-sm mt-1">{vm.errors.amount}</p>}
      </div>

      {/* Payment Method */}
      <div>
        <label className="block text-sm font-medium mb-1">
          Payment Method <span className="text-red-500">*</span>
        </label>
        <select
          value={vm.paymentMethod}
          onChange={e => vm.setPaymentMethod(e.target.value as PaymentMethod)}
          className="w-full border border-gray-300 rounded px-3 py-2"
        >
          {Object.entries(PAYMENT_METHOD_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </div>

      {/* Reference Number */}
      <div>
        <label className="block text-sm font-medium mb-1">Reference Number</label>
        <input
          type="text"
          value={vm.referenceNumber}
          onChange={e => vm.setReferenceNumber(e.target.value)}
          placeholder="Check #, Transaction ID, etc."
          className="w-full border border-gray-300 rounded px-3 py-2"
        />
      </div>

      {/* Notes */}
      <div>
        <label className="block text-sm font-medium mb-1">Notes</label>
        <textarea
          value={vm.notes}
          onChange={e => vm.setNotes(e.target.value)}
          rows={3}
          className="w-full border border-gray-300 rounded px-3 py-2"
          placeholder="Additional payment details..."
        />
      </div>

      {/* Submit Error */}
      {vm.errors.submit && (
        <div className="bg-red-50 border border-red-300 rounded p-3">
          <p className="text-red-700 text-sm">{vm.errors.submit}</p>
        </div>
      )}

      {/* Actions */}
      <div className="flex gap-3">
        <button
          type="submit"
          disabled={vm.isSubmitting}
          className="bg-green-600 text-white px-6 py-2 rounded hover:bg-green-700 disabled:opacity-50"
        >
          {vm.isSubmitting ? 'Recording...' : 'Record Payment'}
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="bg-gray-300 text-gray-700 px-6 py-2 rounded hover:bg-gray-400"
        >
          Cancel
        </button>
      </div>
    </form>
  );
};
