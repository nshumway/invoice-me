import React from 'react';
import type { Invoice } from '../../models/Invoice';
import type { PaymentMethod } from '../../models/Payment';
import { PAYMENT_METHOD_LABELS } from '../../models/Payment';
import { useRecordPaymentViewModel } from '../../viewmodels/payments/RecordPaymentViewModel';
import { Button, Input, Select, Alert } from '../shared';

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
    <form
      onSubmit={vm.handleSubmit}
      className="bg-gray-800 rounded-lg p-4 sm:p-6 space-y-4 border border-gray-700"
    >
      <h3 className="text-xl font-semibold mb-4 text-gray-100">Record Payment</h3>

      <div className="bg-primary-900/30 border border-primary-700/50 rounded-lg p-4">
        <p className="text-sm text-primary-200">Balance Due:</p>
        <p className="text-2xl font-bold text-primary-100">${vm.balanceDue}</p>
      </div>

      <Input
        type="date"
        label="Payment Date"
        value={vm.paymentDate}
        onChange={e => vm.setPaymentDate(e.target.value)}
        error={vm.errors.paymentDate}
        required
      />

      <Input
        type="number"
        label="Amount"
        step="0.01"
        value={vm.amount}
        onChange={e => vm.setAmount(e.target.value)}
        error={vm.errors.amount}
        required
      />

      <Select
        label="Payment Method"
        value={vm.paymentMethod}
        onChange={e => vm.setPaymentMethod(e.target.value as PaymentMethod)}
        options={Object.entries(PAYMENT_METHOD_LABELS).map(([value, label]) => ({
          value,
          label,
        }))}
        required
      />

      <Input
        type="text"
        label="Reference Number"
        value={vm.referenceNumber}
        onChange={e => vm.setReferenceNumber(e.target.value)}
        helperText="Check #, Transaction ID, etc."
      />

      <div className="w-full">
        <label className="block text-sm font-medium mb-2 text-gray-200">Notes</label>
        <textarea
          value={vm.notes}
          onChange={e => vm.setNotes(e.target.value)}
          rows={3}
          className="w-full bg-gray-700 text-gray-100 border border-gray-600 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
          placeholder="Additional payment details..."
        />
      </div>

      {vm.errors.submit && (
        <Alert variant="error">
          <p className="text-sm">{vm.errors.submit}</p>
        </Alert>
      )}

      <div className="flex flex-col sm:flex-row gap-3 pt-2">
        <Button type="submit" disabled={vm.isSubmitting} variant="success">
          {vm.isSubmitting ? 'Recording...' : 'Record Payment'}
        </Button>
        <Button type="button" onClick={onCancel} variant="secondary">
          Cancel
        </Button>
      </div>
    </form>
  );
};
