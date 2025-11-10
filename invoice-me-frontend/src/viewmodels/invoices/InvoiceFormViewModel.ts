import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { invoiceApi } from '../../api/invoiceApi';
import { customerApi } from '../../api/customerApi';
import type { CreateInvoiceRequest } from '../../models/Invoice';
import type { CustomerListItem } from '../../models/Customer';

export const InvoiceFormViewModel = () => {
  const navigate = useNavigate();
  const [customerId, setCustomerId] = useState<string>('');
  const [invoiceNumber, setInvoiceNumber] = useState<string>('');
  const [notes, setNotes] = useState<string>('');

  // Query for customer list (for dropdown)
  const { data: customers, isLoading: isLoadingCustomers } = useQuery<CustomerListItem[]>({
    queryKey: ['customers', 'list'],
    queryFn: customerApi.listAll,
  });

  // Mutation for creating invoice
  const createMutation = useMutation({
    mutationFn: (request: CreateInvoiceRequest) => invoiceApi.create(request),
    onSuccess: invoice => {
      navigate(`/invoices/${invoice.id}`);
    },
  });

  // Computed properties
  const isSubmitting = createMutation.isPending;
  const errorMessage = createMutation.isError
    ? createMutation.error instanceof Error
      ? createMutation.error.message
      : 'Failed to create invoice'
    : null;

  const isValid = customerId.trim() !== '';

  // Actions
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isValid) return;

    const request: CreateInvoiceRequest = {
      customerId,
      notes: notes.trim() || undefined,
      invoiceNumber: invoiceNumber.trim() || undefined,
    };

    createMutation.mutate(request);
  };

  const handleCancel = () => {
    navigate('/invoices');
  };

  // Expose state and actions to view
  return {
    customerId,
    setCustomerId,
    invoiceNumber,
    setInvoiceNumber,
    notes,
    setNotes,
    customers,
    isLoadingCustomers,
    isSubmitting,
    isValid,
    errorMessage,
    handleSubmit,
    handleCancel,
  };
};
