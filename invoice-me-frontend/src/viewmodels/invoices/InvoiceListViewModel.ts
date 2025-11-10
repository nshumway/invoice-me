import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { invoiceApi } from '../../api/invoiceApi';
import type { InvoiceListItem } from '../../models/Invoice';

export const InvoiceListViewModel = () => {
  const navigate = useNavigate();
  const [customerFilter, setCustomerFilter] = useState<string | undefined>(undefined);

  // Query for invoice list
  const {
    data: invoices,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery<InvoiceListItem[]>({
    queryKey: ['invoices', 'list', customerFilter],
    queryFn: () => invoiceApi.listAll(customerFilter),
  });

  // Computed properties
  const errorMessage = isError
    ? error instanceof Error
      ? error.message
      : 'Failed to load invoices'
    : null;

  // Actions
  const handleCreateNew = () => {
    navigate('/invoices/new');
  };

  const handleViewInvoice = (invoiceId: string) => {
    navigate(`/invoices/${invoiceId}`);
  };

  const handleFilterByCustomer = (customerId: string | undefined) => {
    setCustomerFilter(customerId);
  };

  const handleClearFilter = () => {
    setCustomerFilter(undefined);
  };

  // Expose state and actions to view
  return {
    invoices,
    isLoading,
    isError,
    errorMessage,
    customerFilter,
    handleCreateNew,
    handleViewInvoice,
    handleFilterByCustomer,
    handleClearFilter,
    refetch,
  };
};
