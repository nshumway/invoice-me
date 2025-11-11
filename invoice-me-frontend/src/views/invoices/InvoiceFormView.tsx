import React from 'react';
import { InvoiceFormViewModel } from '../../viewmodels/invoices/InvoiceFormViewModel';
import { Button, Input, Select, Alert, Card } from '../../components/shared';

export const InvoiceFormView: React.FC = () => {
  const vm = InvoiceFormViewModel();

  return (
    <div className="min-h-screen bg-gray-900 p-4 sm:p-6">
      <div className="max-w-2xl mx-auto">
        <h1 className="text-2xl sm:text-3xl font-bold mb-6 text-gray-100">Create Invoice</h1>

        {vm.errorMessage && (
          <Alert variant="error" className="mb-4">
            <p className="text-sm">{vm.errorMessage}</p>
          </Alert>
        )}

        <Card>
          <form onSubmit={vm.handleSubmit} className="space-y-4">
            <div>
              <label htmlFor="customerId" className="block text-sm font-medium text-gray-200 mb-2">
                Customer <span className="text-error-500">*</span>
              </label>
              {vm.isLoadingCustomers ? (
                <p className="text-gray-400">Loading customers...</p>
              ) : (
                <Select
                  id="customerId"
                  value={vm.customerId}
                  onChange={e => vm.setCustomerId(e.target.value)}
                  options={[
                    { value: '', label: 'Select a customer' },
                    ...(vm.customers?.map(customer => ({
                      value: customer.id,
                      label: customer.companyName,
                    })) || []),
                  ]}
                  required
                />
              )}
            </div>

            <Input
              type="text"
              id="invoiceNumber"
              label="Invoice Number"
              value={vm.invoiceNumber}
              onChange={e => vm.setInvoiceNumber(e.target.value)}
              placeholder="Leave empty for auto-generation (INV-YYYY-MM-DD-###)"
              helperText="Optional. If not provided, will be auto-generated."
            />

            <div>
              <label htmlFor="notes" className="block text-sm font-medium text-gray-200 mb-2">
                Notes
              </label>
              <textarea
                id="notes"
                value={vm.notes}
                onChange={e => vm.setNotes(e.target.value)}
                rows={4}
                placeholder="Optional notes..."
                className="w-full bg-gray-700 text-gray-100 border border-gray-600 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              />
            </div>

            <div className="flex flex-col sm:flex-row gap-3 pt-2">
              <Button
                type="submit"
                disabled={!vm.isValid || vm.isSubmitting}
                variant="primary"
                fullWidth
              >
                {vm.isSubmitting ? 'Creating...' : 'Create Invoice'}
              </Button>
              <Button
                type="button"
                onClick={vm.handleCancel}
                disabled={vm.isSubmitting}
                variant="secondary"
                fullWidth
              >
                Cancel
              </Button>
            </div>
          </form>
        </Card>
      </div>
    </div>
  );
};
