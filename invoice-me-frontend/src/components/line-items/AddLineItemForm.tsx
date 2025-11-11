import { useState, useMemo } from 'react';
import type { CreateLineItemRequest } from '../../models/LineItem';
import { Button, Input } from '../shared';

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
    <form
      onSubmit={handleSubmit}
      className="bg-gray-700/50 border border-gray-600 rounded-lg p-4 space-y-4"
    >
      <h3 className="font-semibold text-lg text-gray-100">Add Line Item</h3>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Description */}
        <div className="md:col-span-1">
          <Input
            type="text"
            label="Description"
            value={description}
            onChange={e => setDescription(e.target.value)}
            placeholder="Product or service"
            error={errors.description}
            disabled={isSubmitting}
          />
        </div>

        {/* Quantity */}
        <div>
          <Input
            type="number"
            label="Quantity"
            step="0.01"
            value={quantity}
            onChange={e => setQuantity(e.target.value)}
            placeholder="0.00"
            error={errors.quantity}
            disabled={isSubmitting}
          />
        </div>

        {/* Unit Price */}
        <div>
          <Input
            type="number"
            label="Unit Price"
            step="0.01"
            value={unitPrice}
            onChange={e => setUnitPrice(e.target.value)}
            placeholder="0.00"
            error={errors.unitPrice}
            disabled={isSubmitting}
          />
        </div>
      </div>

      {/* Line Total Preview */}
      <div className="bg-primary-900/30 border border-primary-700/50 rounded-lg p-3">
        <span className="text-sm font-medium text-primary-200">Line Total: </span>
        <span className="text-lg font-bold text-primary-100">${lineTotal}</span>
      </div>

      {/* Submit Button */}
      <Button type="submit" disabled={isSubmitting} variant="primary">
        {isSubmitting ? 'Adding...' : 'Add Line Item'}
      </Button>
    </form>
  );
};
