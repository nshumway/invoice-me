import { apiClient } from './client';
import type { ApiResponse } from '../models/ApiResponse';
import type {
  Customer,
  CustomerListItem,
  CreateCustomerRequest,
  UpdateCustomerRequest,
} from '../models/Customer';

export const customerApi = {
  // POST /api/customers
  create: async (request: CreateCustomerRequest): Promise<Customer> => {
    const response = await apiClient.post<ApiResponse<Customer>>('/customers', request);
    return response.data.data;
  },

  // GET /api/customers
  listAll: async (): Promise<CustomerListItem[]> => {
    const response = await apiClient.get<ApiResponse<CustomerListItem[]>>('/customers');
    return response.data.data;
  },

  // GET /api/customers/:id
  getById: async (id: string): Promise<Customer> => {
    const response = await apiClient.get<ApiResponse<Customer>>(`/customers/${id}`);
    return response.data.data;
  },

  // PUT /api/customers/:id
  update: async (request: UpdateCustomerRequest): Promise<Customer> => {
    const response = await apiClient.put<ApiResponse<Customer>>(
      `/customers/${request.id}`,
      request
    );
    return response.data.data;
  },

  // DELETE /api/customers/:id?version=X
  delete: async (id: string, version: number): Promise<void> => {
    await apiClient.delete(`/customers/${id}?version=${version}`);
  },
};
