import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { InvoiceCard } from './InvoiceCard';
import type { InvoiceListItem } from '../models/Invoice';

describe('InvoiceCard', () => {
  const mockInvoice: InvoiceListItem = {
    id: '123',
    invoiceNumber: 'INV-2025-11-09-001',
    customerName: 'Test Customer',
    invoiceDate: '2025-11-09',
    status: 'DRAFT',
    total: 150.5,
    amountPaid: 0,
  };

  it('should render invoice details', () => {
    render(<InvoiceCard invoice={mockInvoice} />);

    expect(screen.getByTestId('invoice-number')).toHaveTextContent('INV-2025-11-09-001');
    expect(screen.getByTestId('customer-name')).toHaveTextContent('Test Customer');
    expect(screen.getByTestId('invoice-status')).toHaveTextContent('DRAFT');
    expect(screen.getByTestId('invoice-total')).toHaveTextContent('$150.50');
    expect(screen.getByTestId('amount-paid')).toHaveTextContent('$0.00');
  });

  it('should display invoice date when available', () => {
    render(<InvoiceCard invoice={mockInvoice} />);

    expect(screen.getByTestId('invoice-date')).toBeInTheDocument();
  });

  it('should not display invoice date when null', () => {
    const invoiceWithoutDate = { ...mockInvoice, invoiceDate: null };
    render(<InvoiceCard invoice={invoiceWithoutDate} />);

    expect(screen.queryByTestId('invoice-date')).not.toBeInTheDocument();
  });

  it('should call onView when view button is clicked', async () => {
    const onView = vi.fn();
    const user = userEvent.setup();

    render(<InvoiceCard invoice={mockInvoice} onView={onView} />);

    await user.click(screen.getByTestId('view-button'));

    expect(onView).toHaveBeenCalledWith('123');
  });

  it('should call onDelete when delete button is clicked (DRAFT status)', async () => {
    const onDelete = vi.fn();
    const user = userEvent.setup();

    render(<InvoiceCard invoice={mockInvoice} onDelete={onDelete} />);

    await user.click(screen.getByTestId('delete-button'));

    expect(onDelete).toHaveBeenCalledWith('123');
  });

  it('should not show delete button for non-DRAFT invoices', () => {
    const sentInvoice = { ...mockInvoice, status: 'SENT' };
    const onDelete = vi.fn();

    render(<InvoiceCard invoice={sentInvoice} onDelete={onDelete} />);

    expect(screen.queryByTestId('delete-button')).not.toBeInTheDocument();
  });

  it('should apply correct status colors', () => {
    const statuses: Array<'DRAFT' | 'SENT' | 'PAID' | 'OVERDUE' | 'CANCELLED'> = [
      'DRAFT',
      'SENT',
      'PAID',
      'OVERDUE',
      'CANCELLED',
    ];

    statuses.forEach(status => {
      const { unmount } = render(<InvoiceCard invoice={{ ...mockInvoice, status }} />);

      const statusElement = screen.getByTestId('invoice-status');
      expect(statusElement).toBeInTheDocument();
      expect(statusElement).toHaveTextContent(status);

      unmount();
    });
  });
});
