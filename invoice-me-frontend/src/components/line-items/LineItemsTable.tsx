import { useState } from 'react';
import type { LineItem, UpdateLineItemRequest } from '../../models/LineItem';

interface LineItemsTableProps {
  lineItems: LineItem[];
  canEdit: boolean;
  onUpdate: (request: UpdateLineItemRequest) => void;
  onDelete: (lineItemId: string, version: number) => void;
  isSubmitting: boolean;
}

export const LineItemsTable = ({
  lineItems,
  canEdit,
  onUpdate,
  onDelete,
  isSubmitting,
}: LineItemsTableProps) => {
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editDescription, setEditDescription] = useState('');
  const [editQuantity, setEditQuantity] = useState('');
  const [editUnitPrice, setEditUnitPrice] = useState('');

  if (!lineItems || lineItems.length === 0) {
    return (
      <div className="bg-gray-50 rounded-lg p-6 text-center">
        <p className="text-gray-600">No line items yet</p>
      </div>
    );
  }

  const handleEdit = (lineItem: LineItem) => {
    setEditingId(lineItem.id);
    setEditDescription(lineItem.description);
    setEditQuantity(lineItem.quantity.toString());
    setEditUnitPrice(lineItem.unitPrice.toString());
  };

  const handleCancelEdit = () => {
    setEditingId(null);
    setEditDescription('');
    setEditQuantity('');
    setEditUnitPrice('');
  };

  const handleSaveEdit = (lineItem: LineItem) => {
    const request: UpdateLineItemRequest = {
      id: lineItem.id,
      version: lineItem.version,
      description: editDescription.trim(),
      quantity: parseFloat(editQuantity),
      unitPrice: parseFloat(editUnitPrice),
    };

    onUpdate(request);
    handleCancelEdit();
  };

  const handleDelete = (lineItem: LineItem) => {
    if (window.confirm(`Delete line item "${lineItem.description}"?`)) {
      onDelete(lineItem.id, lineItem.version);
    }
  };

  const calculateLineTotal = (quantity: string, unitPrice: string): string => {
    const qty = parseFloat(quantity);
    const price = parseFloat(unitPrice);
    if (!isNaN(qty) && !isNaN(price)) {
      return (qty * price).toFixed(2);
    }
    return '0.00';
  };

  return (
    <div className="bg-white rounded-lg shadow overflow-hidden">
      <table className="w-full">
        <thead className="bg-gray-50 border-b">
          <tr>
            <th className="text-left p-4 font-medium text-gray-700">Description</th>
            <th className="text-right p-4 font-medium text-gray-700">Quantity</th>
            <th className="text-right p-4 font-medium text-gray-700">Unit Price</th>
            <th className="text-right p-4 font-medium text-gray-700">Line Total</th>
            {canEdit && <th className="text-right p-4 font-medium text-gray-700">Actions</th>}
          </tr>
        </thead>
        <tbody>
          {lineItems.map(item => (
            <tr key={item.id} className="border-b">
              {editingId === item.id ? (
                // Edit mode
                <>
                  <td className="p-4">
                    <input
                      type="text"
                      value={editDescription}
                      onChange={e => setEditDescription(e.target.value)}
                      className="w-full border rounded px-2 py-1"
                      disabled={isSubmitting}
                    />
                  </td>
                  <td className="p-4">
                    <input
                      type="number"
                      step="0.01"
                      value={editQuantity}
                      onChange={e => setEditQuantity(e.target.value)}
                      className="w-full border rounded px-2 py-1 text-right font-mono"
                      disabled={isSubmitting}
                    />
                  </td>
                  <td className="p-4">
                    <input
                      type="number"
                      step="0.01"
                      value={editUnitPrice}
                      onChange={e => setEditUnitPrice(e.target.value)}
                      className="w-full border rounded px-2 py-1 text-right font-mono"
                      disabled={isSubmitting}
                    />
                  </td>
                  <td className="p-4 text-right font-mono font-semibold">
                    ${calculateLineTotal(editQuantity, editUnitPrice)}
                  </td>
                  <td className="p-4 text-right space-x-2">
                    <button
                      onClick={() => handleSaveEdit(item)}
                      disabled={isSubmitting}
                      className="text-green-600 hover:underline disabled:opacity-50"
                    >
                      Save
                    </button>
                    <button
                      onClick={handleCancelEdit}
                      disabled={isSubmitting}
                      className="text-gray-600 hover:underline disabled:opacity-50"
                    >
                      Cancel
                    </button>
                  </td>
                </>
              ) : (
                // View mode
                <>
                  <td className="p-4">{item.description}</td>
                  <td className="p-4 text-right font-mono">{item.quantity.toFixed(2)}</td>
                  <td className="p-4 text-right font-mono">${item.unitPrice.toFixed(2)}</td>
                  <td className="p-4 text-right font-mono font-semibold">
                    ${item.lineTotal.toFixed(2)}
                  </td>
                  {canEdit && (
                    <td className="p-4 text-right space-x-2">
                      <button
                        onClick={() => handleEdit(item)}
                        disabled={isSubmitting}
                        className="text-blue-600 hover:underline disabled:opacity-50"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => handleDelete(item)}
                        disabled={isSubmitting}
                        className="text-red-600 hover:underline disabled:opacity-50"
                      >
                        Delete
                      </button>
                    </td>
                  )}
                </>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};
