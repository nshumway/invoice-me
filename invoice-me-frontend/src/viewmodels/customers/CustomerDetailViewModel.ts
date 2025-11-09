import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { customerApi } from '../../api/customerApi';
import type { Customer } from '../../models/Customer';

export const CustomerDetailViewModel = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);

  // Query for customer details
  const {
    data: customer,
    isLoading,
    isError,
    error,
  } = useQuery<Customer>({
    queryKey: ['customers', 'detail', id],
    queryFn: () => customerApi.getById(id!),
    enabled: !!id,
  });

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: () => customerApi.delete(id!, customer!.version),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'] });
      navigate('/customers');
    },
    onError: (error: unknown) => {
      console.error('Delete failed:', error);
      const err = error as { response?: { data?: { message?: string } } };
      alert(err.response?.data?.message || 'Failed to delete customer');
    },
  });

  // Computed properties
  const errorMessage = isError
    ? error instanceof Error
      ? error.message
      : 'Failed to load customer'
    : null;

  // Actions
  const handleBack = () => {
    navigate('/customers');
  };

  const handleEdit = () => {
    navigate(`/customers/${id}/edit`);
  };

  const handleDelete = () => {
    setShowDeleteDialog(true);
  };

  const handleConfirmDelete = () => {
    deleteMutation.mutate();
    setShowDeleteDialog(false);
  };

  const handleCancelDelete = () => {
    setShowDeleteDialog(false);
  };

  return {
    customer,
    isLoading,
    isError,
    errorMessage,
    showDeleteDialog,
    handleBack,
    handleEdit,
    handleDelete,
    handleConfirmDelete,
    handleCancelDelete,
    isDeleting: deleteMutation.isPending,
  };
};
