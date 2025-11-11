import React from 'react';
import type { Payment } from '../../models/Payment';
import { PAYMENT_METHOD_LABELS } from '../../models/Payment';
import { useIsMobile } from '../../hooks/useMediaQuery';

interface PaymentsTableProps {
  payments: Payment[];
  onRowClick: (id: string) => void;
}

export const PaymentsTable: React.FC<PaymentsTableProps> = ({ payments, onRowClick }) => {
  const isMobile = useIsMobile();

  if (!payments || payments.length === 0) {
    return (
      <div className="bg-gray-800 rounded-lg p-6 text-center border border-gray-700">
        <p className="text-gray-400">No payments recorded yet</p>
      </div>
    );
  }

  // Calculate total
  const totalPaid = payments.reduce((sum, payment) => sum + parseFloat(payment.amount), 0);

  // Mobile card layout
  if (isMobile) {
    return (
      <div className="space-y-4">
        {payments.map(payment => (
          <div
            key={payment.id}
            onClick={() => onRowClick(payment.id)}
            className="bg-gray-800 border border-gray-700 rounded-lg p-4 space-y-2 cursor-pointer hover:bg-gray-750 transition-colors"
          >
            <div className="flex justify-between items-start">
              <div>
                <p className="text-sm text-gray-400">Date</p>
                <p className="text-gray-100">
                  {new Date(payment.paymentDate).toLocaleDateString()}
                </p>
              </div>
              <div className="text-right">
                <p className="text-sm text-gray-400">Amount</p>
                <p className="text-lg font-semibold text-gray-100 font-mono">
                  ${parseFloat(payment.amount).toFixed(2)}
                </p>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-2 pt-2 border-t border-gray-700">
              <div>
                <p className="text-sm text-gray-400">Method</p>
                <p className="text-gray-100">{PAYMENT_METHOD_LABELS[payment.paymentMethod]}</p>
              </div>
              <div>
                <p className="text-sm text-gray-400">Reference</p>
                <p className="text-gray-100">
                  {payment.referenceNumber || <span className="text-gray-500">—</span>}
                </p>
              </div>
            </div>
          </div>
        ))}
        <div className="bg-gray-800 border border-gray-700 rounded-lg p-4 border-dashed">
          <div className="flex justify-between items-center">
            <p className="font-semibold text-gray-200">Total Paid:</p>
            <p className="text-xl font-bold text-gray-100 font-mono">${totalPaid.toFixed(2)}</p>
          </div>
        </div>
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
              <th className="text-left p-4 font-medium text-gray-200">Date</th>
              <th className="text-right p-4 font-medium text-gray-200">Amount</th>
              <th className="text-left p-4 font-medium text-gray-200">Method</th>
              <th className="text-left p-4 font-medium text-gray-200">Reference</th>
            </tr>
          </thead>
          <tbody>
            {payments.map(payment => (
              <tr
                key={payment.id}
                onClick={() => onRowClick(payment.id)}
                className="border-b border-gray-700 hover:bg-gray-750 cursor-pointer transition-colors"
              >
                <td className="p-4 text-gray-100">
                  {new Date(payment.paymentDate).toLocaleDateString()}
                </td>
                <td className="p-4 text-right font-mono font-semibold text-gray-100">
                  ${parseFloat(payment.amount).toFixed(2)}
                </td>
                <td className="p-4 text-gray-100">
                  {PAYMENT_METHOD_LABELS[payment.paymentMethod]}
                </td>
                <td className="p-4 text-gray-400">
                  {payment.referenceNumber || <span className="text-gray-500">—</span>}
                </td>
              </tr>
            ))}
          </tbody>
          <tfoot className="bg-gray-700 border-t border-gray-600">
            <tr>
              <td className="p-4 font-semibold text-gray-200">Total Paid:</td>
              <td className="p-4 text-right font-mono font-bold text-lg text-gray-100">
                ${totalPaid.toFixed(2)}
              </td>
              <td colSpan={2}></td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>
  );
};
