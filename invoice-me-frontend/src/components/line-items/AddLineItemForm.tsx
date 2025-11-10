import { useState, useMemo } from 'react';
import type { CreateLineItemRequest } from '../../models/LineItem';

interface AddLineItemFormProps {
  invoiceId: string;
  onAdd: (request: CreateLineItemRequest) => void;
  isSubmitting: boolean;
  canEdit: boolean;
}

export const AddLineItemForm = ({
  invoiceId,
  onAdd,
  isSubmitting,
  canEdit,
}: AddLineItemFormProps) => {
  const [description, setDescription] = useState('');
  const [quantity, setQuantity] = useState('');
  const [unitPrice, setUnitPrice] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});

  // Calculate line total for preview
  const lineTotal = useMemo(() => {
    const qty = parseFloat(quantity);
    const price = parseFloat(unitPrice);
    if (!isNaN(qty) && !isNaN(price) && qty > 0 && price > 0) {
      return (qty * price).toFixed(2);
    }
    return '0.00';
  }, [quantity, unitPrice]);

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!description.trim()) {
      newErrors.description = 'Description is required';
    }

    const qty = parseFloat(quantity);
    if (!quantity || isNaN(qty) || qty <= 0) {
      newErrors.quantity = 'Quantity must be greater than 0';
    }

    const price = parseFloat(unitPrice);
    if (!unitPrice || isNaN(price) || price <= 0) {
      newErrors.unitPrice = 'Unit price must be greater than 0';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    const request: CreateLineItemRequest = {
      invoiceId,
      description: description.trim(),
      quantity: parseFloat(quantity),
      unitPrice: parseFloat(unitPrice),
    };

    onAdd(request);

    // Reset form
    setDescription('');
    setQuantity('');
    setUnitPrice('');
    setErrors({});
  };

  if (!canEdit) {
    return null;
  }

  return (
    <form onSubmit={handleSubmit} className="bg-gray-50 rounded-lg p-4 space-y-4">
      <h3 className="font-semibold text-lg">Add Line Item</h3>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Description */}
        <div className="md:col-span-1">
          <label className="block text-sm font-medium mb-1">Description</label>
          <input
            type="text"
            value={description}
            onChange={e => setDescription(e.target.value)}
            placeholder="Product or service"
            className={`w-full border rounded px-3 py-2 ${
              errors.description ? 'border-red-500' : 'border-gray-300'
            }`}
            disabled={isSubmitting}
          />
          {errors.description && <p className="text-red-500 text-sm mt-1">{errors.description}</p>}
        </div>

        {/* Quantity */}
        <div>
          <label className="block text-sm font-medium mb-1">Quantity</label>
          <input
            type="number"
            step="0.01"
            value={quantity}
            onChange={e => setQuantity(e.target.value)}
            placeholder="0.00"
            className={`w-full border rounded px-3 py-2 ${
              errors.quantity ? 'border-red-500' : 'border-gray-300'
            }`}
            disabled={isSubmitting}
          />
          {errors.quantity && <p className="text-red-500 text-sm mt-1">{errors.quantity}</p>}
        </div>

        {/* Unit Price */}
        <div>
          <label className="block text-sm font-medium mb-1">Unit Price</label>
          <input
            type="number"
            step="0.01"
            value={unitPrice}
            onChange={e => setUnitPrice(e.target.value)}
            placeholder="0.00"
            className={`w-full border rounded px-3 py-2 ${
              errors.unitPrice ? 'border-red-500' : 'border-gray-300'
            }`}
            disabled={isSubmitting}
          />
          {errors.unitPrice && <p className="text-red-500 text-sm mt-1">{errors.unitPrice}</p>}
        </div>
      </div>

      {/* Line Total Preview */}
      <div className="bg-blue-50 rounded p-3">
        <span className="text-sm font-medium">Line Total: </span>
        <span className="text-lg font-bold text-blue-700">${lineTotal}</span>
      </div>

      {/* Submit Button */}
      <button
        type="submit"
        disabled={isSubmitting}
        className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
      >
        {isSubmitting ? 'Adding...' : 'Add Line Item'}
      </button>
    </form>
  );
};
