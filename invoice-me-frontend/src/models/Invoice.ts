export interface Invoice {
  id: string;
  customerId: string;
  customerName: string;
  invoiceNumber: string;
  invoiceDate: string | null;
  status: 'DRAFT' | 'SENT' | 'PAID' | 'OVERDUE' | 'CANCELLED';
  total: number;
  amountPaid: number;
  notes: string | null;
  createdAt: string;
  createdBy: string;
  lastModifiedAt: string;
  lastModifiedBy: string;
  version: number;
}

export interface CreateInvoiceRequest {
  customerId: string;
  invoiceNumber?: string;
  notes?: string;
}

export interface UpdateInvoiceRequest {
  notes?: string;
  version: number;
}

export interface InvoiceListItem {
  id: string;
  invoiceNumber: string;
  customerName: string;
  invoiceDate: string | null;
  status: string;
  total: number;
  amountPaid: number;
}
