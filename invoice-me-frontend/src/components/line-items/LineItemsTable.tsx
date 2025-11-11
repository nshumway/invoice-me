import { useState } from 'react';
import type { LineItem, UpdateLineItemRequest } from '../../models/LineItem';
import { useIsMobile } from '../../hooks/useMediaQuery';
import { Button, Input } from '../shared';

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
  const isMobile = useIsMobile();

  if (!lineItems || lineItems.length === 0) {
    return (
      <div className="bg-gray-800 rounded-lg p-6 text-center border border-gray-700">
        <p className="text-gray-400">No line items yet</p>
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

  // Mobile card layout
  if (isMobile) {
    return (
      <div className="space-y-4">
        {lineItems.map(item => (
          <div
            key={item.id}
            className="bg-gray-800 border border-gray-700 rounded-lg p-4 space-y-3"
          >
            {editingId === item.id ? (
              // Edit mode
              <>
                <Input
                  type="text"
                  label="Description"
                  value={editDescription}
                  onChange={e => setEditDescription(e.target.value)}
                  disabled={isSubmitting}
                />
                <div className="grid grid-cols-2 gap-3">
                  <Input
                    type="number"
                    label="Quantity"
                    step="0.01"
                    value={editQuantity}
                    onChange={e => setEditQuantity(e.target.value)}
                    disabled={isSubmitting}
                  />
                  <Input
                    type="number"
                    label="Unit Price"
                    step="0.01"
                    value={editUnitPrice}
                    onChange={e => setEditUnitPrice(e.target.value)}
                    disabled={isSubmitting}
                  />
                </div>
                <div className="pt-2 border-t border-gray-700">
                  <p className="text-sm text-gray-400">Line Total</p>
                  <p className="text-lg font-semibold text-gray-100 font-mono">
                    ${calculateLineTotal(editQuantity, editUnitPrice)}
                  </p>
                </div>
                <div className="flex gap-2 pt-2">
                  <Button
                    onClick={() => handleSaveEdit(item)}
                    disabled={isSubmitting}
                    variant="success"
                    size="sm"
                  >
                    Save
                  </Button>
                  <Button
                    onClick={handleCancelEdit}
                    disabled={isSubmitting}
                    variant="secondary"
                    size="sm"
                  >
                    Cancel
                  </Button>
                </div>
              </>
            ) : (
              // View mode
              <>
                <div>
                  <p className="text-sm text-gray-400">Description</p>
                  <p className="text-gray-100">{item.description}</p>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <p className="text-sm text-gray-400">Quantity</p>
                    <p className="text-gray-100 font-mono">{item.quantity.toFixed(2)}</p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-400">Unit Price</p>
                    <p className="text-gray-100 font-mono">${item.unitPrice.toFixed(2)}</p>
                  </div>
                </div>
                <div className="pt-2 border-t border-gray-700">
                  <p className="text-sm text-gray-400">Line Total</p>
                  <p className="text-lg font-semibold text-gray-100 font-mono">
                    ${item.lineTotal.toFixed(2)}
                  </p>
                </div>
                {canEdit && (
                  <div className="flex gap-2 pt-2">
                    <Button
                      onClick={() => handleEdit(item)}
                      disabled={isSubmitting}
                      variant="primary"
                      size="sm"
                    >
                      Edit
                    </Button>
                    <Button
                      onClick={() => handleDelete(item)}
                      disabled={isSubmitting}
                      variant="danger"
                      size="sm"
                    >
                      Delete
                    </Button>
                  </div>
                )}
              </>
            )}
          </div>
        ))}
      </div>
    );
  }

  // Desktop table layout
  return (
    <div className="bg-gray-800 rounded-lg shadow border border-gray-700 overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead className="bg-gray-700 border-b border-gray-600">
            <tr>
              <th className="text-left p-4 font-medium text-gray-200">Description</th>
              <th className="text-right p-4 font-medium text-gray-200">Quantity</th>
              <th className="text-right p-4 font-medium text-gray-200">Unit Price</th>
              <th className="text-right p-4 font-medium text-gray-200">Line Total</th>
              {canEdit && <th className="text-right p-4 font-medium text-gray-200">Actions</th>}
            </tr>
          </thead>
          <tbody>
            {lineItems.map(item => (
              <tr key={item.id} className="border-b border-gray-700">
                {editingId === item.id ? (
                  // Edit mode
                  <>
                    <td className="p-4">
                      <input
                        type="text"
                        value={editDescription}
                        onChange={e => setEditDescription(e.target.value)}
                        className="w-full bg-gray-700 text-gray-100 border border-gray-600 rounded px-2 py-1 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                        disabled={isSubmitting}
                      />
                    </td>
                    <td className="p-4">
                      <input
                        type="number"
                        step="0.01"
                        value={editQuantity}
                        onChange={e => setEditQuantity(e.target.value)}
                        className="w-full bg-gray-700 text-gray-100 border border-gray-600 rounded px-2 py-1 text-right font-mono focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                        disabled={isSubmitting}
                      />
                    </td>
                    <td className="p-4">
                      <input
                        type="number"
                        step="0.01"
                        value={editUnitPrice}
                        onChange={e => setEditUnitPrice(e.target.value)}
                        className="w-full bg-gray-700 text-gray-100 border border-gray-600 rounded px-2 py-1 text-right font-mono focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                        disabled={isSubmitting}
                      />
                    </td>
                    <td className="p-4 text-right font-mono font-semibold text-gray-100">
                      ${calculateLineTotal(editQuantity, editUnitPrice)}
                    </td>
                    <td className="p-4 text-right space-x-2">
                      <Button
                        onClick={() => handleSaveEdit(item)}
                        disabled={isSubmitting}
                        variant="success"
                        size="sm"
                      >
                        Save
                      </Button>
                      <Button
                        onClick={handleCancelEdit}
                        disabled={isSubmitting}
                        variant="secondary"
                        size="sm"
                      >
                        Cancel
                      </Button>
                    </td>
                  </>
                ) : (
                  // View mode
                  <>
                    <td className="p-4 text-gray-100">{item.description}</td>
                    <td className="p-4 text-right font-mono text-gray-100">
                      {item.quantity.toFixed(2)}
                    </td>
                    <td className="p-4 text-right font-mono text-gray-100">
                      ${item.unitPrice.toFixed(2)}
                    </td>
                    <td className="p-4 text-right font-mono font-semibold text-gray-100">
                      ${item.lineTotal.toFixed(2)}
                    </td>
                    {canEdit && (
                      <td className="p-4 text-right space-x-2">
                        <Button
                          onClick={() => handleEdit(item)}
                          disabled={isSubmitting}
                          variant="primary"
                          size="sm"
                        >
                          Edit
                        </Button>
                        <Button
                          onClick={() => handleDelete(item)}
                          disabled={isSubmitting}
                          variant="danger"
                          size="sm"
                        >
                          Delete
                        </Button>
                      </td>
                    )}
                  </>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};
