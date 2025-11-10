import React from 'react';
import type { Payment } from '../../models/Payment';
import { PAYMENT_METHOD_LABELS } from '../../models/Payment';

interface PaymentsTableProps {
  payments: Payment[];
  onRowClick: (id: string) => void;
}

export const PaymentsTable: React.FC<PaymentsTableProps> = ({ payments, onRowClick }) => {
  if (!payments || payments.length === 0) {
    return (
      <div className="bg-gray-50 rounded-lg p-6 text-center">
        <p className="text-gray-600">No payments recorded yet</p>
      </div>
    );
  }

  // Calculate total
  const totalPaid = payments.reduce((sum, payment) => sum + parseFloat(payment.amount), 0);

  return (
    <div className="bg-white rounded-lg shadow overflow-hidden">
      <table className="w-full">
        <thead className="bg-gray-50 border-b">
          <tr>
            <th className="text-left p-4 font-medium text-gray-700">Date</th>
            <th className="text-right p-4 font-medium text-gray-700">Amount</th>
            <th className="text-left p-4 font-medium text-gray-700">Method</th>
            <th className="text-left p-4 font-medium text-gray-700">Reference</th>
          </tr>
        </thead>
        <tbody>
          {payments.map(payment => (
            <tr
              key={payment.id}
              onClick={() => onRowClick(payment.id)}
              className="border-b hover:bg-gray-50 cursor-pointer"
            >
              <td className="p-4">{new Date(payment.paymentDate).toLocaleDateString()}</td>
              <td className="p-4 text-right font-mono font-semibold">
                ${parseFloat(payment.amount).toFixed(2)}
              </td>
              <td className="p-4">{PAYMENT_METHOD_LABELS[payment.paymentMethod]}</td>
              <td className="p-4 text-gray-600">
                {payment.referenceNumber || <span className="text-gray-400">—</span>}
              </td>
            </tr>
          ))}
        </tbody>
        <tfoot className="bg-gray-50 border-t">
          <tr>
            <td className="p-4 font-semibold">Total Paid:</td>
            <td className="p-4 text-right font-mono font-bold text-lg">${totalPaid.toFixed(2)}</td>
            <td colSpan={2}></td>
          </tr>
        </tfoot>
      </table>
    </div>
  );
};
