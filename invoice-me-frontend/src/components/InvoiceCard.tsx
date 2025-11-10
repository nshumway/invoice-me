import type { InvoiceListItem } from '../models/Invoice';

interface InvoiceCardProps {
  invoice: InvoiceListItem;
  onView?: (id: string) => void;
  onDelete?: (id: string) => void;
}

export function InvoiceCard({ invoice, onView, onDelete }: InvoiceCardProps) {
  const statusColors = {
    DRAFT: 'bg-gray-100 text-gray-800',
    SENT: 'bg-blue-100 text-blue-800',
    PAID: 'bg-green-100 text-green-800',
    OVERDUE: 'bg-red-100 text-red-800',
    CANCELLED: 'bg-gray-100 text-gray-600',
  };

  const statusColor = statusColors[invoice.status as keyof typeof statusColors] || 'bg-gray-100';

  return (
    <div
      className="border rounded-lg p-4 hover:shadow-md transition-shadow"
      data-testid="invoice-card"
    >
      <div className="flex justify-between items-start">
        <div>
          <h3 className="font-semibold text-lg" data-testid="invoice-number">
            {invoice.invoiceNumber}
          </h3>
          <p className="text-gray-600" data-testid="customer-name">
            {invoice.customerName}
          </p>
        </div>
        <span className={`px-2 py-1 rounded text-sm ${statusColor}`} data-testid="invoice-status">
          {invoice.status}
        </span>
      </div>

      <div className="mt-4 grid grid-cols-2 gap-2 text-sm">
        <div>
          <p className="text-gray-500">Total</p>
          <p className="font-medium" data-testid="invoice-total">
            ${invoice.total.toFixed(2)}
          </p>
        </div>
        <div>
          <p className="text-gray-500">Amount Paid</p>
          <p className="font-medium" data-testid="amount-paid">
            ${invoice.amountPaid.toFixed(2)}
          </p>
        </div>
      </div>

      {invoice.invoiceDate && (
        <div className="mt-2 text-sm text-gray-500" data-testid="invoice-date">
          Date: {new Date(invoice.invoiceDate).toLocaleDateString()}
        </div>
      )}

      <div className="mt-4 flex gap-2">
        {onView && (
          <button
            onClick={() => onView(invoice.id)}
            className="px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700"
            data-testid="view-button"
          >
            View
          </button>
        )}
        {onDelete && invoice.status === 'DRAFT' && (
          <button
            onClick={() => onDelete(invoice.id)}
            className="px-3 py-1 bg-red-600 text-white rounded hover:bg-red-700"
            data-testid="delete-button"
          >
            Delete
          </button>
        )}
      </div>
    </div>
  );
}
