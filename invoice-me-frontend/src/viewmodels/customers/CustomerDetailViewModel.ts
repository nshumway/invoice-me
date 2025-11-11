import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { customerApi } from '../../api/customerApi';
import { invoiceApi } from '../../api/invoiceApi';
import type { Customer } from '../../models/Customer';
import type { InvoiceListItem } from '../../models/Invoice';

export const CustomerDetailViewModel = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [invoiceStatusFilter, setInvoiceStatusFilter] = useState<string | undefined>(undefined);

  // Query for customer details
  const {
    data: customer,
    isLoading,
    isError,
    error,
  } = useQuery<Customer>({
    queryKey: ['customers', 'detail', id],
    queryFn: () => customerApi.getById(id!),
    enabled: !!id,
  });

  // Query for customer invoices
  const { data: invoices, isLoading: invoicesLoading } = useQuery<InvoiceListItem[]>({
    queryKey: ['invoices', 'customer', id, invoiceStatusFilter],
    queryFn: () => invoiceApi.listAll(id!, invoiceStatusFilter),
    enabled: !!id,
  });

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: () => customerApi.delete(id!, customer!.version),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'] });
      navigate('/customers');
    },
    onError: (error: unknown) => {
      console.error('Delete failed:', error);
      const err = error as { response?: { data?: { message?: string } } };
      alert(err.response?.data?.message || 'Failed to delete customer');
    },
  });

  // Computed properties
  const errorMessage = isError
    ? error instanceof Error
      ? error.message
      : 'Failed to load customer'
    : null;

  // Actions
  const handleBack = () => {
    navigate('/customers');
  };

  const handleEdit = () => {
    navigate(`/customers/${id}/edit`);
  };

  const handleDelete = () => {
    setShowDeleteDialog(true);
  };

  const handleConfirmDelete = () => {
    deleteMutation.mutate();
    setShowDeleteDialog(false);
  };

  const handleCancelDelete = () => {
    setShowDeleteDialog(false);
  };

  const handleFilterByStatus = (status: string | undefined) => {
    setInvoiceStatusFilter(status);
  };

  const handleViewInvoice = (invoiceId: string) => {
    navigate(`/invoices/${invoiceId}`);
  };

  const handleCreateInvoice = () => {
    navigate(`/invoices/new?customerId=${id}`);
  };

  return {
    customer,
    isLoading,
    isError,
    errorMessage,
    showDeleteDialog,
    invoices,
    invoicesLoading,
    invoiceStatusFilter,
    handleBack,
    handleEdit,
    handleDelete,
    handleConfirmDelete,
    handleCancelDelete,
    handleFilterByStatus,
    handleViewInvoice,
    handleCreateInvoice,
    isDeleting: deleteMutation.isPending,
  };
};
