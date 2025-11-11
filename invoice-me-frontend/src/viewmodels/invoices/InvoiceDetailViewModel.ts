import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { invoiceApi } from '../../api/invoiceApi';
import { lineItemApi } from '../../api/lineItemApi';
import { paymentApi } from '../../api/paymentApi';
import type { Invoice, UpdateInvoiceRequest } from '../../models/Invoice';
import type { LineItem, CreateLineItemRequest, UpdateLineItemRequest } from '../../models/LineItem';
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
    queryKey: ['invoices', 'detail', invoiceId],
    queryFn: () => invoiceApi.getById(invoiceId),
    enabled: !!invoiceId,
  });

  // Query for line items
  const { data: lineItems = [], refetch: refetchLineItems } = useQuery<LineItem[]>({
    queryKey: ['lineItems', 'invoice', invoiceId],
    queryFn: () => lineItemApi.listForInvoice(invoiceId),
    enabled: !!invoiceId,
  });

  // Query for payments
  const { data: payments = [], isLoading: paymentsLoading } = useQuery<Payment[]>({
    queryKey: ['payments', 'invoice', invoiceId],
    queryFn: () => paymentApi.listForInvoice(invoiceId),
    enabled: !!invoiceId && (invoice?.status === 'SENT' || invoice?.status === 'PAID'),
  });

  // Line item mutations
  const createLineItemMutation = useMutation({
    mutationFn: (request: CreateLineItemRequest) => lineItemApi.create(request),
    onSuccess: () => {
      refetchLineItems();
      queryClient.invalidateQueries({ queryKey: ['invoices', 'detail', invoiceId] });
    },
  });

  const updateLineItemMutation = useMutation({
    mutationFn: (request: UpdateLineItemRequest) => lineItemApi.update(invoiceId, request),
    onSuccess: () => {
      refetchLineItems();
      queryClient.invalidateQueries({ queryKey: ['invoices', 'detail', invoiceId] });
    },
  });

  const deleteLineItemMutation = useMutation({
    mutationFn: ({ lineItemId, version }: { lineItemId: string; version: number }) =>
      lineItemApi.delete(invoiceId, lineItemId, version),
    onSuccess: () => {
      refetchLineItems();
      queryClient.invalidateQueries({ queryKey: ['invoices', 'detail', invoiceId] });
    },
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
      // Invalidate customer queries since outstanding amount changes when invoice is sent
      queryClient.invalidateQueries({ queryKey: ['customers'] });
    },
  });

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: (version: number) => invoiceApi.delete(invoiceId, version),
    onSuccess: () => {
      // Invalidate customer queries since draft invoice count changes
      queryClient.invalidateQueries({ queryKey: ['customers'] });
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
  const showPayments = invoice?.status === 'SENT' || invoice?.status === 'PAID';
  const canRecordPayment = showPayments && invoice && invoice.amountPaid < invoice.total;

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
      id: invoice.id,
      version: invoice.version,
      notes: notes.trim() || undefined,
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
    showPayments,
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
    // Line items
    lineItems,
    createLineItemMutation,
    updateLineItemMutation,
    deleteLineItemMutation,
  };
};
