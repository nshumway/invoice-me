import { apiClient } from './client';
import type { ApiResponse } from '../models/ApiResponse';
import type { Payment, RecordPaymentRequest } from '../models/Payment';

export const paymentApi = {
  /**
   * Record a new payment for an invoice
   */
  record: async (request: RecordPaymentRequest): Promise<Payment> => {
    const response = await apiClient.post<ApiResponse<Payment>>('/payments', request);
    return response.data.data;
  },

  /**
   * Get a payment by ID
   */
  getById: async (paymentId: string): Promise<Payment> => {
    const response = await apiClient.get<ApiResponse<Payment>>(`/payments/${paymentId}`);
    return response.data.data;
  },

  /**
   * List all payments for an invoice
   */
  listForInvoice: async (invoiceId: string): Promise<Payment[]> => {
    const response = await apiClient.get<ApiResponse<Payment[]>>(`/invoices/${invoiceId}/payments`);
    return response.data.data;
  },
};
