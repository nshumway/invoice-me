import { apiClient } from './client';
import type { ApiResponse } from '../models/ApiResponse';
import type { LineItem, CreateLineItemRequest, UpdateLineItemRequest } from '../models/LineItem';

export const lineItemApi = {
  // POST /api/invoices/:invoiceId/line-items
  create: async (request: CreateLineItemRequest): Promise<LineItem> => {
    const response = await apiClient.post<ApiResponse<LineItem>>(
      `/invoices/${request.invoiceId}/line-items`,
      request
    );
    return response.data.data;
  },

  // GET /api/invoices/:invoiceId/line-items
  listForInvoice: async (invoiceId: string): Promise<LineItem[]> => {
    const response = await apiClient.get<ApiResponse<LineItem[]>>(
      `/invoices/${invoiceId}/line-items`
    );
    return response.data.data;
  },

  // PUT /api/invoices/:invoiceId/line-items/:lineItemId
  update: async (invoiceId: string, request: UpdateLineItemRequest): Promise<LineItem> => {
    const response = await apiClient.put<ApiResponse<LineItem>>(
      `/invoices/${invoiceId}/line-items/${request.id}`,
      request
    );
    return response.data.data;
  },

  // DELETE /api/invoices/:invoiceId/line-items/:lineItemId?version=X
  delete: async (invoiceId: string, lineItemId: string, version: number): Promise<void> => {
    await apiClient.delete(`/invoices/${invoiceId}/line-items/${lineItemId}?version=${version}`);
  },
};
