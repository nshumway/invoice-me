import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../../api/authApi';
import type { LoginRequest } from '../../models/User';
import { useAuth } from '../../hooks/useAuth';

export const LoginViewModel = () => {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});

  const loginMutation = useMutation({
    mutationFn: authApi.login,
    onSuccess: response => {
      login(response.token, response.user);
      navigate('/customers');
    },
    onError: (error: Error) => {
      const axiosError = error as { response?: { data?: { message?: string } } };
      if (axiosError.response?.data?.message) {
        setErrors({ submit: axiosError.response.data.message });
      } else {
        setErrors({ submit: 'Login failed' });
      }
    },
  });

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!email.trim()) {
      newErrors.email = 'Email is required';
    }

    if (!password) {
      newErrors.password = 'Password is required';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!validate()) return;

    const request: LoginRequest = { email, password };
    loginMutation.mutate(request);
  };

  return {
    email,
    setEmail,
    password,
    setPassword,
    errors,
    handleSubmit,
    isSubmitting: loginMutation.isPending,
  };
};
