export type PaymentMethod = 'CASH' | 'CHECK' | 'CREDIT_CARD' | 'BANK_TRANSFER' | 'OTHER';

export interface Payment {
  id: string;
  invoiceId: string;
  paymentDate: string;
  amount: string;
  paymentMethod: PaymentMethod;
  referenceNumber: string | null;
  notes: string | null;
  customerName: string;
  createdAt: string;
  createdBy: string;
  lastModifiedAt: string;
  lastModifiedBy: string;
  version: number;
}

export interface RecordPaymentRequest {
  invoiceId: string;
  paymentDate: string; // ISO format
  amount: string;
  paymentMethod: PaymentMethod;
  referenceNumber?: string;
  notes?: string;
}

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CASH: 'Cash',
  CHECK: 'Check',
  CREDIT_CARD: 'Credit Card',
  BANK_TRANSFER: 'Bank Transfer',
  OTHER: 'Other',
};
