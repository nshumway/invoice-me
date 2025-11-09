import { useState, useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { customerApi } from '../../api/customerApi';
import type { CreateCustomerRequest, UpdateCustomerRequest } from '../../models/Customer';

interface CustomerFormViewModelProps {
  customerId?: string; // If provided, we're in edit mode
}

export const CustomerFormViewModel = ({ customerId }: CustomerFormViewModelProps = {}) => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const isEditMode = !!customerId;

  // Form fields
  const [companyName, setCompanyName] = useState('');
  const [email, setEmail] = useState('');
  const [contactFirstName, setContactFirstName] = useState('');
  const [contactLastName, setContactLastName] = useState('');
  const [phone, setPhone] = useState('');
  const [addressLine1, setAddressLine1] = useState('');
  const [addressLine2, setAddressLine2] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('');
  const [zipCode, setZipCode] = useState('');
  const [country, setCountry] = useState('');
  const [version, setVersion] = useState<number>(0);

  // Validation errors
  const [errors, setErrors] = useState<Record<string, string>>({});

  // Load existing customer if in edit mode
  const { data: customer, isLoading: isLoadingCustomer } = useQuery({
    queryKey: ['customers', customerId],
    queryFn: () => customerApi.getById(customerId!),
    enabled: isEditMode,
  });

  // Populate form when customer data loads
  useEffect(() => {
    if (customer) {
      setCompanyName(customer.companyName);
      setEmail(customer.email);
      setContactFirstName(customer.contactFirstName || '');
      setContactLastName(customer.contactLastName || '');
      setPhone(customer.phone || '');
      setAddressLine1(customer.addressLine1 || '');
      setAddressLine2(customer.addressLine2 || '');
      setCity(customer.city || '');
      setState(customer.state || '');
      setZipCode(customer.zipCode || '');
      setCountry(customer.country || '');
      setVersion(customer.version);
    }
  }, [customer]);

  // Create mutation
  const createMutation = useMutation({
    mutationFn: customerApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers', 'list'] });
      navigate('/customers');
    },
    onError: (error: unknown) => {
      const err = error as { response?: { data?: { message?: string } } };
      if (err.response?.data?.message) {
        setErrors({ submit: err.response.data.message });
      } else {
        setErrors({ submit: 'Failed to create customer' });
      }
    },
  });

  // Update mutation
  const updateMutation = useMutation({
    mutationFn: customerApi.update,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'] });
      navigate('/customers');
    },
    onError: (error: unknown) => {
      const err = error as { response?: { data?: { message?: string } } };
      if (err.response?.data?.message) {
        setErrors({ submit: err.response.data.message });
      } else {
        setErrors({ submit: 'Failed to update customer' });
      }
    },
  });

  // Validation
  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!companyName.trim()) {
      newErrors.companyName = 'Company name is required';
    }

    if (!email.trim()) {
      newErrors.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      newErrors.email = 'Must be a valid email address';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Submit handler
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!validate()) return;

    if (isEditMode) {
      const updateData: UpdateCustomerRequest = {
        id: customerId!,
        version,
        companyName,
        email,
        contactFirstName: contactFirstName || undefined,
        contactLastName: contactLastName || undefined,
        phone: phone || undefined,
        addressLine1: addressLine1 || undefined,
        addressLine2: addressLine2 || undefined,
        city: city || undefined,
        state: state || undefined,
        zipCode: zipCode || undefined,
        country: country || undefined,
      };
      updateMutation.mutate(updateData);
    } else {
      const createData: CreateCustomerRequest = {
        companyName,
        email,
        contactFirstName: contactFirstName || undefined,
        contactLastName: contactLastName || undefined,
        phone: phone || undefined,
        addressLine1: addressLine1 || undefined,
        addressLine2: addressLine2 || undefined,
        city: city || undefined,
        state: state || undefined,
        zipCode: zipCode || undefined,
        country: country || undefined,
      };
      createMutation.mutate(createData);
    }
  };

  const handleCancel = () => {
    if (isEditMode) {
      navigate(`/customers/${customerId}`);
    } else {
      navigate('/customers');
    }
  };

  return {
    // Mode
    isEditMode,
    isLoadingCustomer,

    // Form state
    companyName,
    setCompanyName,
    email,
    setEmail,
    contactFirstName,
    setContactFirstName,
    contactLastName,
    setContactLastName,
    phone,
    setPhone,
    addressLine1,
    setAddressLine1,
    addressLine2,
    setAddressLine2,
    city,
    setCity,
    state,
    setState,
    zipCode,
    setZipCode,
    country,
    setCountry,

    // Validation
    errors,

    // Actions
    handleSubmit,
    handleCancel,

    // Loading state
    isSubmitting: createMutation.isPending || updateMutation.isPending,
  };
};
