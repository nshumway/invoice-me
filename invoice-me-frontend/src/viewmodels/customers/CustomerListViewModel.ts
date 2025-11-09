import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { customerApi } from '../../api/customerApi';
import type { CustomerListItem } from '../../models/Customer';

export const CustomerListViewModel = () => {
  const navigate = useNavigate();

  // Query for customer list
  const {
    data: customers,
    isLoading,
    isError,
    error,
  } = useQuery<CustomerListItem[]>({
    queryKey: ['customers', 'list'],
    queryFn: customerApi.listAll,
  });

  // Computed properties
  const errorMessage = isError
    ? error instanceof Error
      ? error.message
      : 'Failed to load customers'
    : null;

  // Actions
  const handleCreateNew = () => {
    navigate('/customers/new');
  };

  const handleRowClick = (customerId: string) => {
    navigate(`/customers/${customerId}`);
  };

  // Expose state and actions to view
  return {
    customers,
    isLoading,
    isError,
    errorMessage,
    handleCreateNew,
    handleRowClick,
  };
};
