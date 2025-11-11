import React from 'react';
import { useParams } from 'react-router-dom';
import { CustomerFormViewModel } from '../../viewmodels/customers/CustomerFormViewModel';
import { Button, Input, Alert } from '../../components/shared';

export const CustomerFormView: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const vm = CustomerFormViewModel({ customerId: id });

  if (vm.isLoadingCustomer) {
    return (
      <div className="max-w-2xl mx-auto p-4 sm:p-6">
        <p className="text-gray-400">Loading customer...</p>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto p-4 sm:p-6">
      <h1 className="text-2xl sm:text-3xl font-bold mb-6 text-gray-100">
        {vm.isEditMode ? 'Edit Customer' : 'Create Customer'}
      </h1>

      <form onSubmit={vm.handleSubmit} className="space-y-6">
        <Input
          type="text"
          label="Company Name"
          value={vm.companyName}
          onChange={e => vm.setCompanyName(e.target.value)}
          error={vm.errors.companyName}
          required
          autoComplete="organization"
        />

        <Input
          type="email"
          label="Email"
          value={vm.email}
          onChange={e => vm.setEmail(e.target.value)}
          error={vm.errors.email}
          required
          autoComplete="email"
        />

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Input
            type="text"
            label="Contact First Name"
            value={vm.contactFirstName}
            onChange={e => vm.setContactFirstName(e.target.value)}
            autoComplete="given-name"
          />

          <Input
            type="text"
            label="Contact Last Name"
            value={vm.contactLastName}
            onChange={e => vm.setContactLastName(e.target.value)}
            autoComplete="family-name"
          />
        </div>

        <Input
          type="tel"
          label="Phone"
          value={vm.phone}
          onChange={e => vm.setPhone(e.target.value)}
          autoComplete="tel"
        />

        <Input
          type="text"
          label="Address Line 1"
          value={vm.addressLine1}
          onChange={e => vm.setAddressLine1(e.target.value)}
          autoComplete="address-line1"
        />

        <Input
          type="text"
          label="Address Line 2"
          value={vm.addressLine2}
          onChange={e => vm.setAddressLine2(e.target.value)}
          autoComplete="address-line2"
        />

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="sm:col-span-2">
            <Input
              type="text"
              label="City"
              value={vm.city}
              onChange={e => vm.setCity(e.target.value)}
              autoComplete="address-level2"
            />
          </div>
          <Input
            type="text"
            label="State"
            value={vm.state}
            onChange={e => vm.setState(e.target.value)}
            autoComplete="address-level1"
          />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Input
            type="text"
            label="Zip Code"
            value={vm.zipCode}
            onChange={e => vm.setZipCode(e.target.value)}
            autoComplete="postal-code"
          />

          <Input
            type="text"
            label="Country"
            value={vm.country}
            onChange={e => vm.setCountry(e.target.value)}
            autoComplete="country-name"
          />
        </div>

        {vm.errors.submit && (
          <Alert variant="error">
            <p className="text-sm">{vm.errors.submit}</p>
          </Alert>
        )}

        <div className="flex flex-col sm:flex-row gap-3">
          <Button type="submit" disabled={vm.isSubmitting} variant="primary">
            {vm.isSubmitting
              ? vm.isEditMode
                ? 'Updating...'
                : 'Creating...'
              : vm.isEditMode
                ? 'Update Customer'
                : 'Create Customer'}
          </Button>
          <Button type="button" onClick={vm.handleCancel} variant="secondary">
            Cancel
          </Button>
        </div>
      </form>
    </div>
  );
};
