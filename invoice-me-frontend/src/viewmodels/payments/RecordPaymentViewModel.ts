import { useState, useMemo } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { paymentApi } from '../../api/paymentApi';
import type { RecordPaymentRequest, PaymentMethod } from '../../models/Payment';
import type { Invoice } from '../../models/Invoice';

export const useRecordPaymentViewModel = (invoice: Invoice, onSuccess: () => void) => {
  const queryClient = useQueryClient();

  const [paymentDate, setPaymentDate] = useState(new Date().toISOString().split('T')[0]); // Today
  const [amount, setAmount] = useState('');
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CASH');
  const [referenceNumber, setReferenceNumber] = useState('');
  const [notes, setNotes] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});

  // Calculate balance due
  const balanceDue = useMemo(() => {
    const total = parseFloat(invoice.total.toString());
    const paid = parseFloat(invoice.amountPaid.toString());
    return (total - paid).toFixed(2);
  }, [invoice]);

  const recordMutation = useMutation({
    mutationFn: paymentApi.record,
    onSuccess: () => {
      // Invalidate invoice query to refresh data (status, amountPaid may change)
      queryClient.invalidateQueries({ queryKey: ['invoices', 'detail', invoice.id] });
      queryClient.invalidateQueries({ queryKey: ['invoices', 'list'] });
      queryClient.invalidateQueries({ queryKey: ['payments', 'invoice', invoice.id] });
      // Invalidate customer queries since outstanding amount changes when payment is recorded
      queryClient.invalidateQueries({ queryKey: ['customers'] });

      // Reset form
      setAmount('');
      setReferenceNumber('');
      setNotes('');
      setErrors({});

      onSuccess();
    },
    onError: (error: unknown) => {
      const apiError = error as { response?: { data?: { message?: string } } };
      setErrors({ submit: apiError.response?.data?.message || 'Failed to record payment' });
    },
  });

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!paymentDate) {
      newErrors.paymentDate = 'Payment date is required';
    } else if (invoice.invoiceDate) {
      // Check not before invoice date
      const payDate = new Date(paymentDate);
      const invDate = new Date(invoice.invoiceDate);
      if (payDate < invDate) {
        newErrors.paymentDate = 'Payment date cannot be before invoice date';
      }
    }

    if (!amount || parseFloat(amount) <= 0) {
      newErrors.amount = 'Amount must be greater than 0';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    const formData: RecordPaymentRequest = {
      invoiceId: invoice.id,
      paymentDate: new Date(paymentDate).toISOString(),
      amount,
      paymentMethod,
      referenceNumber: referenceNumber || undefined,
      notes: notes || undefined,
    };

    recordMutation.mutate(formData);
  };

  return {
    paymentDate,
    setPaymentDate,
    amount,
    setAmount,
    paymentMethod,
    setPaymentMethod,
    referenceNumber,
    setReferenceNumber,
    notes,
    setNotes,
    balanceDue,
    errors,
    handleSubmit,
    isSubmitting: recordMutation.isPending,
  };
};
