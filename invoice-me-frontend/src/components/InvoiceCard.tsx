import type { InvoiceListItem } from '../models/Invoice';
import { StatusBadge } from './shared/StatusBadge';
import { Button } from './shared/Button';

interface InvoiceCardProps {
  invoice: InvoiceListItem;
  onView?: (id: string) => void;
  onDelete?: (id: string) => void;
}

export function InvoiceCard({ invoice, onView, onDelete }: InvoiceCardProps) {
  return (
    <div
      className="bg-gray-800 border border-gray-700 rounded-lg p-4 hover:shadow-md transition-shadow"
      data-testid="invoice-card"
    >
      <div className="flex justify-between items-start">
        <div>
          <h3 className="font-semibold text-lg text-gray-100" data-testid="invoice-number">
            {invoice.invoiceNumber}
          </h3>
          <p className="text-gray-400" data-testid="customer-name">
            {invoice.customerName}
          </p>
        </div>
        <span data-testid="invoice-status">
          <StatusBadge
            status={invoice.status as 'DRAFT' | 'SENT' | 'PAID' | 'OVERDUE' | 'CANCELLED'}
          />
        </span>
      </div>

      <div className="mt-4 grid grid-cols-2 gap-2 text-sm">
        <div>
          <p className="text-gray-500">Total</p>
          <p className="font-medium text-gray-200" data-testid="invoice-total">
            ${invoice.total.toFixed(2)}
          </p>
        </div>
        <div>
          <p className="text-gray-500">Amount Paid</p>
          <p className="font-medium text-gray-200" data-testid="amount-paid">
            ${invoice.amountPaid.toFixed(2)}
          </p>
        </div>
      </div>

      {invoice.invoiceDate && (
        <div className="mt-2 text-sm text-gray-500" data-testid="invoice-date">
          Date: {new Date(invoice.invoiceDate).toLocaleDateString()}
        </div>
      )}

      <div className="mt-4 flex flex-wrap gap-2">
        {onView && (
          <Button onClick={() => onView(invoice.id)} size="sm" data-testid="view-button">
            View
          </Button>
        )}
        {onDelete && invoice.status === 'DRAFT' && (
          <Button
            onClick={() => onDelete(invoice.id)}
            variant="danger"
            size="sm"
            data-testid="delete-button"
          >
            Delete
          </Button>
        )}
      </div>
    </div>
  );
}
