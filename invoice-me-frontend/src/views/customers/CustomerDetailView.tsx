import React from 'react';
import { CustomerDetailViewModel } from '../../viewmodels/customers/CustomerDetailViewModel';
import { ConfirmDialog } from '../../components/ConfirmDialog';

export const CustomerDetailView: React.FC = () => {
  const vm = CustomerDetailViewModel();

  if (vm.isLoading) {
    return (
      <div className="max-w-4xl mx-auto p-6">
        <div className="text-center py-8">
          <p className="text-gray-600">Loading customer...</p>
        </div>
      </div>
    );
  }

  if (vm.isError) {
    return (
      <div className="max-w-4xl mx-auto p-6">
        <div className="bg-red-50 border border-red-300 rounded p-4">
          <p className="text-red-700">{vm.errorMessage}</p>
        </div>
        <button onClick={vm.handleBack} className="mt-4 text-blue-600 hover:underline">
          ← Back to Customers
        </button>
      </div>
    );
  }

  if (!vm.customer) {
    return null;
  }

  return (
    <div className="max-w-4xl mx-auto p-6">
      {/* Header */}
      <div className="flex justify-between items-start mb-6">
        <div>
          <button onClick={vm.handleBack} className="text-blue-600 hover:underline mb-2">
            ← Back to Customers
          </button>
          <h1 className="text-3xl font-bold">{vm.customer.companyName}</h1>
        </div>
        <div className="flex gap-3">
          <button
            onClick={vm.handleEdit}
            className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
          >
            Edit
          </button>
          <button
            onClick={vm.handleDelete}
            className="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700"
          >
            Delete
          </button>
        </div>
      </div>

      {/* Customer Details */}
      <div className="bg-white rounded-lg shadow p-6 space-y-6">
        {/* Contact Information */}
        <div>
          <h2 className="text-xl font-semibold mb-4 text-gray-800">Contact Information</h2>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">Contact Name</label>
              <p className="text-gray-900">
                {vm.customer.contactFirstName || vm.customer.contactLastName
                  ? `${vm.customer.contactFirstName || ''} ${vm.customer.contactLastName || ''}`.trim()
                  : '—'}
              </p>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">Email</label>
              <p className="text-gray-900">{vm.customer.email}</p>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">Phone</label>
              <p className="text-gray-900">{vm.customer.phone || '—'}</p>
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
            <h2 className="text-xl font-semibold mb-4 text-gray-800">Address</h2>
            <div className="text-gray-900">
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

        {/* Invoice Statistics */}
        <div>
          <h2 className="text-xl font-semibold mb-4 text-gray-800">Invoice Statistics</h2>
          <div className="grid grid-cols-4 gap-4">
            <div className="bg-gray-50 p-4 rounded">
              <p className="text-sm text-gray-600 mb-1">Draft Invoices</p>
              <p className="text-2xl font-bold text-gray-900">{vm.customer.draftInvoiceCount}</p>
            </div>
            <div className="bg-blue-50 p-4 rounded">
              <p className="text-sm text-gray-600 mb-1">Sent Invoices</p>
              <p className="text-2xl font-bold text-blue-900">{vm.customer.sentInvoiceCount}</p>
            </div>
            <div className="bg-green-50 p-4 rounded">
              <p className="text-sm text-gray-600 mb-1">Paid Invoices</p>
              <p className="text-2xl font-bold text-green-900">{vm.customer.paidInvoiceCount}</p>
            </div>
            <div className="bg-yellow-50 p-4 rounded">
              <p className="text-sm text-gray-600 mb-1">Outstanding</p>
              <p className="text-2xl font-bold text-yellow-900">
                ${parseFloat(vm.customer.totalOutstanding).toFixed(2)}
              </p>
            </div>
          </div>
        </div>

        {/* Metadata */}
        <div className="border-t pt-4">
          <h2 className="text-xl font-semibold mb-4 text-gray-800">Metadata</h2>
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <label className="block text-gray-600 mb-1">Created At</label>
              <p className="text-gray-900">{new Date(vm.customer.createdAt).toLocaleString()}</p>
            </div>
            <div>
              <label className="block text-gray-600 mb-1">Last Modified</label>
              <p className="text-gray-900">
                {new Date(vm.customer.lastModifiedAt).toLocaleString()}
              </p>
            </div>
            <div>
              <label className="block text-gray-600 mb-1">Version</label>
              <p className="text-gray-900">{vm.customer.version}</p>
            </div>
          </div>
        </div>
      </div>

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
  );
};
