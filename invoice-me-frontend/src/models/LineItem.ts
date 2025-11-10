export interface LineItem {
  id: string;
  invoiceId: string;
  description: string;
  quantity: number;
  unitPrice: number;
  customerName: string;
  lineTotal: number;
  createdAt: string;
  createdBy: string;
  lastModifiedAt: string;
  lastModifiedBy: string;
  version: number;
}

export interface CreateLineItemRequest {
  invoiceId: string;
  description: string;
  quantity: number;
  unitPrice: number;
}

export interface UpdateLineItemRequest {
  id: string;
  version: number;
  description: string;
  quantity: number;
  unitPrice: number;
}
