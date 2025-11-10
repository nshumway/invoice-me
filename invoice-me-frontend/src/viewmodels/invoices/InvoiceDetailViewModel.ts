import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { invoiceApi } from '../../api/invoiceApi';
import { paymentApi } from '../../api/paymentApi';
import type { Invoice, UpdateInvoiceRequest } from '../../models/Invoice';
import type { Payment } from '../../models/Payment';

export const InvoiceDetailViewModel = (invoiceId: string) => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [isEditing, setIsEditing] = useState(false);
  const [notes, setNotes] = useState<string>('');
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [showPaymentForm, setShowPaymentForm] = useState(false);

  // Query for invoice details
  const {
    data: invoice,
    isLoading,
    isError,
    error,
  } = useQuery<Invoice>({
    queryKey: ['invoices', invoiceId],
    queryFn: () => invoiceApi.getById(invoiceId),
    enabled: !!invoiceId,
  });

  // Query for payments
  const { data: payments = [], isLoading: paymentsLoading } = useQuery<Payment[]>({
    queryKey: ['payments', 'invoice', invoiceId],
    queryFn: () => paymentApi.listForInvoice(invoiceId),
    enabled: !!invoiceId && (invoice?.status === 'SENT' || invoice?.status === 'PAID'),
  });

  // Update mutation
  const updateMutation = useMutation({
    mutationFn: (request: UpdateInvoiceRequest) => invoiceApi.update(invoiceId, request),
    onSuccess: updatedInvoice => {
      queryClient.setQueryData(['invoices', 'detail', invoiceId], updatedInvoice);
      setIsEditing(false);
      setNotes('');
    },
  });

  // Mark as sent mutation
  const markAsSentMutation = useMutation({
    mutationFn: (version: number) => invoiceApi.markAsSent(invoiceId, version),
    onSuccess: updatedInvoice => {
      queryClient.setQueryData(['invoices', 'detail', invoiceId], updatedInvoice);
      queryClient.invalidateQueries({ queryKey: ['invoices', 'list'] });
    },
  });

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: (version: number) => invoiceApi.delete(invoiceId, version),
    onSuccess: () => {
      navigate('/invoices');
    },
  });

  // Computed properties
  const errorMessage = isError
    ? error instanceof Error
      ? error.message
      : 'Failed to load invoice'
    : null;

  const canEdit = invoice?.status === 'DRAFT';
  const canDelete = invoice?.status === 'DRAFT';
  const canMarkAsSent = invoice?.status === 'DRAFT';
  const canRecordPayment = invoice?.status === 'SENT' || invoice?.status === 'PAID';

  const isSubmitting =
    updateMutation.isPending || markAsSentMutation.isPending || deleteMutation.isPending;

  const updateErrorMessage = updateMutation.isError
    ? updateMutation.error instanceof Error
      ? updateMutation.error.message
      : 'Failed to update invoice'
    : null;

  // Actions
  const handleEdit = () => {
    setNotes(invoice?.notes || '');
    setIsEditing(true);
  };

  const handleCancelEdit = () => {
    setIsEditing(false);
    setNotes('');
  };

  const handleSaveEdit = () => {
    if (!invoice) return;

    const request: UpdateInvoiceRequest = {
      notes: notes.trim() || undefined,
      version: invoice.version,
    };

    updateMutation.mutate(request);
  };

  const handleMarkAsSent = () => {
    if (!invoice) return;
    markAsSentMutation.mutate(invoice.version);
  };

  const handleDelete = () => {
    setShowDeleteConfirm(true);
  };

  const handleConfirmDelete = () => {
    if (!invoice) return;
    deleteMutation.mutate(invoice.version);
  };

  const handleCancelDelete = () => {
    setShowDeleteConfirm(false);
  };

  const handleBack = () => {
    navigate('/invoices');
  };

  const handleRecordPayment = () => {
    setShowPaymentForm(true);
  };

  const handlePaymentSuccess = () => {
    setShowPaymentForm(false);
    // Queries will auto-refresh due to invalidation in RecordPaymentViewModel
  };

  const handleCancelPayment = () => {
    setShowPaymentForm(false);
  };

  const handlePaymentClick = (paymentId: string) => {
    navigate(`/payments/${paymentId}`);
  };

  // Expose state and actions to view
  return {
    invoice,
    isLoading,
    isError,
    errorMessage,
    isEditing,
    notes,
    setNotes,
    canEdit,
    canDelete,
    canMarkAsSent,
    canRecordPayment,
    isSubmitting,
    updateErrorMessage,
    showDeleteConfirm,
    showPaymentForm,
    payments,
    paymentsLoading,
    handleEdit,
    handleCancelEdit,
    handleSaveEdit,
    handleMarkAsSent,
    handleDelete,
    handleConfirmDelete,
    handleCancelDelete,
    handleBack,
    handleRecordPayment,
    handlePaymentSuccess,
    handleCancelPayment,
    handlePaymentClick,
  };
};
