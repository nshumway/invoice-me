import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { invoiceApi } from '../../api/invoiceApi';
import type { InvoiceListItem } from '../../models/Invoice';

export const InvoiceListViewModel = () => {
  const navigate = useNavigate();
  const [customerFilter, setCustomerFilter] = useState<string | undefined>(undefined);
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);

  // Query for invoice list
  const {
    data: invoices,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery<InvoiceListItem[]>({
    queryKey: ['invoices', 'list', customerFilter, statusFilter],
    queryFn: () => invoiceApi.listAll(customerFilter, statusFilter),
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

  const handleFilterByStatus = (status: string | undefined) => {
    setStatusFilter(status);
  };

  const handleClearStatusFilter = () => {
    setStatusFilter(undefined);
  };

  // Expose state and actions to view
  return {
    invoices,
    isLoading,
    isError,
    errorMessage,
    customerFilter,
    statusFilter,
    handleCreateNew,
    handleViewInvoice,
    handleFilterByCustomer,
    handleClearFilter,
    handleFilterByStatus,
    handleClearStatusFilter,
    refetch,
  };
};
