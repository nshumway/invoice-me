import { describe, it, expect, vi, beforeEach } from 'vitest';
import { invoiceApi } from './invoiceApi';
import { apiClient } from './client';
import type {
  Invoice,
  InvoiceListItem,
  CreateInvoiceRequest,
  UpdateInvoiceRequest,
} from '../models/Invoice';
import type { ApiResponse } from '../models/ApiResponse';

vi.mock('./client');

describe('invoiceApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('create', () => {
    it('should create a new invoice', async () => {
      const request: CreateInvoiceRequest = {
        customerId: '123',
        notes: 'Test invoice',
      };

      const mockInvoice: Invoice = {
        id: '456',
        customerId: '123',
        customerName: 'Test Customer',
        invoiceNumber: 'INV-2025-11-09-001',
        invoiceDate: null,
        status: 'DRAFT',
        total: 0,
        amountPaid: 0,
        notes: 'Test invoice',
        createdAt: '2025-11-09T12:00:00Z',
        createdBy: 'user-1',
        lastModifiedAt: '2025-11-09T12:00:00Z',
        lastModifiedBy: 'user-1',
        version: 0,
      };

      const mockResponse: ApiResponse<Invoice> = {
        data: mockInvoice,
        success: true,
      };

      vi.mocked(apiClient.post).mockResolvedValue({ data: mockResponse });

      const result = await invoiceApi.create(request);

      expect(apiClient.post).toHaveBeenCalledWith('/invoices', request);
      expect(result).toEqual(mockInvoice);
    });
  });

  describe('listAll', () => {
    it('should list all invoices', async () => {
      const mockInvoices: InvoiceListItem[] = [
        {
          id: '1',
          invoiceNumber: 'INV-001',
          customerName: 'Customer 1',
          invoiceDate: '2025-11-09',
          status: 'DRAFT',
          total: 100,
          amountPaid: 0,
        },
      ];

      const mockResponse: ApiResponse<InvoiceListItem[]> = {
        data: mockInvoices,
        success: true,
      };

      vi.mocked(apiClient.get).mockResolvedValue({ data: mockResponse });

      const result = await invoiceApi.listAll();

      expect(apiClient.get).toHaveBeenCalledWith('/invoices');
      expect(result).toEqual(mockInvoices);
    });

    it('should filter invoices by customerId', async () => {
      const mockInvoices: InvoiceListItem[] = [];
      const mockResponse: ApiResponse<InvoiceListItem[]> = {
        data: mockInvoices,
        success: true,
      };

      vi.mocked(apiClient.get).mockResolvedValue({ data: mockResponse });

      await invoiceApi.listAll('customer-123');

      expect(apiClient.get).toHaveBeenCalledWith('/invoices?customerId=customer-123');
    });
  });

  describe('getById', () => {
    it('should get invoice by id', async () => {
      const mockInvoice: Invoice = {
        id: '456',
        customerId: '123',
        customerName: 'Test Customer',
        invoiceNumber: 'INV-2025-11-09-001',
        invoiceDate: null,
        status: 'DRAFT',
        total: 0,
        amountPaid: 0,
        notes: 'Test invoice',
        createdAt: '2025-11-09T12:00:00Z',
        createdBy: 'user-1',
        lastModifiedAt: '2025-11-09T12:00:00Z',
        lastModifiedBy: 'user-1',
        version: 0,
      };

      const mockResponse: ApiResponse<Invoice> = {
        data: mockInvoice,
        success: true,
      };

      vi.mocked(apiClient.get).mockResolvedValue({ data: mockResponse });

      const result = await invoiceApi.getById('456');

      expect(apiClient.get).toHaveBeenCalledWith('/invoices/456');
      expect(result).toEqual(mockInvoice);
    });
  });

  describe('update', () => {
    it('should update an invoice', async () => {
      const request: UpdateInvoiceRequest = {
        id: '456',
        notes: 'Updated notes',
        version: 0,
      };

      const mockInvoice: Invoice = {
        id: '456',
        customerId: '123',
        customerName: 'Test Customer',
        invoiceNumber: 'INV-2025-11-09-001',
        invoiceDate: null,
        status: 'DRAFT',
        total: 0,
        amountPaid: 0,
        notes: 'Updated notes',
        createdAt: '2025-11-09T12:00:00Z',
        createdBy: 'user-1',
        lastModifiedAt: '2025-11-09T12:01:00Z',
        lastModifiedBy: 'user-1',
        version: 1,
      };

      const mockResponse: ApiResponse<Invoice> = {
        data: mockInvoice,
        success: true,
      };

      vi.mocked(apiClient.put).mockResolvedValue({ data: mockResponse });

      const result = await invoiceApi.update('456', request);

      expect(apiClient.put).toHaveBeenCalledWith('/invoices/456', request);
      expect(result).toEqual(mockInvoice);
    });
  });

  describe('markAsSent', () => {
    it('should mark invoice as sent', async () => {
      const mockInvoice: Invoice = {
        id: '456',
        customerId: '123',
        customerName: 'Test Customer',
        invoiceNumber: 'INV-2025-11-09-001',
        invoiceDate: '2025-11-09T12:00:00Z',
        status: 'SENT',
        total: 100,
        amountPaid: 0,
        notes: 'Test invoice',
        createdAt: '2025-11-09T12:00:00Z',
        createdBy: 'user-1',
        lastModifiedAt: '2025-11-09T12:01:00Z',
        lastModifiedBy: 'user-1',
        version: 1,
      };

      const mockResponse: ApiResponse<Invoice> = {
        data: mockInvoice,
        success: true,
      };

      vi.mocked(apiClient.post).mockResolvedValue({ data: mockResponse });

      const result = await invoiceApi.markAsSent('456', 0);

      expect(apiClient.post).toHaveBeenCalledWith('/invoices/456/send', { version: 0 });
      expect(result).toEqual(mockInvoice);
    });
  });

  describe('delete', () => {
    it('should delete an invoice', async () => {
      vi.mocked(apiClient.delete).mockResolvedValue({ data: undefined });

      await invoiceApi.delete('456', 0);

      expect(apiClient.delete).toHaveBeenCalledWith('/invoices/456?version=0');
    });
  });
});
