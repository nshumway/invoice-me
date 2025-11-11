import React from 'react';
import { Badge } from './Badge';
import type { BadgeVariant } from './Badge';

type InvoiceStatus = 'DRAFT' | 'SENT' | 'PAID' | 'OVERDUE' | 'CANCELLED';

interface StatusBadgeProps {
  status: InvoiceStatus;
  className?: string;
}

const statusVariantMap: Record<InvoiceStatus, BadgeVariant> = {
  DRAFT: 'neutral',
  SENT: 'primary',
  PAID: 'success',
  OVERDUE: 'error',
  CANCELLED: 'neutral',
};

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, className }) => {
  const variant = statusVariantMap[status] || 'neutral';

  return (
    <Badge variant={variant} className={className}>
      {status}
    </Badge>
  );
};
