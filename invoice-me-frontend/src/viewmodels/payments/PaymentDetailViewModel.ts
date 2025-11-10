import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { paymentApi } from '../../api/paymentApi';
import type { Payment } from '../../models/Payment';

export const usePaymentDetailViewModel = (paymentId: string) => {
  const navigate = useNavigate();

  // Query for payment details
  const {
    data: payment,
    isLoading,
    isError,
    error,
  } = useQuery<Payment>({
    queryKey: ['payments', paymentId],
    queryFn: () => paymentApi.getById(paymentId),
    enabled: !!paymentId,
  });

  const errorMessage = isError
    ? error instanceof Error
      ? error.message
      : 'Failed to load payment'
    : null;

  const handleBack = () => {
    if (payment) {
      navigate(`/invoices/${payment.invoiceId}`);
    } else {
      navigate('/invoices');
    }
  };

  return {
    payment,
    isLoading,
    isError,
    errorMessage,
    handleBack,
  };
};
