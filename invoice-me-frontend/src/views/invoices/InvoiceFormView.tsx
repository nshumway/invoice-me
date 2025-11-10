import React from 'react';
import { InvoiceFormViewModel } from '../../viewmodels/invoices/InvoiceFormViewModel';

export const InvoiceFormView: React.FC = () => {
  const vm = InvoiceFormViewModel();

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="container mx-auto max-w-2xl">
        <h1 className="text-3xl font-bold mb-6">Create Invoice</h1>

        {vm.errorMessage && (
          <div className="bg-red-50 border border-red-300 rounded p-4 mb-4">
            <p className="text-red-700">{vm.errorMessage}</p>
          </div>
        )}

        <form onSubmit={vm.handleSubmit} className="bg-white rounded-lg shadow p-6">
          <div className="mb-4">
            <label htmlFor="customerId" className="block text-sm font-medium text-gray-700 mb-2">
              Customer <span className="text-red-500">*</span>
            </label>
            {vm.isLoadingCustomers ? (
              <p className="text-gray-500">Loading customers...</p>
            ) : (
              <select
                id="customerId"
                value={vm.customerId}
                onChange={e => vm.setCustomerId(e.target.value)}
                className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                required
              >
                <option value="">Select a customer</option>
                {vm.customers?.map(customer => (
                  <option key={customer.id} value={customer.id}>
                    {customer.companyName}
                  </option>
                ))}
              </select>
            )}
          </div>

          <div className="mb-4">
            <label htmlFor="invoiceNumber" className="block text-sm font-medium text-gray-700 mb-2">
              Invoice Number
            </label>
            <input
              type="text"
              id="invoiceNumber"
              value={vm.invoiceNumber}
              onChange={e => vm.setInvoiceNumber(e.target.value)}
              placeholder="Leave empty for auto-generation (INV-YYYY-MM-DD-###)"
              className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <p className="text-sm text-gray-500 mt-1">
              Optional. If not provided, will be auto-generated.
            </p>
          </div>

          <div className="mb-6">
            <label htmlFor="notes" className="block text-sm font-medium text-gray-700 mb-2">
              Notes
            </label>
            <textarea
              id="notes"
              value={vm.notes}
              onChange={e => vm.setNotes(e.target.value)}
              rows={4}
              placeholder="Optional notes..."
              className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="flex gap-3">
            <button
              type="submit"
              disabled={!vm.isValid || vm.isSubmitting}
              className="flex-1 bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
            >
              {vm.isSubmitting ? 'Creating...' : 'Create Invoice'}
            </button>
            <button
              type="button"
              onClick={vm.handleCancel}
              disabled={vm.isSubmitting}
              className="flex-1 bg-gray-300 text-gray-700 px-4 py-2 rounded hover:bg-gray-400 disabled:bg-gray-200 disabled:cursor-not-allowed"
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
