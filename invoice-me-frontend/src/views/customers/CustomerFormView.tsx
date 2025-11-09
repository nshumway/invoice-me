import React from 'react';
import { useParams } from 'react-router-dom';
import { CustomerFormViewModel } from '../../viewmodels/customers/CustomerFormViewModel';

export const CustomerFormView: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const vm = CustomerFormViewModel({ customerId: id });

  if (vm.isLoadingCustomer) {
    return (
      <div className="max-w-2xl mx-auto p-6">
        <p className="text-gray-600">Loading customer...</p>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto p-6">
      <h1 className="text-3xl font-bold mb-6">
        {vm.isEditMode ? 'Edit Customer' : 'Create Customer'}
      </h1>

      <form onSubmit={vm.handleSubmit} className="space-y-6">
        {/* Company Name */}
        <div>
          <label className="block text-sm font-medium mb-2">
            Company Name <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={vm.companyName}
            onChange={e => vm.setCompanyName(e.target.value)}
            className={`w-full border rounded px-3 py-2 ${
              vm.errors.companyName ? 'border-red-500' : 'border-gray-300'
            }`}
          />
          {vm.errors.companyName && (
            <p className="text-red-500 text-sm mt-1">{vm.errors.companyName}</p>
          )}
        </div>

        {/* Email */}
        <div>
          <label className="block text-sm font-medium mb-2">
            Email <span className="text-red-500">*</span>
          </label>
          <input
            type="email"
            value={vm.email}
            onChange={e => vm.setEmail(e.target.value)}
            className={`w-full border rounded px-3 py-2 ${
              vm.errors.email ? 'border-red-500' : 'border-gray-300'
            }`}
          />
          {vm.errors.email && <p className="text-red-500 text-sm mt-1">{vm.errors.email}</p>}
        </div>

        {/* Contact Name */}
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium mb-2">Contact First Name</label>
            <input
              type="text"
              value={vm.contactFirstName}
              onChange={e => vm.setContactFirstName(e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-2"
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-2">Contact Last Name</label>
            <input
              type="text"
              value={vm.contactLastName}
              onChange={e => vm.setContactLastName(e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-2"
            />
          </div>
        </div>

        {/* Phone */}
        <div>
          <label className="block text-sm font-medium mb-2">Phone</label>
          <input
            type="tel"
            value={vm.phone}
            onChange={e => vm.setPhone(e.target.value)}
            className="w-full border border-gray-300 rounded px-3 py-2"
          />
        </div>

        {/* Address */}
        <div>
          <label className="block text-sm font-medium mb-2">Address Line 1</label>
          <input
            type="text"
            value={vm.addressLine1}
            onChange={e => vm.setAddressLine1(e.target.value)}
            className="w-full border border-gray-300 rounded px-3 py-2"
          />
        </div>

        <div>
          <label className="block text-sm font-medium mb-2">Address Line 2</label>
          <input
            type="text"
            value={vm.addressLine2}
            onChange={e => vm.setAddressLine2(e.target.value)}
            className="w-full border border-gray-300 rounded px-3 py-2"
          />
        </div>

        {/* City, State, Zip */}
        <div className="grid grid-cols-3 gap-4">
          <div className="col-span-2">
            <label className="block text-sm font-medium mb-2">City</label>
            <input
              type="text"
              value={vm.city}
              onChange={e => vm.setCity(e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-2"
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-2">State</label>
            <input
              type="text"
              value={vm.state}
              onChange={e => vm.setState(e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-2"
            />
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium mb-2">Zip Code</label>
            <input
              type="text"
              value={vm.zipCode}
              onChange={e => vm.setZipCode(e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-2"
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-2">Country</label>
            <input
              type="text"
              value={vm.country}
              onChange={e => vm.setCountry(e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-2"
            />
          </div>
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
            className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
          >
            {vm.isSubmitting
              ? vm.isEditMode
                ? 'Updating...'
                : 'Creating...'
              : vm.isEditMode
                ? 'Update Customer'
                : 'Create Customer'}
          </button>
          <button
            type="button"
            onClick={vm.handleCancel}
            className="bg-gray-300 text-gray-700 px-6 py-2 rounded hover:bg-gray-400"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
};
